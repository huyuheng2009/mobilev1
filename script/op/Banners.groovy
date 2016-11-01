package op

import util.Commons
import util.Dao

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

//HttpServletRequest req
//HttpServletResponse resp
//HttpSession session
//Dao dao
def required = [ 'appVersion']
def miss = required.any{lastvalidparam = it;  !(req.getParameter(it))}
if (miss) {
    log.info "miss required param: ${lastvalidparam}"
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', "缺少参数" + lastvalidparam)); return
}
def ver = Commons.versionParse(req.getParameter('appVersion'))
def product_code = '';
if(ver.model=='hft'){
    product_code = "'HFT'"
}else if(ver.model=='zlzf'){
    product_code = "'ZLZF'"
}else{
    render(Commons.fail(null, 'ILLEGAL_ARGUMENT', "产品代码 不合法" + ver.model)); return
}
def sql = 'select * from mobile_banner where product_code='+product_code
def banner = []
dao.db.rows(sql, 0, 10)?.each {
    banner << [
            bannerId: it.id,
            bannerName: it.name,
            imageURI: it.uri,
            jumpType: it.jump_type,
            jumpAddr: it.jump_address,
    ]
}
render Commons.success([respNo: req.getParameter('reqNo')?:null, banners:banner])