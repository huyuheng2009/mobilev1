package op

import org.apache.tomcat.util.http.fileupload.FileItem
import util.Commons

/**
 * Created by lei on 14-5-8.
 */
//发送签购单
if (!'post'.equalsIgnoreCase(req.method)) {
    resp.status = 405
    resp.getOutputStream() << new File('static/405.html').newInputStream()
    return
}

def params = Commons.parseRequest(req)
log.info "params: $params"
def required = ['appVersion','reqTime', 'reqNo', 'cardNoTail', 'transTime', 'amount']
def miss = required.any{lastvalidparam = it; !(params.containsKey(it))}
if (miss) {
    log.info "miss required param: ${lastvalidparam}"
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', "缺少参数" + lastvalidparam)); return
}
def ver = Commons.versionParse(params.get('appVersion'))
def signature;
if(ver.model=='hft'){
    signature = '和付通'
}else if(ver.model=='zlzf'){
    signature = '招联支付'
}else{
    signature = '和付通'
}
if(params.mobile){
    Commons.newSendMsg(params.mobile, "您尾号${params.cardNoTail}的银行卡于${params.transTime}消费人民币${params.amount}元。【${signature}】")
}

if(params.mail){
    def salesSlip = params.salesSlip as FileItem

    if (!salesSlip || salesSlip.isFormField()) {
        render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
    }
    def file = null
    if (salesSlip) {
        def rootDir = new File('uploads/salesSlip/')
        salesSlipUri = "${params.cardNoTail}_${params.transTime}.png"
        file = new File(rootDir, salesSlipUri)
        if (!file.parentFile.exists()) file.parentFile.mkdirs()
        log.info "salesSlipUri: $salesSlipUri"
        salesSlip.write(file)
        salesSlip.delete()
    }
    Commons.sendEamil(subject: "${signature}签购单",
            to: ["${params.mail}"],
            content: "您尾号${params.cardNoTail}的银行卡于${params.transTime} 消费人民币${params.amount}元，附件中是签购单",
            attachment: [
                    [name: "交易签购单.png", file: file]
            ])
}
render(Commons.success(['reqNo':params.reqNo?:null]))