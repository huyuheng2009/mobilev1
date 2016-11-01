package op

import org.jpos.security.SMAdapter
import org.jpos.space.SpaceFactory
import util.Commons
import util.Constants
import util.Dao
import util.Des3
import util.JCEHandler

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession
import java.sql.Timestamp

/**
 * @author hanlei
 */

//HttpServletRequest req
//HttpServletResponse resp
//HttpSession session
//Dao dao

// request should be post
if (!'post'.equalsIgnoreCase(req.method)) {
	resp.status = 405
	resp.getOutputStream() << new File('static/405.html').newInputStream()
	return
}
def required = ['reqTime', 'loginName', 'password', 'appVersion']
def miss = required.any{lastvalidparam = it;  !(req.getParameter(it))}
if (miss) {
	log.info "miss required param: ${lastvalidparam}"
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT', "缺少参数" + lastvalidparam)); return
}

def ver = Commons.versionParse(req.getParameter('appVersion'))
if (!ver) {
	render Commons.fail(null, 'ILLEGAL_ARGUMENT', 'app version error'); return
}

def password = req.getParameter('password')
def customerUser = dao.findCustomerUserByLoginName(req.getParameter('loginName'))
println("password:"+password);
println(password.encodeAsSHA1());
if (!customerUser || customerUser.password != password.encodeAsSHA1()) {
    render(Commons.fail(null, 'ILLEGAL_LOGIN_OR_PASSWD')); return
}
def posMerchant = dao.findPosMerchantByMerchantNo(customerUser?.merchant_no)
def mobileMerchant = dao.findMobileMerchantByMerchantNo(customerUser?.merchant_no)
def posTerminal = dao.findTerminalByMerchantNo(customerUser?.merchant_no)
log.info("posMerchant=${posMerchant}     mobileMerchant=${mobileMerchant}     pos_terminal=${posTerminal}")
if(!posMerchant || !posTerminal || !posTerminal){
    render(Commons.fail(null, 'USER_NOT_AVAILABLE')); return
}

def ksnInfo = dao.findKsnInfoByTerminalNo(posTerminal.terminal_no)
log.info("ksnInfo=${ksnInfo}")
if(!ksnInfo){
    render(Commons.fail(null, 'ILLEGAL_KSNNO')); return
}

def realStatus = mobileMerchant.real_name_status
def accountStatus = posMerchant.account_status
def status = realStatus.toString()+accountStatus.toString()
log.info('status='+status)
def nextReqNo = Integer.parseInt(posTerminal?.voucher_no)+1
posTerminal.voucher_no = nextReqNo.toString().padLeft(6, '0')

//后台增加二审，前段需要特别处理
if(realStatus == 4){
    realStatus = 2
}else if(realStatus == 2){
    realStatus = 1
}else if(realStatus == 5){
    realStatus = 3
}
if(accountStatus == 4){
    accountStatus = 2
}else if(accountStatus == 2){
    accountStatus = 1
}else if(accountStatus == 5){
    accountStatus = 3
}
def result = [
        realStatus: realStatus,
        accountStatus: accountStatus,
        model : ksnInfo?.model,
		nextReqNo : nextReqNo,
        ksnNo : ksnInfo?.ksn_no
]
def tpk = JCEHandler.generateDESKey(SMAdapter.LENGTH_DES3_2KEY)
def tmkTpk = JCEHandler.encryptData(tpk, posTerminal?.tmk)
posTerminal.tmk_tpk = tmkTpk
dao.update(posTerminal)

if(ksnInfo?.model == Constants.SHUA_MODEL){
    result << [
        key : Des3.encode(tpk, Constants.TRANSFER_KEY),
    ]
}

//TODO小刷卡器返回秘钥
def now = new Timestamp(new Date().time)
def hftsession = [
		create_time: now,
		last_update_time: now,
		expiry_time: new Timestamp(new Date().time + 30 * 60 * 1000),
		ip: ip,
		session_no: session?.id?:UUID.randomUUID().toString().replaceAll('-',''),
		merchant_no: posMerchant?.merchant_no,
]

// delete ksn sessions
dao.db.executeUpdate("delete from mobile_session where merchant_no=${posMerchant?.merchant_no}")
dao.db.dataSet('mobile_session').add(hftsession)
hftsession = dao.wrap(hftsession, 'mobile_session')
println("hftsession=${hftsession}")
session?.setAttribute('sid', hftsession.merchant_no)
resp.setHeader('HFTNO', hftsession.session_no)
log.info "session no:${hftsession.session_no}"
def reqMsg = ''
if(status.indexOf('3') != -1){
    reqMsg = '认证有未通过情况，请重新提交资质'
}else if(status.indexOf('5') != -1){
    reqMsg = '认证有未通过情况，请重新提交资质'
}else if(status == '44'){
    reqMsg = '登录成功'
}else if(status.indexOf('0') != -1){
    reqMsg = '认证有未提交情况，请注意提交'
}else {
    reqMsg = '认证审核中，请耐心等候'
}
render(Commons.success(result, reqMsg))