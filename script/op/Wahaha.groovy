package op

import util.Commons

/**
 * Created with IntelliJ IDEA.
 * User: hanlei
 * Date: 13-10-30
 * Time: 下午3:06
 * To change this template use File | Settings | File Templates.
 */
def p = Integer.parseInt(req.getParameter('p')?:'1')
def max = 15
if (p < 1)  p = 1
def offset = (p - 1) * max
def sql = """select t.mobile_no as mobile,t.id_code as code,t.date_created,t.ksn_no as ksn,t.status,p.id
from WS_IDENTIFY_CODE t left join cm_personal p on p.mobile_no = t.mobile_no order by t.date_created desc"""
def sqlwrap = "select * from (select page_.*, rownum rownum_ from (" + sql + ") page_ ) where rownum_ <= ${offset + max} and rownum_ > ${offset}"
def countwrap = "select count(0) total from (" + sql + ")"
def wahaha = []
dao.db.rows(sqlwrap)?.each {
    def registerStatus = 0
    if(it.id){
        registerStatus = 1
    }
    wahaha << [mobile:it.mobile, code:it.code, date:it.date_created?.timestampValue()?.format('yyyy-MM-dd HH:mm:ss'), ksn:it.ksn, status:it.status, registerStatus:registerStatus]
}
def total = dao.db.firstRow(countwrap)?.total as Long
total = Math.ceil(total / max) as Long
def result = [
        total: total,
        data:wahaha
]
render Commons.success(result)