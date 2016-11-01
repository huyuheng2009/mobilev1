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
 * Date: 13-8-29
 * Time: 下午5:31
 * To change this template use File | Settings | File Templates.
 */
class AishuaUtil {
    static log = new Log(NameRegistrar.getIfExists('logger.Q2') as Logger, Commons.getSimpleName())
    static jceHandler = new MyJCEHandler('com.sun.crypto.provider.SunJCE')
    static final KEYSTORE_KEY = 'keyStore'
    static final SMADAPTER_KEY = 'hsm'

    static mac(params){

        def ks = NameRegistrar.get(KEYSTORE_KEY) as SecureKeyStore
        def sm = NameRegistrar.get(SMADAPTER_KEY) as SoftSecurityModule

        def tak = ks.getKey("ws.${params.ksn}.tak")
        def clear_tmk = jceHandler.formDESKey(SMAdapter.LENGTH_DES3_2KEY, new byte[16])
        def lmk_tmk = sm.encryptToLMK(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_TMK, clear_tmk)
        def clear_tdk = jceHandler.formDESKey(SMAdapter.LENGTH_DES3_2KEY, jceHandler.decryptData(sm.exportKey(tak, lmk_tmk), clear_tmk))

        def encTracks = params.encTracks[0..-17]
        def random = params.encTracks[-16..-1]
        def ksnNo = params.ksn
        def reqNo = params.reqNo
        def data = ksnNo + reqNo
        def macStr = params.checksum[-8..-1]
        StringBuffer ext = new StringBuffer()
        for(int i=0;i<8;i++){
            ext.append('0').append(macStr[i])
        }
        println(ext.toString())
        byte[] vb = ISOUtil.hex2byte(encTracks+random+ksnNo+ISOUtil.hexString(data.bytes))
        println(ISOUtil.hexString(vb))
        byte[] vt = new byte[vb.length+8]
        System.arraycopy(vb, 0, vt, 0,vb.length)
        int size = vt.length / 8
        byte[] res = new byte[8]
        for(int i=0;i<size;i++){
            byte[] temp = new byte[8]
            System.arraycopy(vt, i*8, temp, 0, 8)
            res = ISOUtil.xor(res, temp)
        }
        def left = jceHandler.encryptData(ISOUtil.hex2byte(ext.toString()), clear_tdk)
        def xor = ISOUtil.xor(ISOUtil.hex2byte(ext.toString()), ISOUtil.hex2byte('FFFFFFFFFFFFFFFF'))
        def right = jceHandler.encryptData(xor, clear_tdk)
        def reslut = ISOUtil.concat(left, right)
        println(ISOUtil.hexString(reslut))
        def key = jceHandler.formDESKey(SMAdapter.LENGTH_DES3_2KEY, reslut)
        def leftMac = jceHandler.encryptData(res, key)
        def mac = new byte[4]
        System.arraycopy(leftMac,0,mac,0,4)
        ISOUtil.hexString(mac)+macStr
    }

    static decodeTracks(String encTracks, ksnNo){
        def ks = NameRegistrar.get(KEYSTORE_KEY) as SecureKeyStore
        def sm = NameRegistrar.get(SMADAPTER_KEY) as SoftSecurityModule
        def tdk = ks.getKey("ws.${ksnNo}.tak")
        def clear_tmk = jceHandler.formDESKey(SMAdapter.LENGTH_DES3_2KEY, new byte[16])
        def lmk_tmk = sm.encryptToLMK(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_ZMK, clear_tmk)
        def clear_tdk = jceHandler.formDESKey(SMAdapter.LENGTH_DES3_2KEY, jceHandler.decryptData(sm.exportKey(tdk, lmk_tmk), clear_tmk))

        def random = encTracks[-16..-1]
        def left = jceHandler.encryptData(ISOUtil.hex2byte(random), clear_tdk)
        def xor = ISOUtil.xor(ISOUtil.hex2byte(random), ISOUtil.hex2byte('FFFFFFFFFFFFFFFF'))
        def right = jceHandler.encryptData(xor, clear_tdk)
        def reslut = ISOUtil.concat(left, right)
        def key = jceHandler.formDESKey(SMAdapter.LENGTH_DES3_2KEY, reslut)
        println(ISOUtil.hexString(reslut))
        def tracks = jceHandler.decryptData(ISOUtil.hex2byte(encTracks), key)
        log.info("tracks:"+ISOUtil.hexString(tracks))
        def track2 = ISOUtil.hexString(tracks)[0..36].replaceAll('D','=')
        track2 = track2.replaceAll('F','')
        log.info "track2:$track2"
        def cardHolder = new CardHolder(track2)
        cardHolder
    }
}
