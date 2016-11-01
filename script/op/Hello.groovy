package op

import util.Commons

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

/**
 * echo
 *
 * @author yinheli
 */

//HttpServletRequest req
//HttpServletResponse resp
//HttpSession session

def params = [:]
req.parameterMap.each { k, v ->
	if (v && v.size() > 1) {
		params[k] = v
	} else {
		params[k] = v[0]
	}
}

if (session?.getAttribute('sid') || wssession) {
	render(Commons.success([reqNo: req.getParameter('reqNo')?:null, params:params?:null])); return
}

render(Commons.fail(null, 'WITHOUT_SESSION'))
