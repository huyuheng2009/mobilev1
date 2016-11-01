package op

import util.Commons
import util.Constants
/**
 * 刷卡器激活
 *
 * @author hanlei
 */

if (!'post'.equalsIgnoreCase(req.method)) {
	resp.status = 405
	return
}

def ksnNo = req.getParameter('ksnNo')
def code = req.getParameter('activeCode')
if (!ksnNo || !code) {
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
}

def ksn = dao.findKSNByKSNNO(ksnNo)
def activeCode = dao.findActiveCodeByCode(code)


if(!ksn || !activeCode){
    render(Commons.fail(null, 'KSNNO_OR_ACTIVECODE_NOT_EXIST')); return
}

log.info "ksn: (${ksn?.id}, ${ksn?.ksn_no}, is_used:${ksn?.enable}, is_activated:${ksn?.is_activated})",
		 "activeCode: (${activeCode?.id}, ${activeCode?.code}, is_used:${activeCode?.enable},fee_type:${activeCode?.fee_type})"

if (!ksn.enable || !activeCode.enable) {
	render(Commons.fail(null, 'KSNNO_OR_ACTIVECODE_NOT_ENABLE')); return
}
if(ksn.is_activated && activeCode.ksn_id != ksn.id){
    render(Commons.fail(null, 'KSNNO_IS_NOT_SAME','两次激活的序列号不一致')); return
}

def appVersion = req.getParameter('appVersion')
if(!appVersion){
    render Commons.fail(null, 'ILLEGAL_ARGUMENT', "缺少参数appVersion"); return
}
def ver = Commons.versionParse(appVersion)
if (!ver) {
    render Commons.fail(null, 'ILLEGAL_ARGUMENT', 'app version error'); return
}

ksn.is_activated = true
activeCode.ksn_id  = ksn.id
activeCode.is_actived = true

dao.update(ksn)
dao.update(activeCode)

def result = [
        respNo: req.getParameter('reqNo')?:null
]
render Commons.success(result)