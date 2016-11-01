package op

import util.Commons
import util.Dao

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 查询最后10笔
 *
 * @author hanlei
 */

//HttpServletRequest req
//HttpServletResponse resp
//Dao dao

def terminal = dao.findTerminalByMerchantNo(session.merchant_no)
def posMerchant = dao.findPosMerchantByMerchantNo(session.merchant_no)

log.info "terminal=${terminal},posMerchant=${posMerchant}"

def rows = dao.db.rows("""
	select * from trans_info where trans_status='SUCCESS'
	and terminal_no=${terminal?.terminal_no} and merchant_no=${terminal?.merchant_no}
   and trans_type='SALE'
	order by id desc
""", 0, 10)

def transactions = []
rows.each {
    def  cardbin = dao.findCardbin(it.card_no)
    transactions << [
            respNo: it.id,
            transType: 'SALE',
            voucherNo: it.terminal_voucher_no,
            respCode: it.response_code,
            amount:it.trans_amount+'',
            currency: 'CNY',
//                feeAmount:it.fee,
            transTime:(new Date(it.create_time.time)).format('yyyyMMddHHmmss'),
            refNo:it.terminal_ref_no,
            authNo:it.terminal_auth_no?it.terminal_auth_no:' ',
            cardTail:it.card_no[-4..-1],
            cardNoWipe:it.card_no[0..5] + '*****' + it.card_no[-4..-1],
            issuer:cardbin?cardbin.bank_name.substring(0,cardbin.bank_name.lastIndexOf("(")):null,
            batchNo:it.terminal_batch_no,
            ]
}
render Commons.success([
		respNo: req.getParameter('respNo')?:null,
		merchantName:posMerchant.merchant_name,
		merchantNo: posMerchant.merchant_no,
		terminalNo: terminal.terminal_no,
		transactions: transactions,
])