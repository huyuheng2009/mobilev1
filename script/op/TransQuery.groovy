package op

import org.jpos.core.CardHolder
import org.jpos.iso.ISOMsg
import org.jpos.iso.ISOUtil
import org.jpos.tlv.TLVList
import util.Commons
import util.Constants
import util.JCEHandler

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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

def required = ['reqTime', 'reqNo', 'ksnNo']
def lastvalidparam = null
def miss = required.any{lastvalidparam = it; !(req.getParameter(it))}
if (miss) {
	log.info "miss required param: ${lastvalidparam}"
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
}
if((params.containsKey('encTracks') && params.containsKey('iccData'))||(!params.containsKey('encTracks') && !params.containsKey('iccData'))){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT','非法参数 encTracks、iccData')); return
}

def terminal = dao.findPosTerminalByKsnNo(req.getParameter('ksnNo'))
def posMerchant = dao.findPosMerchantByMerchantNo(session.merchant_no)
if (!terminal) {
    render(Commons.fail(null, 'INVALID_TERMINAL', '终端不可用')); return
}
if(!posMerchant){
    render(Commons.fail(null, 'INVALID_MERCHANT')); return
}

def mobileMerchant = dao.findMobileMerchantByMerchantNo(session.merchant_no)
if(!(mobileMerchant.real_name_status == 4 && posMerchant.account_status == 4)){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT','该商户没通过认证')); return
}

def ksn = dao.findKSNByKSNNO(req.getParameter('ksnNo'))
if(!ksn || !ksn.id){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT','未找到该设备')); return
}

def batch = "${terminal.batch_no}".padLeft(6, '0')
def trace = "${req.getParameter('reqNo')}".padLeft(6, '0')

terminal.voucher_no = trace as int
dao.update(terminal)

def key = dao.findKeyByKsnNo(req.getParameter('ksnNo'))
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
    encPinblock = tlvList.getString(0xC7)
    def decode_C2 = Commons.decodeTracks2(tlv_C0, key?.bdk, tlv_C2, ksn.model)
    println("C2:${decode_C2}")
    def hexLen = decode_C2.substring(4,8)
    println("hexLen:${hexLen}")
    int length = Commons.char2Hex(hexLen.charAt(0))*Math.pow(16,3)+Commons.char2Hex(hexLen.charAt(1))*Math.pow(16,2)+Commons.char2Hex(hexLen.charAt(2))*Math.pow(16,1)+Commons.char2Hex(hexLen.charAt(3))*Math.pow(16,0)
    println("length:${length}")
    //去掉前面8位固定位和后面多余补位
    decode_C2=decode_C2.substring(8,decode_C2.length()).substring(0,length*2)
    println("decode_C2:${decode_C2}")
    tlvList.unpack(ISOUtil.hex2byte(decode_C2));
    def track2 = tlvList.getString(0x57).substring(0,tlvList.getString(0x57).indexOf("F")).replace("D","="), pan =tlvList.getString(0x5A), exp =tlvList.getString(0x5F24).substring(0,tlvList.getString(0x5F24).length()-2);
    cardHolder = new CardHolder(track2)
    println("track2:${cardHolder.track2}")
    println("pan:${cardHolder.pan}")
    println("exp:${cardHolder.getEXP()}")
    trailler = new TLVList();
    trailler.append(0x9F26,tlvList.getString(0x9F26))
    trailler.append(0x9F27,tlvList.getString(0x9F27))
    trailler.append(0x9F10,tlvList.getString(0x9F10))
    trailler.append(0x9F37,tlvList.getString(0x9F37))
    trailler.append(0x9F36,tlvList.getString(0x9F36))
    trailler.append(0x95,tlvList.getString(0x95))
    trailler.append(0x9A,tlvList.getString(0x9A))
    trailler.append(0x9C,tlvList.getString(0x9C))
    trailler.append(0x9F02,tlvList.getString(0x9F02))
   trailler.append(0x5F2A,tlvList.getString(0x5F2A))
    trailler.append(0x82,tlvList.getString(0x82))
    println("0x82:${tlvList.getString(0x82)}")
    trailler.append(0x9F1A,tlvList.getString(0x9F1A))
    trailler.append(0x9F03,tlvList.getString(0x9F03))
    trailler.append(0x9F33,tlvList.getString(0x9F33))
    trailler.append(0x9F34,tlvList.getString(0x9F34))
    trailler.append(0x9F35,tlvList.getString(0x9F35))
    trailler.append(0x9F1E,tlvList.getString(0x9F1E))
    trailler.append(0x84,tlvList.getString(0x84))
    trailler.append(0x9F09,tlvList.getString(0x9F09))
    trailler.append(0x9F41,tlvList.getString(0x9F41))
    region23 ='0'+tlvList.getString(0x5F34)
    println('region23:'+region23)
    trailler = trailler.pack()
    println("trailler:${trailler}")
}else{//磁条卡
    cardHolder = Commons.decodeTracks(params.trackKsn, key?.bdk, params.encTracks, ksn.model)
}

def cardbin = dao.findCardbin(cardHolder.pan)
if (!cardbin) {
    render(Commons.fail(null, '15', '不支持的卡')); return
}
if("借记卡" != cardbin.card_type){
    render(Commons.fail(null, 'NOT_SUPPORT_CREDIT_CARD', '该功能不支持信用卡')); return
}
log.info("terminal=${terminal}")
def clearTpk = JCEHandler.decryptData(terminal.tmk_tpk, terminal.tmk)
def clearTak = JCEHandler.decryptData(terminal.tmk_tak, terminal.tmk)
def pinBlock
try{
    if(params.containsKey('iccData')){
        println("pinKsn：${pinKsn},encPinblock:${encPinblock},bdk:${key?.bdk},pan:${cardHolder.pan},clearTpk:${clearTpk},model:${ksn.model}")
        pinBlock = Commons.decodePinBlock(pinKsn, key?.bdk, cardHolder.pan, encPinblock, clearTpk, ksn.model)
    }else if(params.containsKey('encTracks')){
        pinBlock = Commons.decodePinBlock(req.getParameter('pinKsn'), key?.bdk, cardHolder.pan, req.getParameter('encPinblock'), clearTpk, ksn.model)
    }else{
        render(Commons.fail(null, 'ILLEGAL_ARGUMENT','非法参数 encTracks、iccData')); return
    }
}catch(Exception e){
    e.printStackTrace()
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT','密码长度不对')); return
}

ISOMsg msg = new ISOMsg()
msg.set  0, '0200'
msg.set  2, cardHolder.pan
msg.set  3, '310000'
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
    msg.set 23, '000'
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
msg.set 60, '01' + batch+'000'
msg.set 64, new byte[8]

println('msg:'+msg)

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
			balance: r.getString(54)[-12..-1],
			currency:'CNY',
            iccData:r.hasField(55)?r.getString(55):' ',
	])
} else {
	result = Commons.fail(null, code)
}
terminal.voucher_no = trace
dao.update(terminal)
render result