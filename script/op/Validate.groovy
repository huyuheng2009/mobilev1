package op

import util.Commons

import javax.servlet.http.Cookie
import java.sql.Timestamp

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
//def required = ['reqTime', 'mobile', 'idCode', 'ksnNo']
//def miss = required.any{lastvalidparam = it;  !(req.getParameter(it))}
//if (miss) {
//    log.info "miss required param: ${lastvalidparam}"
//    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', "缺少参数" + lastvalidparam)); return
//}
//def ksn = req.getParameter('ksnNo')?.trim()
//def idCode = req.getParameter('idCode')?.trim()
//def mobileIdCode = dao.findeMobileIdentifyCodeByKsnNo(ksn)
//if(mobileIdCode){
//    if(System.currentTimeMillis() - mobileIdCode.date_created?.timestampValue().time >= 30*60*1000L){
//        log.info("时间间隔：${System.currentTimeMillis() - mobileIdCode.date_created?.timestampValue().time}")
//        render(Commons.fail(null, 'IDENTIFY_CODE_OVER','验证码过期')); return
//    }
//}else{
//    render(Commons.fail(null, 'NOT_VALIDATE','该手机号没有验证')); return
//}
//Cookie[] c = req.getCookies()
//String value
//if(c){
//   if(c[0] && c[0].getName() == 'idCode'){
//       value = c[0].getValue()
//   }
//}
//mobileIdCode.validate_count = mobileIdCode.validate_count + 1
//if(!value || value != mobileIdCode.cookie_value){
//    dao.update(mobileIdCode)
//    render(Commons.fail(null, 'AFRESH_IDENTIFY',"请重新获取验证码"));return
//}
//
//if(!mobileIdCode || mobileIdCode.id_code != idCode){
//    dao.update(mobileIdCode)
//    if(mobileIdCode.validate_count > 5){
//        render(Commons.fail(null, 'REQUEST_TOO_OFFEN', "请求太频繁")); return
//    }
//    render(Commons.fail(null, 'INVALID_IDENTIFY_CODE', "验证码不正确")); return
//}
//
//mobileIdCode.validate_status = '1'
//mobileIdCode.date_created = new Timestamp(new Date().time)
//mobileIdCode.validate_count = 0
//dao.update(mobileIdCode)
render(Commons.success(reqNo: req.getParameter('reqNo')?:null, '验证成功'))
