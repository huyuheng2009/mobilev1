package op

import org.apache.tomcat.util.http.fileupload.FileItem
import util.Commons

import java.sql.Timestamp
import java.util.regex.Pattern

/**
 * Created with IntelliJ IDEA.
 * User: hanlei
 * Date: 13-8-15
 * Time: 上午11:45
 * To change this template use File | Settings | File Templates.
 */
if (!'post'.equalsIgnoreCase(req.method)) {
    resp.status = 405
    resp.getOutputStream() << new File('static/405.html').newInputStream()
    return
}
def params = Commons.parseRequest(req)
log.info('params='+params)
def required = ['reqTime',  'name', 'bankDeposit',  'unionBankNo', 'accountNo', 'card','isSendMsg','appVersion']
def miss = required.any{lastvalidparam = it; !(params.containsKey(it))}
if (miss) {
    log.info "miss required param: ${lastvalidparam}"
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', "缺少参数" + lastvalidparam)); return
}
def ver = Commons.versionParse(params.get('appVersion'))
def accountNo = params.accountNo
def cardbin = dao.findCardbin(accountNo)
if(!cardbin){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '不支持卡')); return
}
def posMerchant = dao.findPosMerchantByMerchantNo(session.merchant_no)
if(!posMerchant){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '没有找到对应的商户')); return
}
def card = params.card as FileItem
if("借记卡" != cardbin.card_type){
    render(Commons.fail(null, 'CREDIT_CARD_CANOT_REGISTER', '信用卡不能注册')); return
}

if(posMerchant.account_status == '1' || posMerchant.account_status == '2'){
    render(Commons.fail(null, 'PERMISSION_DENIED', '抱歉, 您不能进行此操作')); return
}

if(!card || card.isFormField()){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
}
if(card.size < 1024 || card.size > 2*1024*1024){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '图片大小不合格')); return
}

def rootDir = new File('uploads/merchant/')
def cardUri = "c${posMerchant.merchant_no}.png"
def cardFile = new File(rootDir, cardUri)
card.write(cardFile)
card.delete()

posMerchant.settle_bank_name = params.bankDeposit?.trim()
posMerchant.settle_account_name = params.name?.trim()
posMerchant.account_status = '1'
posMerchant.pinyin = Commons.getPinYinHeadChar(params.name?.trim())
posMerchant.settle_bank_cnaps = params.unionBankNo?.trim()
posMerchant.settle_bank_no = params.accountNo?.trim()

log.info "posMerchant $posMerchant"
dao.update(posMerchant)

def signature;
if(ver.model=='hft'){
    signature = '和付通'
}else if(ver.model=='zlzf'){
    signature = '招联支付'
}else{
    signature = '和付通'
}
if('1'==params.get('isSendMsg')){
    def mobileMerchant = dao.findMobileMerchantByMerchantNo(session.merchant_no)
    if(mobileMerchant&&mobileMerchant.mobile_no){
        log.info "账户认证短信通知 $mobileMerchant.mobile_no"
        Commons.newSendMsg("$mobileMerchant.mobile_no","您已更改结算账户为${posMerchant.settle_bank_no}，请知悉【${signature}】")
    }
}

render(Commons.success(['reqNo':params.reqNo?:null]))