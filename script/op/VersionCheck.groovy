package op

import util.Commons

/**
 * 版本检测
 *
 * @author yinheli
 */
File version = new File('version/version.txt')
if(!version){
    render Commons.fail(null, 'NOT_NEW_VERSION','没有新版本更新'); return
}
render version