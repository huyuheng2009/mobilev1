package op

import org.apache.tomcat.util.http.fileupload.FileItem
import util.Commons

/**
 * echo
 *
 * @author yinheli
 */

//HttpServletRequest req
//HttpServletResponse resp
//Dao dao

if (!'post'.equalsIgnoreCase(req.method)) {
	resp.status = 405
	resp.getOutputStream() << new File('static/405.html').newInputStream()
	return
}

def matcher = (req.requestURI =~ '/upload/([a-zA-Z0-9]+)?/?')
def type = matcher?matcher[0][1]:null
log.info "type $type"
if (!(type in ['business', 'personal', 'personalBack'])) {
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '未知上传类型, 支持:营业执照, 身份证正反面')); return
}

def params = Commons.parseRequest(req)
log.info "params: $params"

def photo = params.photo as FileItem
if (!photo) {
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT', 'PHOTO MISSING')); return
}

if (photo.isFormField()) {
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT', 'Please upload certificate file')); return
}

if (!(photo.contentType?.toLowerCase().startsWith('image')) && photo.contentType != 'application/octet-stream') {
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '请上传图片')); return
}

if (photo.size < 1024 * 10) {
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '图片大小不合格')); return
}

if (photo.size > Commons.getConfig().commons.maxFileUpload) {
	render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '文件过大')); return
}

def rootDir = new File('uploads/merchant/')

def merchant = dao.findMerchantById(wssession.merchant_id)

//def sufix = photo.name[photo.name.lastIndexOf('.')+1..-1].toLowerCase()
def typePrefix = [
        'personal'    : 'f',
        'personalBack': 'b',
        'business'    : 'q',
]
def fileUri = "${typePrefix.get(type)}${merchant.merchant_no}.png"
def file = new File(rootDir, fileUri)
if (!file.parentFile.exists()) file.parentFile.mkdirs()
photo.write(file)
photo.delete()

// update status
dao.db.executeUpdate("""update cm_merchant set qualification_status=0, review_status='submit' where id=${merchant.id}""")

render(Commons.success(['reqNo':params.reqNo?:null]))