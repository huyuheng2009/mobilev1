package op

import org.jpos.security.SMAdapter
import util.Commons
import util.Constants
import util.Des3
import util.JCEHandler

/**
 * 刷卡器替换
 *
 * @author hanlei
 */
//HttpServletRequest req
//HttpServletResponse resp
//Dao dao

if (!'post'.equalsIgnoreCase(req.method)) {
	resp.status = 405
	return
}

if (!session) {
	render(Commons.fail(null, 'SESSION_TIMEOUT')); return
}

def ksnNo = req.getParameter('ksnNo')

if (!ksnNo) {
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
}

def terminal = dao.findTerminalByMerchantNo(session.merchant_no)
def ksn = dao.findKsnInfoByTerminalNo(terminal.terminal_no)
//def activeCode = dao.findActiveCodeByKSNId(ksn.id)

def oldKsn = ksn
def newKsn = dao.findKSNByKSNNO(ksnNo)

log.info "old: $oldKsn", "new: $newKsn"

if (!newKsn || !newKsn.enable) {
	render Commons.fail(null, 'SWIPER_NOT_EXIST'); return
}

if (oldKsn.id == newKsn.id || (newKsn.terminal_no != 'NULL' && newKsn.terminal_no != null)) {
	render Commons.fail(null, 'ILLEGAL_ARGUMENT', '请使用新刷卡器'); return
}

oldKsn.is_activated = true
oldKsn.enable = true
oldKsn.terminal_no = 'NULL'

newKsn.is_activated = true
newKsn.enable = true
newKsn.terminal_no = terminal.terminal_no

//activeCode.ksn_id = newKsn.id

def result = [
    respNo: req.getParameter('respNo')?:null,
    model : newKsn?.model
]

def tpk = JCEHandler.generateDESKey(SMAdapter.LENGTH_DES3_2KEY)
def tmkTpk = JCEHandler.encryptData(tpk, terminal?.tmk)
terminal.tmk_tpk = tmkTpk
dao.update(terminal)

if(newKsn?.model == Constants.SHUA_MODEL){
    result << [
            key : Des3.encode(tpk, Constants.TRANSFER_KEY),
    ]
}

render(Commons.success(result))

dao.db.withTransaction {
    dao.update(oldKsn)
    dao.update(newKsn)
//    dao.update(activeCode)
}