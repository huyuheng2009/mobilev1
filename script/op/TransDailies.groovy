package op

import util.Commons
import util.Dao

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.sql.Timestamp
import java.text.SimpleDateFormat

/**
 * @author yinheli
 */

//HttpServletRequest req
//HttpServletResponse resp
//Dao dao
def terminal = dao.findTerminalById(wssession.terminal_id)
log.info "terminal:${terminal}"
def dates = req.getRequestURI().replace('/transactions/', '').split('/')
def year = dates[0]
def month = "${dates[1]}".padLeft(2, '0')

log.info "year:${year}, month:${month}"

def sdf = new SimpleDateFormat('yyyyMMddHHmmss')
def start = sdf.parse("${year}${month}01000000")
def cal = Calendar.getInstance()
cal.setTime(start)
cal.add(Calendar.MONTH, 1)
cal.add(Calendar.DAY_OF_MONTH, -1)
cal.set(Calendar.HOUR_OF_DAY, 23)
cal.set(Calendar.MINUTE, 59)
cal.set(Calendar.SECOND, 59)
cal.set(Calendar.MILLISECOND, 99)
def end = cal.getTime()

start = new Timestamp(start.time)
end = new Timestamp(end.time)


def rows = dao.db.rows("""
	select count(id) n, sum(amount) amount, trans_date from WS_TRANS where
	terminal_no=${terminal?.terminal_no} and time_create>=${start} and time_create<=${end}
	and trans_status=1 and comp_status=2
	group by trans_date
""")

def dailies = []
rows.each {
	dailies << [transDate:it.trans_date, count:it.n, totalAmount:it.amount]
}

render Commons.success([dailies: dailies])