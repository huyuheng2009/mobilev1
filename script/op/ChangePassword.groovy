package op

import util.Commons
import util.Dao

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.sql.Timestamp

//HttpServletRequest req
//HttpServletResponse resp
//Dao dao

def required = ['reqTime', 'password', 'newPassword']
def miss = required.any{lastvalidparam = it; !(req.getParameter(it))}
if (miss) {
	log.info "miss required param: ${lastvalidparam}"
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT', "缺少参数" + lastvalidparam)); return
}

def customerUser = dao.findCustomerUserByMerchantNo(session.merchant_no)
if (!customerUser) {
	render(Commons.fail(null, 'NOT_FOUND_USER')); return
}

def password = req.getParameter('password')
def newpassowrd = req.getParameter('newPassword')

if (!(newpassowrd ==~ /^[0-9A-Za-z]{6,16}$/)) {
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '密码至少是6位, 最多16位')); return
}

if ((password).encodeAsSHA1() != customerUser.password) {
	render(Commons.fail(null, 'ILLEGAL_PASSWORD')); return
}

customerUser.password = newpassowrd.encodeAsSHA1()
dao.update(customerUser)

render Commons.success([reqNo: req.getParameter('reqNo')?:null])
