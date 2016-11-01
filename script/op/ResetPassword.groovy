package op

import util.Commons
import java.sql.Timestamp
import util.Constants

/**
 * @author yinheli
 */

//HttpServletRequest req
//HttpServletResponse resp
//Dao dao

def required = ['reqTime', 'mobile', 'idCode', 'password']
def miss = required.any{lastvalidparam = it; !(req.getParameter(it))}
if (miss) {
	log.info "miss required param: ${lastvalidparam}"
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT', "缺少参数" + lastvalidparam)); return
}

def mobile = req.getParameter('mobile')?.trim()
def idCode = req.getParameter('idCode')?.trim()
def password = req.getParameter('password')?.trim()
if (!(mobile =~ /^(13|14|15|18)\d{9}$/)) {
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '手机号有误')); return
}
def customer = dao.findCustomerUserByLoginName(mobile)
def mobileMerchant = dao.findMobileMerchantByMobileNo(mobile)
if(!customer || !mobileMerchant){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '无效手机号')); return
}

def mobileIdCode = dao.findeMobileIdentifyCodeByMobile(mobile)
if(!mobileIdCode){
    render(Commons.fail(null, 'NOT_VALIDATE','该手机号没有验证')); return
}

mobileIdCode.validate_count = mobileIdCode.validate_count + 1

if(!mobileIdCode || mobileIdCode.id_code != idCode){
    dao.update(mobileIdCode)
    if(mobileIdCode.validate_count > 10){
        render(Commons.fail(null, 'REQUEST_TOO_OFFEN', "请求太频繁")); return
    }
    render(Commons.fail(null, 'INVALID_IDENTIFY_CODE', "验证码不正确")); return
}

def now = new Timestamp(new Date().time)

mobileIdCode.validate_status = '1'
mobileIdCode.create_time = now
mobileIdCode.validate_count = 0
dao.update(mobileIdCode)

customer.password = password.encodeAsSHA1()
dao.update(customer)

render Commons.success([reqNo: req.getParameter('reqNo')?:null])