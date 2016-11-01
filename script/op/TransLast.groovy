package op

import util.Commons
import util.Dao

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.sql.Timestamp
import java.text.SimpleDateFormat

/**
 * 查询最后一笔
 *
 * @author yinheli
 */
//HttpServletRequest req
//HttpServletResponse resp
//Dao dao

def terminal = dao.findTerminalById(wssession.terminal_id)
def merchant = dao.findTerminalByMerchantId(wssession.merchant_id)

def rows = dao.db.rows("""
	select * from ws_trans where comp_status=2 and trans_status=1
	and ksn_no=${terminal?.terminal_no}
	order by id desc
""", 0, 1)

def transactions = []
rows.each {
    if(it.trans_type == 'sale'){
        def  cardbin = dao.findCardbin(it.card_no)
        transactions << [
                respNo: it.id,
                transType: 'sale',
                voucherNo: it.trace_no,
                respCode: it.resp_code,
                amount:it.amount,
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
                amount:it.amount,
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