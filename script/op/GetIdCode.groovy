package op

import util.Commons
import util.Constants

import javax.servlet.http.Cookie
import java.sql.Timestamp
import java.text.SimpleDateFormat

/**
 * Created with IntelliJ IDEA.
 * User: hanlei
 * Date: 13-7-2
 * Time: 下午2:33
 * To change this template use File | Settings | File Templates.
 */
if (!'post'.equalsIgnoreCase(req.method)) {
    resp.status = 405
    resp.getOutputStream() << new File('static/405.html').newInputStream()
    return
}
def required = ['reqTime', 'mobile', 'appVersion']
def miss = required.any{lastvalidparam = it;  !(req.getParameter(it))}
if (miss) {
    log.info "miss required param: ${lastvalidparam}"
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', "缺少参数" + lastvalidparam)); return
}
def mobile = req.getParameter('mobile')?.trim()
def ksnNo = req.getParameter('ksnNo')?.trim()
def product = req.getParameter('product')?.trim()
def isValidateMobile = req.getParameter('isValidateMobile')?.trim()
def ver = Commons.versionParse(req.getParameter('appVersion'))
def business = req.getParameter('business')?.trim()
if (!ver) {
    render Commons.fail(null, 'ILLEGAL_ARGUMENT', 'app version error')
}
//def ksn = dao.findKSNByKSNNO(ksnNo)
//if (!ksnNo || !ksn.id) {
//    render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
//}
def mobileMerchant = dao.findMobileMerchantByMobileNo(mobile);
if ("1"==isValidateMobile&&mobileMerchant) {
    render(Commons.fail(null, 'MOBILE_EXIST')); return
}else if("2"==isValidateMobile&&!mobileMerchant){
    render(Commons.fail(null, 'MOBILE_NOTEXIST')); return
}
def mobileIdCode = []
mobileIdCode = dao.findeMobileIdentifyCodeByMobile(mobile)
println("mobileIdCode=${mobileIdCode}")
if(mobileIdCode){
    if(System.currentTimeMillis() - mobileIdCode.create_time?.time <= 50*1000L){
        log.info("时间间隔：${System.currentTimeMillis() - mobileIdCode.create_time?.time}")
        render(Commons.fail(null, 'REQUEST_TOO_OFFEN','请求太频繁')); return
    }
}
int idCode = Math.random()*9000+1000
int count = mobileIdCode?mobileIdCode.send_count:0
SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
String today = sdf.format(new Date())
if(mobileIdCode && today != mobileIdCode.create_time.format('yyyy-MM-dd')){
    count = 0
}
//TODO签名以后多产品要分开控制
if(count >= 10){
    render(Commons.fail(null, 'SEND_MSG_FAILS','今天发送短信过多')); return
}
log.info('***   begin send message  ***')
def signature,tips;
if(ver.model=='hft'){
    signature = '和付通'
}else if(ver.model=='zlzf'){
    signature = '招联支付'
}else{
    signature = '和付通'
}
tips = Constants.message_code_mapping.get(business,"")
def flag = Commons.newSendMsg(mobile, "${tips}验证码：${idCode}【${signature}】")
mobileIdCode = [
        'id_code' : idCode,
        'create_time' : new Timestamp(new Date().time),
        'mobile_no' : mobile,
        'ksn_no'     : ksnNo,
        'validate_status' : '0',
        'send_count' : count,
        'validate_count' : 0,
        'send_status' : flag
]
//if(!flag){
//    // render(Commons.fail(null, 'SEND_MSG_FAILS','发送短信失败')); return
//    mobileIdCode.status = 0
//}else{
//    mobileIdCode.status = 1
//    count++
//}

dao.db.executeUpdate("delete from mobile_identify_code where mobile_no=${mobile}")
dao.db.dataSet('mobile_identify_code').add(mobileIdCode)
render(Commons.success(reqNo: req.getParameter('reqNo')?:null, '发送验证码成功，注意查收'))