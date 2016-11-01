package op

import util.Commons
import util.Dao

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

/**
 * @author hanlei
 */

//HttpServletRequest req
//HttpServletResponse resp
//HttpSession session
//Dao dao

if (session) {
	dao.db.executeUpdate("delete from mobile_session where merchant_no=${session.merchant_no}")
}
render(Commons.success(null))