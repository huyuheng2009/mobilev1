package op

import util.Commons
import util.Constants
/**
 * 刷卡器重置
 *
 * @author yinheli
 */

//HttpServletRequest req
//HttpServletResponse resp
//Dao dao

if (!'post'.equalsIgnoreCase(req.method)) {
	resp.status = 405
	return
}

if (!wssession) {
	render(Commons.fail(null, 'SESSION_TIMEOUT')); return
}

def idNumber = req.getParameter('idNumber')
def ksnNo = req.getParameter('ksnNo')

if (!ksnNo || !idNumber) {
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
}
def model = Commons.getModelByKsnNo(ksnNo)
if (model != Constants.AISHUA_MODEL && model != Constants.WOSHUA_MODEL && model !=Constants.ZFSHUA_MODEL) {
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '非法请求')); return
}

def ksn = dao.findKSNByKSNNO(ksnNo)
def terminal = dao.findTerminalById(ksn?.terminal_id)
def merchant = dao.findMerchantById(wssession.merchant_id)

log.info "ksn: ${ksn?.ksn_no}"
log.info "terminal:[${terminal?.merchant_no}, ${terminal?.terminal_no}]", "merchant:${merchant?.merchant_no}"

if (!ksn || !merchant || !terminal) {
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '请使用原刷卡器进行匹配')); return
}

if(terminal.merchant_no != merchant.merchant_no){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '请使用原刷卡器进行匹配')); return
}

if(merchant.id_no != 'NULL'){
    if(idNumber != merchant.id_no){
        render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '身份证号不匹配')); return
    }
}


def result = [
	respNo: req.getParameter('respNo')?:null,
]
if(model == Constants.WOSHUA_MODEL || model == Constants.AISHUA_MODEL || model == Constants.ZFSHUA_MODEL){
    def key = Commons.genTMK(ksn.ksn_no)
    result << [
            baseKey: key[0],
            keyCheck: key[1],
    ]
}

render(Commons.success(result))

// logout
dao.db.executeUpdate("delete from ws_session where ksn=${ksnNo}")
session?.invalidate()