package op

import org.jpos.space.SpaceFactory
import util.Commons
import util.Dao

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 联行号查询
 *
 * @author hanlei
 */

//HttpServletRequest req
//HttpServletResponse resp
//Dao dao

def keywordName = []
def keywordAddr = []
req.getParameter('keyword')?.split('\\s+')?.each {
	if (it in ['招商', '招行']) it = '招商银行'
	if (it in ['建行', '建设']) it = '建设银行'
    if (it in ['中行']) it = '中国银行'
	if (it in ['工行']) it = '工商银行'
    keywordName << "t.bank_name like '%${it}%'"
    keywordAddr << "t.bank_addr like '%${it}%'"
}

if (!keywordName||!keywordAddr) {
	render Commons.fail(null, 'ILLEGAL_ARGUMENT', '请输入关键字'); return
}

if (keywordName.size()>3 || keywordAddr.size()>3 ) {
	render Commons.fail(null, 'ILLEGAL_ARGUMENT', '输入的关键字个数过多'); return
}

def max = 20
def p = 1
try {
	max = Integer.parseInt(req.getParameter('max')?:'5')
} catch (ignore) {
	render Commons.fail(null, 'ILLEGAL_ARGUMENT', 'max param error'); return
}

try {
	p = Integer.parseInt(req.getParameter('p')?:'1')
	if (p < 1)  p = 1
} catch (ignore) {
	render Commons.fail(null, 'ILLEGAL_ARGUMENT', 'page param error'); return
}

def space = SpaceFactory.getSpace()
def cache_key = 'bankQuery:' + req.parameterMap.toString().encodeAsSHA1()
def cache = space.rdp(cache_key)
if (cache) {
	log.info "load bank query result from cache, key '$cache_key'"
	render Commons.success(cache); return
}

def offset = (p - 1) * max
def sql = "select * from dict_cnaps_no t where ( ${keywordName.join(' and ')} )"
def sqlwrap = "select * from (" + sql + ") a LIMIT ${offset},${max}"
def countwrap = "select count(0) total from (" + sql + ") b"
println("sqlwrap=${sqlwrap}")
println("countwrap=${countwrap}")
def banks = []
println("==========="+dao.db.rows(sqlwrap))
dao.db.rows(sqlwrap)?.each {
	banks << [bankDeposit: it.bank_name, unionBankNo:it.cnaps_no]
}
def total = dao.db.firstRow(countwrap)?.total as Long
total = Math.ceil(total / max) as Long

if (p > total && total != 0) {
	render Commons.fail(null, 'ILLEGAL_ARGUMENT', 'page bigger than total'); return
}

def result = [
		total: total,
		tip:total>10?'结果较多, 建议使用精确关键字, 例如添加地名等':null,
		reqNo: req.getParameter('reqNo')?:null,
		banks:banks
]

render Commons.success(result)

if (banks) {
	log.info "cache"
	space.out(cache_key, result, 3600000L)
}
