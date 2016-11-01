package op

import util.Commons

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * @author yinheli
 */

//HttpServletRequest req
//HttpServletResponse resp

def root = new File('static/pages')
def file = new File(root, req.requestURI)
if (file.exists()) {
	resp.getOutputStream() << file.newInputStream()
} else {
    resp.getOutputStream() << new File('static/404.html').newInputStream()
}