package op

import org.apache.tomcat.util.http.fileupload.FileItem
import util.Commons
import util.Constants
import javax.servlet.http.Cookie
import java.sql.Timestamp

/**
 * @author hanlei
 */

//HttpServletRequest req
//HttpServletResponse resp
//Dao dao

// request should be post
if (!'post'.equalsIgnoreCase(req.method)) {
	resp.status = 405
	resp.getOutputStream() << new File('static/405.html').newInputStream()
	return
}
def params = Commons.parseRequest(req)
log.info("params:${params}")
def ksnNo = params.ksnNo
def ksn = dao.findKSNByKSNNO(ksnNo)
if (!ksnNo || !ksn.id) {
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
}
//def activeCode = dao.findActiveCodeByKSNId(ksn.id)
def required = ['reqTime', 'ksnNo', 'appVersion', 'mobile', 'password','idCode']

def lastvalidparam = null
def miss = required.any{lastvalidparam = it; !(params[it])}
if (miss) {
	log.info "miss required param: ${lastvalidparam}"
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
}

def mobile = params.mobile
def idCode = params.idCode
def product = params.product

if (!(params.password ==~ /^[0-9A-Za-z]{6,16}$/)) {
	log.info 'password illegal!'
	render(Commons.fail(null, 'ILLEGAL_PASSWORD')); return
}
log.info("ksn:${ksn}")
if(!ksn){
    render(Commons.fail(null, 'SWIPER_NOT_EXIST')); return
}
//if (!ksn || !ksn.is_activated) {
//	render(Commons.fail(null, 'KSNNO_NOT_REGISTERED')); return
//}
if (!ksn.enable ||  (ksn.terminal_no != 'NULL' && ksn.terminal_no != null)) {
	render(Commons.fail(null, 'ILLEGAL_KSNNO', '刷卡器不能使用')); return
}

def agentNo = ksn.agent_no

def ver = Commons.versionParse(params.appVersion)
if (!ver) {
	render Commons.fail(null, 'ILLEGAL_ARGUMENT', 'app version error'); return
}

if (dao.findMobileMerchantByMobileNo(mobile)) {
	render(Commons.fail(null, 'MOBILE_EXIST')); return
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

def merchant_no_current_value = dao.db.firstRow("select * from sequence s where s.name='merchant_no_seq'").current_value as Integer
def merchant_no = '666' + merchant_no_current_value.toString().padLeft(12, '0')
dao.db.execute("update sequence set current_value=${merchant_no_current_value+1} where name='merchant_no_seq'")

def pos_merchant = [
		agent_no: agentNo,
		merchant_no: merchant_no,
        merchant_name: '',
        pinyin: '',
        short_name: '',
        sales_name: '',
        legal_person: '',
        mcc: '',
        province_code: '',
        city_code: '',
        address: '',
        biz_name: '',
        biz_phone: '',
        biz_email: '',
        remark: '',
        open_status: 1,
        settle_days: 1,
        settle_account_type: 'PRIVATE',
        settle_account_name: '',
        settle_bank_name: '',
        settle_bank_cnaps: '',
        settle_bank_no: '',
        permission: '1111101111',
        create_time: now,
        settle_flag: 0,
        account_status: 0,
        merchant_type: 'M',
        pay_type: 'B',
        type: '其他'
]

def mobile_merchant = [
		id_no: '',
		merchant_no: merchant_no,
		mobile_no: mobile,
        create_time: now,
        real_name_status: 0,
        product_code: product
]

def pos_terminal = []
log.info "pos_merchant $pos_merchant"
log.info "mobile_merchant $mobile_merchant"

dao.db.withTransaction {
    def terminal_no_current_value = (dao.db.firstRow("select * from sequence s where s.name='terminal_no_seq'").current_value as Integer) +1
	def terminal_no = terminal_no_current_value.toString().padLeft(8, '0')
    dao.db.execute("update sequence set current_value=${terminal_no_current_value} where name='terminal_no_seq'")
    //保存pos_merchnat
	dao.db.dataSet('pos_merchant').add(pos_merchant)
    //保存mobile_merchant
	dao.db.dataSet('mobile_merchant').add(mobile_merchant)
    //保存终端信息
	pos_terminal = [
			agent_no: agentNo,
			factory: 'dspread',
            model: ver.model,
			sn: '',
            open_status: 1,
            terminal_no: terminal_no,
            merchant_no: merchant_no,
            tmk: '14FF7AEF878B7F4FE80E3440789BC930',
            tmk_tpk: '4F2681FD7EAA45DA83CDF90FC78D37DB',
            tmk_tak: 'EE2EB1C31EAD70FB45D62DA2D4710A26',
            batch_no: '000001',
            voucher_no: '000001',
            last_check_in: now,
            create_time: now
	]
	dao.db.dataSet('pos_terminal').add(pos_terminal)
    //保存商户后台操作员
    def customer_user = [
            user_name: mobile,
            password: params.password.encodeAsSHA1(),
            status: 1,
            create_time: now,
            merchant_no: merchant_no
    ]
    dao.db.dataSet('customer_user').add(customer_user)

	ksn.terminal_no = terminal_no
	dao.update(ksn)
    //保存费率
//    def fee_type = activeCode.fee_type
//    def base_rate = null
//    switch (fee_type){
//        case '0.78' : base_rate = 0.78
//            break
//        case '0.5' : base_rate = 0.5
//            break
//        default : base_rate = 0.78
//    }
    def base_rate = 0.49
    def pos_rate = [
            acq_name : 'SELF',
            card_type : 'CREDIT',
            merchant_no : merchant_no,
            rate_type : 'SINGLE',
            base_rate : base_rate,
            top_rate : '',
            step_rate : ''
    ]
    dao.db.dataSet('pos_rate').add(pos_rate)
    pos_rate.card_type = 'DEBIT'
    dao.db.dataSet('pos_rate').add(pos_rate)
}
render(Commons.success(reqNo: req.getParameter('reqNo')?:null, '祝贺您成功注册, 请登录后进行认证.'))