import op.*
import org.jpos.iso.ISOUtil
import util.Commons
import util.Constants
import util.Mapping

import java.lang.management.ManagementFactory
import java.sql.Timestamp
import java.text.SimpleDateFormat

//HttpServletRequest req
//HttpServletResponse resp
//HttpSession session

def start = System.currentTimeMillis()
def ip = req.getHeader('X-Real-IP')?:req.getRemoteAddr()
def uri = req.requestURI
// server status
if (uri in ['/_hft_status']) {
	def rumtimeMx = ManagementFactory.getRuntimeMXBean()
	def runtime = Runtime.getRuntime()
	def threadInfo = ''
	def threadIds =  ManagementFactory.threadMXBean.allThreadIds
	Arrays.sort(threadIds)
	ManagementFactory.threadMXBean.getThreadInfo(threadIds).each {threadInfo += it.toString().trim() + '\n\t'}
	render """\
server :  hft - \$idx ${rumtimeMx.getName()}
system :  cpus: ${ManagementFactory.operatingSystemMXBean.availableProcessors} load: ${ManagementFactory.operatingSystemMXBean.systemLoadAverage}
vm     :  ${rumtimeMx.vmName} ${System.properties.getProperty('java.runtime.version')}(build:${rumtimeMx.vmVersion}) ${rumtimeMx.getVmVendor()}
version:  2.0.1
uptime :  ${ISOUtil.millisToString(q2.getUptime())}
Memory :
	inUseMemory : ${runtime.totalMemory() - runtime.freeMemory()}
	freeMemory  : ${runtime.freeMemory()}
	totalMemory : ${runtime.totalMemory()}

	HeapMemoryUsage   :${ManagementFactory.memoryMXBean.heapMemoryUsage.toString()}
	NonHeapMemoryUsage:${ManagementFactory.memoryMXBean.nonHeapMemoryUsage.toString()}

Threads(${ManagementFactory.threadMXBean.threadCount}):
	${threadInfo}
""".toString()
	return
}

if (req.getParameterMap().size() > 0) {
	log.info "params: ${req.getParameterMap()}"
}
def script = Mapping.conf.find {uri.matches(it.key)}?.value
def scriptName = script?.name?:'None'
log.info '-' * 10 + " op: [${scriptName}] " + '-' * 10
if (!script) {
	resp.setStatus 404
	resp.getOutputStream() << new File('static/404.html').newInputStream()
	return
}
// # end server status

def reqTime = req.getParameter('reqTime')
if (reqTime && Commons.getConfig().commons.reqTimeCheck) {
	try {
		def time = new SimpleDateFormat('yyyyMMddHHmmss').parse(reqTime).getTime()
		if (Math.abs(System.currentTimeMillis() - time) > 10 * 60 * 1000L) {
			render(Commons.fail(null, 'ILLEGAL_ARGUMENT', '请求时间有误')); return
		}
	} catch (ignore) {
		render(Commons.fail(null, 'ILLEGAL_ARGUMENT'));return
	}
}


def dao = Commons.getDAO()

def sid = session?.getAttribute('sid')?:null
def session = null

if (sid) session = dao.findSessionById(sid)

def hftno = req.getHeader('HFTNO')
if (hftno) session = dao.findSessionBySessionNo(hftno)

println("hftno=================${hftno}")
println("session================${session}")
if (session) {
	if (session.expiry_time.time - start <= 0) {
		log.info 'session exp'
        session = null
	}
}
// check
if (!session && !(script in [Hello, Login, Logout, BankQuery, Register, ResetPassword,
		StaticPage, SwiperRegister, VersionCheck, GetIdCode, Validate, Upgrade, SwiperCheck, CardQuery,Banners])) {
	render(Commons.fail(null, 'SESSION_TIMEOUT'));return
}

try {
	def op = script.newInstance(binding)
	op.ip = ip
	op.session = session
	op.dao = dao
	op.run()
} catch (e) {
	render Commons.fail(null, 'SYSTEM_ERROR', Constants.error_code_mapping.SYSTEM_ERROR)
	log.error e
} finally {
	log.info '-' * 10 + " op: [${scriptName}] time: ${System.currentTimeMillis() - start}ms" + '-' * 10
}

if (session) {
    session.expiry_time = new Timestamp(new Date().time + 30 * 60 * 1000)
	dao.update(session)
}
