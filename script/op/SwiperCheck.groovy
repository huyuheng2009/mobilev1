package op

import util.Commons
import util.Constants

/**
 * Created with IntelliJ IDEA.
 * User: hanlei
 * Date: 13-11-29
 * Time: 下午3:47
 * To change this template use File | Settings | File Templates.
 */
if (!'post'.equalsIgnoreCase(req.method)) {
    resp.status = 405
    return
}

def ksnNo = req.getParameter('ksnNo')
def appVersion = req.getParameter('appVersion')
def ver = Commons.versionParse(appVersion)
if (!ksnNo) {
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
}

if (!ver) {
    render Commons.fail(null, 'ILLEGAL_ARGUMENT', 'app version error')
}

def ksn = dao.findKSNByKSNNO(ksnNo)
if(!ksn){
    render(Commons.fail(null, 'SWIPER_NOT_EXIST')); return
}
log.info "ksn: ${ksn}"
def result = [
        respNo: req.getParameter('reqNo')?:null,
]
if(!(ksn.terminal_no == 'NULL' || ksn.terminal_no == null || ksn.terminal_no == '')){
    render(Commons.fail(null, 'KSNNO_REGISTERED')); return
}
if(ksn.enable == 1){
    render Commons.success(result)
}

//if(ksn.is_activated == 0 && ksn.enable == 1){
//    render Commons.success(result)
//}else if(ksn.is_activated == 1 && ksn.enable == 1){
//    render (Commons.fail(result, 'KSNNO_ACTIVATED', '刷卡器激活过')); return
//}else{
//    render(Commons.fail(null, 'KSNNO_NOT_AVAILABLE','请确认刷卡器是未使用过')); return
//}