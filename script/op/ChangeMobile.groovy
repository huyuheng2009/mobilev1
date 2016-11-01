package op

import util.Commons
import util.Dao

import javax.servlet.http.HttpServletRequest

/**
 * Created by zhanggc on 2014/7/24.
 */
//HttpServletRequest req;

def required = [ 'password', 'newMobile', 'idCode']
def miss = required.any { lastvalidparam = it; !(req.getParameter(it)) }
if(!req.getHeader("HFTNO")){
    miss = true
    lastvalidparam='HFTNO'
}
if (miss) {
    log.info "miss required param: ${lastvalidparam}"
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', "缺少参数" + lastvalidparam)); return
}
def sessionNo = req.getHeader('HFTNO')
def pwd = req.getParameter('password')
def mobile = req.getParameter('newMobile')
def idCode = req.getParameter('idCode')
log.info "params:HFTNO->${sessionNo},password->${pwd},newMobile->${mobile},idCode->${idCode}"
def session = dao.findSessionBySessionNo(sessionNo)
def merchantNo = session?.merchant_no
if (merchantNo) {
    def mobileIdentifyCode = dao.findeMobileIdentifyCodeByMobile(mobile)
    if (mobileIdentifyCode && mobileIdentifyCode.id_code == idCode) {
        def customer = dao.findCustomerUserByMerchantNo(merchantNo)
        def merchant = dao.findMobileMerchantByMerchantNo(merchantNo)
        if (merchant && customer) {
            merchant.mobile_no = mobile
            dao.update(merchant)
            customer.user_name = mobile
            dao.update(customer)
            render Commons.success(null, '修改成功'); return
        }
    } else {
        render Commons.fail(null, null, '验证码不对，修改失败'); return
    }
}

render Commons.fail(null, null, '缺失必要参数，修改失败');