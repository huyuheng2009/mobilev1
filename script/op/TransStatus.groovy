package op

import util.Commons
import util.Dao

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 交易状态查询
 *
 * @author yinheli
 */

//HttpServletRequest req
//HttpServletResponse resp
//Dao dao

if (!'post'.equalsIgnoreCase(req.method)) {
	resp.status = 405
	resp.getOutputStream() << new File('static/405.html').newInputStream()
	return
}

def required = ['reqTime', 'reqNo', 'origReqTime', 'origReqNo', 'origTransType', 'amount']
def lastvalidparam = null
def miss = required.any{lastvalidparam = it; !(req.getParameter(it))}
if (miss) {
	log.info "miss required param: ${lastvalidparam}"
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
}

def origReqNo = req.getParameter('origReqNo')
def origTransType = req.getParameter('origTransType')
def amount = req.getParameter('amount')

if (origTransType != 'sale') {
	render Commons.fail(null, 'ILLEGAL_ARGUMENT', '不支持的交易'); return
}

def terminal = dao.findPosTerminalByKsnNo(params.ksnNo)
def batch = "${terminal.batch_no}".padLeft(6, '0')
def trace = "${origReqNo}".padLeft(6, '0')

def ori = dao.db.firstRow("""
		select * from trans_info where reference_no=${oriWSTrans.reference_no} or (
			terminal_batch_no=${batch} and terminal_voucher_no=${trace} and terminal_no=${terminal.terminal_no} and merchant_no=${terminal.merchant_no}
			and amount=${amount as Long}
		)
	""")

if (!ori || ori.trans_status == 'INIT') {
	render Commons.fail(null, 'PROCESSING'); return
}

log.info "ori trans: ${ori}"

if (ori.trans_status == 'SUCCESS') {
	def cardbin = dao.findCardbin(ori.card_no)
	render Commons.success([
			reqNo: ori.terminal_voucher_no,
			merchantName: ori.merchant_name,
			merchantNo: terminal.merchant_no,
			terminalNo: terminal.terminal_no,
			cardNoWipe: ori.card_no[0..5] + '*****' + ori.card_no[-4..-1],
			amount:"${oriWSTrans.amount}".padLeft(12, '0'),
			currency:oriWSTrans.currency?:'CNY',
			issuer:cardbin?cardbin.issuer_name:null,
			voucherNo:"${oriWSTrans.trace_no}".padLeft(6, '0'),
			batchNo:"${oriWSTrans.batch_no}".padLeft(6, '0'),
			transTime:oriWSTrans.time_create?.timestampValue()?.format('yyyyMMddHHmmss'),
			refNo:ori.reference_no,
			authNo:ori.acq_auth_no?:' ',
	])
	dao.update(oriWSTrans)
} else {
	render Commons.fail(null, ori.resp_code?:'96')
}
