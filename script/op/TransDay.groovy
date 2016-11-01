package op

import util.Commons
import util.Dao

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.sql.Timestamp
import java.text.SimpleDateFormat

/**
 * 按日期交易记录查询
 * 只查询状态成功且可以展现的交易
 *
 * @author yinheli
 */
//
//HttpServletRequest req
//HttpServletResponse resp
//Dao dao

def date = req.getRequestURI().replace('/transactions/', '').replaceAll('/', '')
if (!date =~ /\d{8}/) {
	render Commons.fail(null, 'ILLEGAL_ARGUMENT', '不是合法的日期')
}
def sdf = new SimpleDateFormat('yyyyMMddHHmmss')
def start = sdf.parse(date + '000000')
def end = sdf.parse(date+'235959')

def terminal = dao.findTerminalById(wssession.terminal_id)
def merchant = dao.findMerchantById(wssession.merchant_id)

def rows = dao.db.rows("""
	select * from ws_trans where comp_status=2 and trans_status=1
	and terminal_no=${terminal?.terminal_no}
	and time_create >= ${new Timestamp(start.time)} and time_create <= ${new Timestamp(end.time)}
	order by id asc
""")

def transactions = []
rows.each {
    if(it.trans_type == 'sale'){
        def  cardbin = dao.findCardbin(it.card_no)
        transactions << [
                respNo: it.id,
                transType: 'sale',
                voucherNo: it.trace_no,
                respCode: it.resp_code,
                amount:it.amount+'',
                currency:it.currency,
//                feeAmount:it.fee,
                transTime:(new Date(it.time_create.timestampValue().time)).format('yyyyMMddHHmmss'),
                refNo:it.reference_no,
                authNo:it.auth_no,
                cardTail:it.card_no[-4..-1],
                cardNoWipe:it.card_no_wipe,
                operatorNo: '01',
                issuer:cardbin?cardbin.issuer_name:null,
                batchNo:it.batch_no,
        ]
    }else{
        transactions << [
                respNo: it.id,
                transType: 'sale',
                voucherNo: it.trace_no,
                respCode: it.resp_code,
                amount:it.amount+'',
                currency:it.currency,
//                feeAmount:it.fee,
                transTime:(new Date(it.time_create.timestampValue().time)).format('yyyyMMddHHmmss'),
                refNo:it.reference_no,
                authNo:it.auth_no,
                cardTail:it.card_no[-4..-1],
        ]
    }

}

render Commons.success([
		respNo: req.getParameter('respNo')?:null,
		merchantName:merchant.merchant_name,
		merchantNo: merchant.merchant_no,
		terminalNo: terminal.terminal_no,
		transactions: transactions,
])