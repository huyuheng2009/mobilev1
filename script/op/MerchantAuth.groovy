package op

import org.apache.tomcat.util.http.fileupload.FileItem
import util.Commons

import java.sql.Timestamp

/**
 * Created with IntelliJ IDEA.
 * User: hanlei
 * Date: 13-8-15
 * Time: 上午11:44
 * To change this template use File | Settings | File Templates.
 */
if (!'post'.equalsIgnoreCase(req.method)) {
    resp.status = 405
    resp.getOutputStream() << new File('static/405.html').newInputStream()
    return
}
def params = Commons.parseRequest(req)
log.info('params='+params)
def required = ['reqTime',  'companyName', 'businessLicense', 'regPlace', 'business']
def miss = required.any{lastvalidparam = it; !(params.containsKey(it))}
if (miss) {
    log.info "miss required param: ${lastvalidparam}"
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', "缺少参数" + lastvalidparam)); return
}
def companyName = URLDecoder.decode(params.companyName?.trim(), 'UTF-8')
def businessLicense = params.businessLicense?.trim()
def regPlace = URLDecoder.decode(params.regPlace?.trim(), 'UTF-8')

def merchant = dao.findCmMerchantById(wssession.merchant_id)
if(!merchant){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '没有找到session中对应的商户')); return
}
if(merchant && (merchant.review_status == 'submit' || merchant.review_status == 'accept')){
    render(Commons.fail(null, 'PERMISSION_DENIED', '抱歉, 您不能进行此操作')); return
}
def personal = dao.findCmPersonalById(wssession.merchant_id)

def business = params.business as FileItem
def gate = params.gate as FileItem
def businessPlace = params.businessPlace as FileItem
def cashierDesk = params.cashierDesk as FileItem

if(!business || business.isFormField()){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
}
if(business.size < 1024 || business.size > 1024*1024){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '图片大小不合格')); return
}

def rootDir = new File('uploads/merchant/')
def businessUri = "q${merchant.merchant_no}.png"
def businessFile = new File(rootDir, businessUri)
business.write(businessFile)
business.delete()

def operator = dao.findMerchantOperatorByMerchantId(merchant.id)

def model = operator?.login_name.split('\\.')[1]

if(model == 'zfmini'){
    if(!gate || gate.isFormField() || !businessPlace || businessPlace.isFormField() || !cashierDesk || cashierDesk.isFormField()){
        render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
    }
    if(gate.size < 1024 || gate.size > 512*1024 || businessPlace.size < 1024 || businessPlace.size > 512*1024 || cashierDesk.size < 1024 || cashierDesk.size > 512*1024){
        render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '图片大小不合格')); return
    }
    def gateUri = "g${merchant.merchant_no}.png"
    def businessPlaceUri = "n${merchant.merchant_no}.png"
    def cashierDeskUri = "d${merchant.merchant_no}.png"
    def gateFile = new File(rootDir, gateUri)
    def businessPlaceFile = new File(rootDir, businessPlaceUri)
    def cashierDeskFile = new File(rootDir, cashierDeskUri)
    gate.write(gateFile)
    gate.delete()
    businessPlace.write(businessPlaceFile)
    businessPlace.delete()
    cashierDesk.write(cashierDeskFile)
    cashierDesk.delete()
}
merchant.last_updated = new Timestamp(new Date().time)
merchant.merchant_name = companyName
merchant.review_status = 'submit'
dao.update(merchant)

personal.business_license_code = businessLicense
personal.business_place = regPlace
personal.merchant_reason = ''
dao.update(personal)
render(Commons.success(['reqNo':params.reqNo?:null]))