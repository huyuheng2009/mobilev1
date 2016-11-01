package cron

import util.Commons

/**
 * @author yinheli
 */

log.info '-' * 10 + " force logout start " + '-' * 10
def dao = Commons.getDAO()
def count = dao.db.executeUpdate("delete from ws_session where 1=1")
log.info '-' * 10 + " force logout end, logout terminal count $count " + '-' * 10