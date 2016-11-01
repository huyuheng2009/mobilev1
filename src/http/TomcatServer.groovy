package http

import com.alibaba.fastjson.JSONArray
import org.apache.catalina.connector.Connector
import org.apache.catalina.startup.Tomcat
import org.apache.coyote.http11.Http11NioProtocol
import org.jpos.ext.groovy.GroovySupport
import org.jpos.q2.QBeanSupport
import javax.servlet.ServletException
import javax.servlet.http.Cookie
import javax.servlet.http.HttpServlet
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.logging.Level

class TomcatServer extends QBeanSupport {

	private Tomcat tomcat = null
	private String script

	@Override
	protected void initService() throws Exception {
		java.util.logging.Logger.getLogger("").setLevel(Level.WARNING)
		script = cfg.get('script', 'Handler.groovy')
		System.setProperty 'catalina.home', 'tomcat'
		tomcat = new Tomcat()
		tomcat.silent = true
		tomcat.engine.name = 'hft'
		def connector = new Connector(Http11NioProtocol.name)
		connector.port = cfg.getInt('port', 8093)
		connector.setAttribute 'backlog'              , 500
		connector.setAttribute 'minSpareThreads'      , 4
		connector.setAttribute 'maxThreads'           , 100
		connector.setAttribute 'pollerThreadCount'    , 4
		connector.setAttribute 'maxKeepAliveRequests' , 100
		connector.setAttribute 'socketBuffer'         , 10240
		connector.setAttribute 'maxSavePostSize'      , 1024 * 1024
		connector.setAttribute 'connectionTimeout'    , 2 * 60 * 1000
		connector.setAttribute 'socket.reuseAddress'  , true
		connector.URIEncoding = 'UTF-8'
		connector.xpoweredBy  = false
		tomcat.setConnector         connector
		tomcat.service.addConnector connector
		def ctx = tomcat.addContext('/',  new File('static').absolutePath)
		ctx.reloadable        = false
		ctx.cookies           = false
		ctx.sessionTimeout    = 30
		ctx.sessionCookieName = 'HFTNO'
		tomcat.addServlet(ctx, 'handler', new HttpServlet() {
			@Override
			protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
				req.setCharacterEncoding('UTF-8')
				resp.setCharacterEncoding('UTF-8')
                resp.setHeader 'Expires', '-1'
                resp.setHeader 'Pragma', 'no-cache'
                resp.setHeader 'Cache-Control', 'no-cache, no-store, must-revalidate'
				resp.setHeader 'Server', 'yogapay hft trans front/1.0.0'
				if (req.requestURI == '/favicon.ico') {
					resp.contentType = 'image/x-icon'
					resp.outputStream << new File('static/favicon.ico').newInputStream()
					resp.outputStream.flush()
					return
				}

				def render = { content ->
					if (content instanceof Map || content instanceof Collection) {
						resp.setContentType('application/json;charset=UTF-8')
                        log.info('resp='+JSONArray.toJSONString(content, false))
						resp.writer.write JSONArray.toJSONString(content, false)
					} else if (content instanceof String) {
						resp.setContentType('text/plain;charset=UTF-8')
						resp.writer.write content
					} else if (content instanceof File) {
                        resp.setContentLength(content.length() as int)
                        if(content.getName().toLowerCase().lastIndexOf(".apk") == (content.getName().length() -4)){
                            resp.setContentType('application/vnd.android.package-archive')
                        }
                        resp.outputStream << content.newInputStream()
                    }
				}

				try {
					def binding = new Binding()
					binding.setVariable("req", req)
					binding.setVariable("resp", resp)
					binding.setVariable("log", log)
					binding.setVariable("render", render)
					binding.setVariable("q2", getServer())
					binding.setVariable("session", null)
					binding.setVariable("binding", binding)
					if (script.endsWith("groovy")) {
						GroovySupport.gse.run(script, binding)
					} else {
						Class<Script> c = (Class<Script>) Class.forName(script)
						Script sc = c.getConstructor(Binding.class).newInstance(binding)
						sc.run()
					}
				} catch (e) {
					resp.status = 500
//                    resp.outputStream << new File('static/50x.html').newInputStream()
					log.error e
				}
			}
		})
		ctx.addServletMapping('/*', 'handler')

		if (Boolean.parseBoolean(System.getProperty('dev', 'false'))) {
			tomcat.addServlet(ctx, 'druid', 'com.alibaba.druid.support.http.StatViewServlet')
			ctx.addServletMapping('/druid/*', 'druid')
		}
	}

	@Override
	protected void startService() throws Exception {
		tomcat?.start()
	}

	@Override
	protected void stopService() throws Exception {
		tomcat?.stop()
	}

	@Override
	protected void destroyService() throws Exception {
		tomcat?.destroy()
	}
}
