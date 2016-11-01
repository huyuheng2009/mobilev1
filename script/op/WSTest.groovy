package op

import com.alibaba.fastjson.JSONArray
import org.jpos.ext.security.SoftSecurityModule
import org.jpos.iso.ISOUtil
import org.jpos.security.SMAdapter
import org.jpos.security.SecureKeyStore
import org.jpos.util.NameRegistrar
import util.Commons

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

/**
 * 测试用
 *
 * @author yinheli
 */

def pass = Boolean.parseBoolean(System.getProperty('dev', 'false'))
//
if (!pass) {
	resp.setStatus 404
	resp.getOutputStream() << new File('static/404.html').newInputStream()
	return
}

//HttpServletRequest req
//HttpServletResponse resp
//HttpSession session

def r = [
        [type: 'abc', features:[
                printAble:true,
        ]]
]
render r
