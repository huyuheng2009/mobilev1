package op

import com.alibaba.fastjson.JSONArray
import org.apache.tomcat.util.http.fileupload.FileItem
import org.jpos.core.CardHolder
import org.jpos.iso.ISOMsg
import org.jpos.iso.ISOUtil
import org.jpos.tlv.TLVList
import util.Commons
import util.Constants
import util.JCEHandler

import java.math.RoundingMode
import java.sql.Timestamp
import java.text.SimpleDateFormat

/**
 * @author hanlei
 */

//HttpServletRequest req
//HttpServletResponse resp
//Dao dao
if (!'post'.equalsIgnoreCase(req.method)) {
    resp.status = 405
    resp.getOutputStream() << new File('static/405.html').newInputStream()
    return
}

def params = Commons.parseRequest(req)

log.info "params: $params"

def required = ['reqTime', 'reqNo', 'amount', 'ksnNo']
def lastvalidparam = null
def miss = required.any{lastvalidparam = it; !(params.containsKey(it))}
if (miss) {
    log.info "miss required param: ${lastvalidparam}"
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', "缺少参数" + lastvalidparam)); return
}
if((params.containsKey('encTracks') && params.containsKey('iccData'))||(!params.containsKey('encTracks') && !params.containsKey('iccData'))){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT','非法参数 encTracks、iccData')); return
}

def terminal = dao.findPosTerminalByKsnNo(params.ksnNo)
def posMerchant = dao.findPosMerchantByMerchantNo(session.merchant_no)

if (!terminal) {
    render(Commons.fail(null, 'KSNNO_OR_LICENSECODE_NOT_AVAILABLE', '终端不可用')); return
}
if(!posMerchant){
    render(Commons.fail(null, 'INVALID_MERCHANT')); return
}

def mobileMerchant = dao.findMobileMerchantByMerchantNo(session.merchant_no)
if(!(mobileMerchant.real_name_status == 4 && posMerchant.account_status == 4)){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT','该商户没通过认证')); return
}

def ksn = dao.findKSNByKSNNO(params.ksnNo)
if(!ksn || !ksn.id){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT','未找到该设备')); return
}

if (terminal.merchant_no != posMerchant.merchant_no) {
    log.error 'terminal merchant not match real merchant'
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
}

def mac = params.mac?.trim()
//附加数据（ksn+reqNo的16进制）+磁道密文+PIN密文+ksn+terSerial
def mac_local;

if(params.containsKey('encTracks')){
    mac_local = Commons.encodeAsSHA1(params.reqTime + params.reqNo + params.amount + params.encTracks + params.position)
    log.info "sha1 之前的=${params.reqTime + params.reqNo + params.amount + params.encTracks + params.position}"
}else if(params.containsKey('iccData')){
    mac_local = Commons.encodeAsSHA1(params.reqTime + params.reqNo + params.amount + params.iccData + params.position)
    log.info "sha1 之前的=${params.reqTime + params.reqNo + params.amount + params.iccData + params.position}"
}else{
    render(Commons.fail(null, 'PERMISSION_DENIED', '抱歉, 您不能进行此操作')); return
}
log.info 'mac:'+"${mac}"+'  mac_local:'+"${mac_local}"
if(!mac || mac != mac_local){
    render(Commons.fail(null, 'PERMISSION_DENIED', '抱歉, 您不能进行此操作')); return
}

def now = new Date()
def batch = "${terminal.batch_no}".padLeft(6, '0')
def trace = "${params.reqNo}".padLeft(6, '0')

def _tcount = dao.db.firstRow("""
	select count(id) c from trans_info where terminal_batch_no=${batch} and terminal_voucher_no=${trace} and terminal_no=${terminal.terminal_no}
	and merchant_no=${terminal.merchant_no} and to_days(create_time) = to_days(${new Date()})
""").c
if (_tcount > 0) {
    render(Commons.fail(null, '94', '流水号重复,需要重新登录')); return
}

def signatureUri = null
def signature = params.signature as FileItem
if (!signature || signature.isFormField()) {
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
}

if (signature) {
    if (signature.size < 1024 || signature.size > 1024 * 1024) {
        render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '图片大小不合格')); return
    }

    def rootDir = new File('uploads/signature/')
    signatureUri = "${now.format('yyyyMMdd')}${terminal.terminal_no}${batch}${trace}.png"
    def file = new File(rootDir, signatureUri)
    if (!file.parentFile.exists()) file.parentFile.mkdirs()
    log.info "signatureUri: $signatureUri"
    signature.write(file)
    signature.delete()
}

def key = dao.findKeyByKsnNo(params.ksnNo)
if(!key){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '设备有问题')); return
}

TLVList tlvList;
def cardHolder;
def trailler;
def encPinblock;
def pinKsn;
def region23
if(params.iccData){//带芯片卡
    tlvList = new TLVList();
    tlvList.unpack(ISOUtil.hex2byte(params.iccData));
    String tlv_C0 = tlvList.getString(0xC0);
    pinKsn = tlvList.getString(0xC1);
    String tlv_C2 = tlvList.getString(0xC2);
    encPinblock = tlvList.getString(0xC7);
    def decode_C2 = Commons.decodeTracks2(tlv_C0, key?.bdk, tlv_C2, ksn.model)
    println("C2:${decode_C2}")
    def hexLen = decode_C2.substring(4,8)
    println("hexLen:${hexLen}")
    int length = Commons.char2Hex(hexLen.charAt(0))*Math.pow(16,3)+Commons.char2Hex(hexLen.charAt(1))*Math.pow(16,2)+Commons.char2Hex(hexLen.charAt(2))*Math.pow(16,1)+Commons.char2Hex(hexLen.charAt(3))*Math.pow(16,0)
    println("length:${length}")
    //去掉前面8位固定位和后面多余补位
    decode_C2=decode_C2.substring(8,decode_C2.length()).substring(0,length*2)
    tlvList.unpack(ISOUtil.hex2byte(decode_C2));
    def _track2 = tlvList.getString(0x57).substring(0,tlvList.getString(0x57).indexOf("F")).replace("D","=");
    cardHolder = new CardHolder(_track2)
    println("track2:${cardHolder.getTrack2()}")
    println("pan:${cardHolder.getPAN()}")
    println("exp:${cardHolder.getEXP()}")
    trailler = new TLVList();
    trailler.append(0x9F33,tlvList.getString(0x9F33))
    trailler.append(0x95,tlvList.getString(0x95))
    trailler.append(0x9F1E,tlvList.getString(0x9F1E))
    trailler.append(0x9F10,tlvList.getString(0x9F10))
    trailler.append(0x9F26,tlvList.getString(0x9F26))
    trailler.append(0x9F36,tlvList.getString(0x9F36))
    trailler.append(0x82,tlvList.getString(0x82))
    println("0x82:${tlvList.getString(0x82)}")
    trailler.append(0x9C,tlvList.getString(0x9C))
    trailler.append(0x9F1A,tlvList.getString(0x9F1A))
    trailler.append(0x9A,tlvList.getString(0x9A))
    trailler.append(0x9F02,tlvList.getString(0x9F02))
    trailler.append(0x5F2A,tlvList.getString(0x5F2A))
    trailler.append(0x9F03,tlvList.getString(0x9F03))
    trailler.append(0x9F35,tlvList.getString(0x9F35))
    trailler.append(0x9F34,tlvList.getString(0x9F34))
    trailler.append(0x9F37,tlvList.getString(0x9F37))
    trailler.append(0x9F27,tlvList.getString(0x9F27))
    trailler.append(0x9F41,tlvList.getString(0x9F41))

    trailler.append(0x84,tlvList.getString(0x84))
    trailler.append(0x9F09,tlvList.getString(0x9F09))
    trailler = trailler.pack()
    region23 ='0'+tlvList.getString(0x5F34)
    println('region23:'+region23)
    println("trailler:${trailler}")

}else{//磁条卡
     cardHolder = Commons.decodeTracks(params.trackKsn, key?.bdk, params.encTracks, ksn.model)
}
def cardbin = dao.findCardbin(cardHolder.pan)
if (!cardbin) {
    render(Commons.fail(null, '15', '不支持的卡')); return
}

def  t_count = dao.db.firstRow("""
    select count(id) c from trans_info where terminal_no=${terminal.terminal_no} and card_no=${cardHolder.pan}
    and trans_status='SUCCESS' and card_type <> 'DEBIT' and trans_type='SALE' and to_days(create_time) = to_days(${new Date()})
""").c
if (t_count > 2) {
    render(Commons.fail(null, '65', '超出消费次数限制')); return
}


def clearTpk = JCEHandler.decryptData(terminal.tmk_tpk, terminal.tmk)
def clearTak = JCEHandler.decryptData(terminal.tmk_tak, terminal.tmk)
def pinBlock
try{
    if(params.containsKey('iccData')){
        println("pinKsn：${pinKsn},encPinblock:${encPinblock},bdk:${key?.bdk},pan:${cardHolder.pan},clearTpk:${clearTpk},model:${ksn.model}")
            pinBlock = Commons.decodePinBlock(pinKsn, key?.bdk, cardHolder.pan, encPinblock, clearTpk, ksn.model)
    }else if(params.containsKey('encTracks')){
        pinBlock = Commons.decodePinBlock(params.pinKsn, key?.bdk, cardHolder.pan, params.encPinblock, clearTpk, ksn.model)
    }else{
        render(Commons.fail(null, 'ILLEGAL_ARGUMENT','非法参数 encTracks、iccData')); return
    }
}catch(Exception e){
    e.printStackTrace()
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT','密码长度不对')); return
}
def amount = Long.parseLong(params.amount)

//检验是否是金额整数
if(!Commons.checkAmount(amount)){
    render(Commons.fail(null, '13', '无效金额')); return
}

//检验是否是是白名单
def whiteList = dao.findWhiteListByMerchantNo(posMerchant.merchant_no)
println("whiteList=${whiteList}")
SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
long systemMillis = System.currentTimeMillis();
if(whiteList){
    if(whiteList){
        //风控规则
        if(amount > whiteList.each_max){
            render(Commons.fail(null, 'TR_TRAN_AMOUNT_TOO_MUCH', '很抱歉, 交易被拒绝, 交易金额过高')); return
        }
        if(amount < whiteList.each_min){
            render(Commons.fail(null, 'TR_TRAN_AMOUNT_TOO_SMALL', '很抱歉, 交易被拒绝, 交易金额过低')); return
        }
        if(whiteList.terminal_daily_max){
            def sumDaily = dao.findSumAmountDailyByTerminalNo(terminal.terminal_no)*100
            println("sumDaily+amount=${sumDaily+amount},sumDaily=${sumDaily},riskValue.terminal_daily_max=${whiteList.terminal_daily_max}")
            if(sumDaily+amount > whiteList.terminal_daily_max){
                render(Commons.fail(null, 'TR_TRAN_AMOUNT_TOO_MUCH', '很抱歉, 交易被拒绝, 超出当日限额')); return
            }
        }
        if(whiteList.terminal_month_max){
            def sumMonth = dao.findSumAmountMonthByTerminalNo(terminal.terminal_no)*100
            println("sumMonth=${sumMonth},sumMonth+amount=${sumMonth+amount},riskValue.terminal_month_max=${whiteList.terminal_month_max}")
            if(sumMonth+amount > whiteList.terminal_month_max){
                render(Commons.fail(null, 'TR_TRAN_AMOUNT_TOO_MUCH', '很抱歉, 交易被拒绝, 超出当月限额')); return
            }
        }
        if(whiteList.limit_time_begin&&whiteList.limit_time_end){
            Date temp = sf.parse("2014-08-22 "+whiteList.limit_time_begin);
            Calendar limitTimeBegin = Calendar.getInstance();
            limitTimeBegin.set(Calendar.HOUR_OF_DAY,temp.getHours());
            limitTimeBegin.set(Calendar.MINUTE,temp.getMinutes());
            limitTimeBegin.set(Calendar.SECOND, temp.getSeconds());
            long limitTimeBeginMillis = limitTimeBegin.getTimeInMillis();

            temp = sf.parse("2014-08-22 "+whiteList.limit_time_end);
            Calendar limitTimeEnd = Calendar.getInstance();
            limitTimeEnd.set(Calendar.HOUR_OF_DAY,temp.getHours());
            limitTimeEnd.set(Calendar.MINUTE,temp.getMinutes());
            limitTimeEnd.set(Calendar.SECOND, temp.getSeconds());
            long limitTimeEndMillis = limitTimeEnd.getTimeInMillis();

            if(systemMillis<limitTimeBeginMillis||systemMillis>limitTimeEndMillis){
                render(Commons.fail(null, 'TR_TRAN_TIME_NOT_ALLOWED', '很抱歉, 交易被拒绝, 该时间不允许交易')); return
            }
        }
    }
}else{
    def riskValue = dao.findRiskValueByProductCodeAndModel('hft', 'mpos')
    println("riskValue=${riskValue}")
    if(riskValue){
        //风控规则
        if(amount > riskValue.each_max){
            render(Commons.fail(null, 'TR_TRAN_AMOUNT_TOO_MUCH', '很抱歉, 交易被拒绝, 交易金额过高')); return
        }
        if(amount < riskValue.each_min){
            render(Commons.fail(null, 'TR_TRAN_AMOUNT_TOO_SMALL', '很抱歉, 交易被拒绝, 交易金额过低')); return
        }
        if(riskValue.terminal_daily_max){
            def sumDaily = dao.findSumAmountDailyByTerminalNo(terminal.terminal_no)*100
            println("sumDaily+amount=${sumDaily+amount},sumDaily=${sumDaily},riskValue.terminal_daily_max=${riskValue.terminal_daily_max}")
            if(sumDaily+amount > riskValue.terminal_daily_max){
                render(Commons.fail(null, 'TR_TRAN_AMOUNT_TOO_MUCH', '很抱歉, 交易被拒绝, 超出当日限额')); return
            }
        }
        if(riskValue.terminal_month_max){
            def sumMonth = dao.findSumAmountMonthByTerminalNo(terminal.terminal_no)*100
            println("sumMonth=${sumMonth},sumMonth+amount=${sumMonth+amount},riskValue.terminal_month_max=${riskValue.terminal_month_max}")
            if(sumMonth+amount > riskValue.terminal_month_max){
                render(Commons.fail(null, 'TR_TRAN_AMOUNT_TOO_MUCH', '很抱歉, 交易被拒绝, 超出当月限额')); return
            }
        }
        if(riskValue.limit_time_begin&&riskValue.limit_time_end){
            Date temp = sf.parse("2014-08-22 "+riskValue.limit_time_begin);
            Calendar limitTimeBegin = Calendar.getInstance();
            limitTimeBegin.set(Calendar.HOUR_OF_DAY,temp.getHours());
            limitTimeBegin.set(Calendar.MINUTE,temp.getMinutes());
            limitTimeBegin.set(Calendar.SECOND, temp.getSeconds());
            long limitTimeBeginMillis = limitTimeBegin.getTimeInMillis();

            temp = sf.parse("2014-08-22 "+riskValue.limit_time_end);
            Calendar limitTimeEnd = Calendar.getInstance();
            limitTimeEnd.set(Calendar.HOUR_OF_DAY,temp.getHours());
            limitTimeEnd.set(Calendar.MINUTE,temp.getMinutes());
            limitTimeEnd.set(Calendar.SECOND, temp.getSeconds());
            long limitTimeEndMillis = limitTimeEnd.getTimeInMillis();

            if(systemMillis<limitTimeBeginMillis||systemMillis>limitTimeEndMillis){
                render(Commons.fail(null, 'TR_TRAN_TIME_NOT_ALLOWED', '很抱歉, 交易被拒绝, 该时间不允许交易')); return
            }
        }
    }
}



// send trans to ts
ISOMsg msg = new ISOMsg()
msg.set  0, '0200'
msg.set  2, cardHolder.pan
msg.set  3, '000000'
msg.set  4, "${amount}".padLeft(12, '0')
msg.set 11, trace
if(params.containsKey('iccData')){
    msg.set 14, cardHolder.getEXP()
}
if(params.containsKey('encTracks')){
    msg.set 22, params.encPinblock?'021':'022'
}else{
    msg.set 22, '051'
}
if(params.containsKey('iccData')){
    msg.set 23, region23
}
msg.set 25, '00'
if(params.containsKey('iccData')){
    msg.set 26, '12'
}else{
    msg.set 26, '06'
}
msg.set 35, cardHolder.track2
msg.set 41, terminal.terminal_no
msg.set 42, terminal.merchant_no
msg.set 49, '156'
if (pinBlock) {
    msg.set 52, ISOUtil.hex2byte(pinBlock)
}
msg.set 53, '2000000000000000'
if(params.containsKey('iccData')){
    msg.set 55,  trailler
}
msg.set 60, '22' + batch+'000'
msg.set 64, new byte[8]

println('msg:'+msg)
try {
    ISOMsg r = Commons.sendAndRecive(msg, clearTak)

    if (!r) {
        // until timeout
        return
    }

    def code = r.getString(39)

    def result = null
    if (code == '00') {
        println "iccData:${r.getString(55)}"
        result = Commons.success([
                reqNo: params.reqNo?:null,
                merchantName: posMerchant.merchant_name,
                merchantNo: terminal.merchant_no,
                terminalNo: terminal.terminal_no,
                operatorNo: '01',
                cardNoWipe: cardHolder.pan[0..5] + '*****' + cardHolder.pan[-4..-1],
                amount: amount,
                currency:params.currency?:'CNY',
                issuer:cardbin?cardbin.bank_name:null,
                voucherNo:trace,
                batchNo:batch,
                transTime:now.format('yyyy') + r.getString(13) + r.getString(12),
                refNo:r.getString(37),
                authNo:r.hasField(38)?r.getString(38):' ',
                iccData:r.hasField(55)?r.getString(55):' ',
        ])
    } else {
        result = Commons.fail([reqNo: params.reqNo?:null], code)
    }

    render result

} finally {
    terminal.voucher_no = params.reqNo.toString().padLeft(6, '0')
    dao.update(terminal)
}