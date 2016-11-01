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
def required = ['reqTime',  'name', 'idNumber', 'personal', 'personalBack', 'personalPic']
def miss = required.any{lastvalidparam = it; !(params.containsKey(it))}
if (miss) {
    log.info "miss required param: ${lastvalidparam}"
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', "缺少参数" + lastvalidparam)); return
}
def personal = params.personal as FileItem
def personalBack = params.personalBack as FileItem
def personalPic = params.personalPic as FileItem
if (!personal || personal.isFormField() || !personalBack || personalBack.isFormField() || !personalPic || personalPic.isFormField()) {
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT')); return
}
if(personal || personalBack || personalPic){
     if(personal.size < 1024 || personal.size > 2*1024*1024 || personalBack.size < 1024 || personalBack.size > 2*1024*1024
        || personalPic.size < 1024 || personalPic.size > 2*1024*1024){
         render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '图片大小不合格')); return
     }
}
def rootDir = new File('uploads/merchant/')
def posMerchant = dao.findPosMerchantByMerchantNo(session.merchant_no)
def mobileMerchant = dao.findMobileMerchantByMerchantNo(session.merchant_no)
if(!posMerchant || !mobileMerchant){
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '没有找到对应的商户')); return
}
if(mobileMerchant && (mobileMerchant.real_name_status == '1' || mobileMerchant.real_name_status == '3')){
    render(Commons.fail(null, 'PERMISSION_DENIED', '抱歉, 您不能进行此操作')); return
}

def personalUri = "f${posMerchant.merchant_no}.png"
def personalBackUri = "b${posMerchant.merchant_no}.png"
def personalPicUri = "p${posMerchant.merchant_no}.png"
def personalFile = new File(rootDir, personalUri)
def personalBackFile = new File(rootDir, personalBackUri)
def personalPicFile = new File(rootDir, personalPicUri)
if(personalFile.parentFile.exists())  personalFile.parentFile.mkdirs()
personal.write(personalFile)
personal.delete()
personalBack.write(personalBackFile)
personalBack.delete()
personalPic.write(personalPicFile)
personalPic.delete()

posMerchant.merchant_name = params.name
println('posMerchant='+posMerchant)
dao.update(posMerchant)

mobileMerchant.real_name_status = 1 //0未提交，1已提交，2通过，3未通过
mobileMerchant.id_no = params.idNumber?.trim()
dao.update(mobileMerchant)

render(Commons.success(['reqNo':params.reqNo?:null]))