package util

import org.jpos.core.CardHolder
import org.jpos.ext.security.MyJCEHandler
import org.jpos.ext.security.SoftSecurityModule
import org.jpos.iso.ISOUtil
import org.jpos.security.SMAdapter
import org.jpos.security.SecureKeyStore
import org.jpos.util.Log
import org.jpos.util.Logger
import org.jpos.util.NameRegistrar

/**
 * Created with IntelliJ IDEA.
 * User: hanlei
 * Date: 13-10-14
 * Time: 下午3:31
 * To change this template use File | Settings | File Templates.
 */
class ZfshuaUtil {
    static log = new Log(NameRegistrar.getIfExists('logger.Q2') as Logger, Commons.getSimpleName())
    static jceHandler = new MyJCEHandler('com.sun.crypto.provider.SunJCE')
    static final KEYSTORE_KEY = 'keyStore'
    static final SMADAPTER_KEY = 'hsm'
    static mac(params){

    }
    static decodeTracks(String encTracks, ksnNo){
        println("encTracks=${encTracks}")
        def ks = NameRegistrar.get(KEYSTORE_KEY) as SecureKeyStore
        def sm = NameRegistrar.get(SMADAPTER_KEY) as SoftSecurityModule
        def tdk = ks.getKey("ws.${ksnNo}.tak")
        def clear_zmk = jceHandler.formDESKey(SMAdapter.LENGTH_DES3_2KEY, new byte[16])
        def zmk = sm.encryptToLMK(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_ZMK, clear_zmk)
        def tdk_zmk = sm.exportKey(tdk, zmk)
        def clear_tdk = jceHandler.formDESKey(SMAdapter.LENGTH_DES3_2KEY, jceHandler.decryptData(tdk_zmk, clear_zmk))
        def random = encTracks[-16..-1]
        println("random=${random}")
        def left = ISOUtil.hex2byte(random)
        def right = ISOUtil.xor(left,ISOUtil.hex2byte('FFFFFFFFFFFFFFFF'))
        def result = ISOUtil.concat(left,right)
        def temp = jceHandler.encryptData(result,clear_tdk)
        println("temp=${ISOUtil.hexString(temp)}")
        def tempKey = jceHandler.formDESKey(SMAdapter.LENGTH_DES3_2KEY, temp)
        def tracks = ISOUtil.hexString(jceHandler.decryptData(ISOUtil.hex2byte(encTracks[0..-17]),tempKey))
        log.info "tracks:$tracks"
        def track2 = tracks[0..36].replaceAll('D','=')
        log.info "track2:$track2"
        def cardHolder = new CardHolder(track2)
        cardHolder
    }
}
