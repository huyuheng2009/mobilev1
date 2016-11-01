package util

import com.alibaba.druid.pool.DruidDataSource
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import net.sourceforge.pinyin4j.PinyinHelper
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.NameValuePair
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.params.HttpMethodParams
import org.apache.commons.mail.EmailAttachment
import org.apache.commons.mail.HtmlEmail
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload
import org.jpos.core.CardHolder
import org.jpos.ext.security.MyJCEHandler
import org.jpos.ext.security.SoftSecurityModule
import org.jpos.iso.BaseChannel
import org.jpos.iso.ISOMsg
import org.jpos.iso.ISOUtil
import org.jpos.iso.MUX
import org.jpos.security.EncryptedPIN
import org.jpos.security.SMAdapter
import org.jpos.security.SecureDESKey
import org.jpos.security.SecureKeyStore
import org.jpos.util.Log
import org.jpos.util.Logger
import org.jpos.util.NameRegistrar
import sun.misc.BASE64Decoder
import sun.misc.BASE64Encoder

import javax.servlet.http.Cookie
import javax.servlet.http.HttpServletRequest
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * @author yinheli
 */
class Commons {
	static log = new Log(NameRegistrar.getIfExists('logger.Q2') as Logger, Commons.getSimpleName())
	static jceHandler = new MyJCEHandler('com.sun.crypto.provider.SunJCE')
    private static emailExecutor = new ThreadPoolExecutor(2, 10, 5L, TimeUnit.MINUTES, new LinkedBlockingQueue<Runnable>(100))

	static final CONFIG_KEY = 'config:default'
	static final DATASOURCE_KEY = 'datasource:default'
	static final ACQ_MUX_KEY = 'mux.ts_mux'
	static final KEYSTORE_KEY = 'keyStore'
	static final SMADAPTER_KEY = 'hsm'

	static final FRONT_ZMK_KEY = 'internal.zmk'
	static final FRONT_ZPK_KEY = 'internal.zpk'
	static final FRONT_ZAK_KEY = 'internal.zak'

	static final Map BDKs = [
			'300'   : '0000000000000000000BE10106154000',
			'399'   : '0000000000000000000BE10106154000'
	]

	static {
		String.metaClass.encodeAsSHA1 << {
			ISOUtil.hexString(DigestUtils.sha(new String(value))).toLowerCase()
		}
	}

	static getConfig() {
		def conf = NameRegistrar.getIfExists(CONFIG_KEY)
		if (!conf) {
			conf = new ConfigSlurper().parse(new File('conf/Config.groovy').toURI().toURL())
			NameRegistrar.register(CONFIG_KEY, conf)
		}
		conf
	}

	static Map getCache() {
		def key = 'WS:CACHE'
		def cache = NameRegistrar.getIfExists(key)
		if (!cache) {
			cache = Collections.synchronizedMap([:])
			NameRegistrar.register(key, cache)
		}
		cache
	}

	static getFrontZMK() {
		def zmk = NameRegistrar.getIfExists(FRONT_ZMK_KEY) as SecureDESKey
		if (!zmk) {
			def ks = NameRegistrar.get(KEYSTORE_KEY) as SecureKeyStore
			zmk = ks.getKey(FRONT_ZMK_KEY)
			NameRegistrar.register(FRONT_ZMK_KEY, zmk)
		}
		zmk
	}

	static getFrontZPK() {
		def zpk = NameRegistrar.getIfExists(FRONT_ZPK_KEY) as SecureDESKey
		if (!zpk) {
			def ks = NameRegistrar.get(KEYSTORE_KEY) as SecureKeyStore
			zpk = ks.getKey(FRONT_ZPK_KEY)
			NameRegistrar.register(FRONT_ZPK_KEY, zpk)
		}
		zpk
	}

	static getFrontZAK() {
		def zak = NameRegistrar.getIfExists(FRONT_ZAK_KEY) as SecureDESKey
		if (!zak) {
			def ks = NameRegistrar.get(KEYSTORE_KEY) as SecureKeyStore
			zak = ks.getKey(FRONT_ZAK_KEY)
			NameRegistrar.register(FRONT_ZAK_KEY, zak)
		}
		zak
	}

	static getDAO() {
		def ds = NameRegistrar.getIfExists(DATASOURCE_KEY)
		if (!ds) {
			def conf = getConfig()
			ds = new DruidDataSource()
			ds.setDriverClassName(conf.datasource.jdbc.driver)
			ds.setUrl(conf.datasource.jdbc.url)
			ds.setUsername(conf.datasource.jdbc.user)
			ds.setPassword(conf.datasource.jdbc.password)
			ds.setMinIdle(conf.datasource.pool.MinIdle)
			ds.setMaxActive(conf.datasource.pool.MaxActive)
			ds.setMaxWait(conf.datasource.pool.MaxWait)
			ds.setTestWhileIdle(conf.datasource.pool.TestWhileIdle)
			ds.setTimeBetweenEvictionRunsMillis(conf.datasource.pool.TimeBetweenEvictionRunsMillis)
			ds.setMinEvictableIdleTimeMillis(conf.datasource.pool.MinEvictableIdleTimeMillis)
			ds.setValidationQuery(conf.datasource.pool.ValidationQuery)
			NameRegistrar.register(DATASOURCE_KEY, ds)
		}
		new Dao(ds)
	}

	static versionParse(String ver) {
		def v = ver.split(/\./)
		if (v.length < 5) return null
		[
	        os:v[0].toLowerCase(),
            model :v[1].toLowerCase(),
			mainVersion:Long.parseLong(v[2]),
			subVersion:Long.parseLong(v[3]),
			subsubVersion:Long.parseLong(v[4]),
		]
	}

    static checkVersion(def ver, def product){
        def version = ver.mainVersion+"."+ver.subVersion
        def versionDouble = Double.parseDouble(version)
        if(product == Constants.ZFT){
            if(ver.os == "android"){
                if(versionDouble < 1.3){
                    return  false
                }else if(versionDouble == 1.3){
                    if(ver.subsubVersion < 277){
                        return false
                    }
                }
            }else if(ver.os == "ios"){
                if(ver.subVersion == 31){
                    return false
                }
                if(versionDouble < 1.3){
                    return false
                }else if(versionDouble == 1.3){
                    if(ver.subsubVersion < 2){
                        return false
                    }
                }
            }
        }else if(product == Constants.SZKM){
            if(ver.os == "android"){
                if(versionDouble < 1.3){
                    return  false
                }else if(versionDouble == 1.3){
                    if(ver.subsubVersion < 190){
                        return false
                    }
                }
            }
        }

        return  true
    }

	static success(map, msg = '') {
		if (!msg) msg = Constants.error_code_mapping.SUCCESS
		ret map?:[:], true, 'SUCCESS', msg
	}

    static success(map, msg = '',Cookie cookie) {
        if (!msg) msg = Constants.error_code_mapping.SUCCESS
        ret map?:[:], true, 'SUCCESS', msg, cookie
    }

	static fail(map, code, msg = '') {
		ret map?:[:], false, code, msg
	}

	static ret(map, isSuccess, code, msg) {
		if (!msg) {
			msg = Constants.error_code_mapping.get(code, "交易失败, 错误码: $code")
		}
		def resp = [
				respTime  : new Date().format('yyyyMMddHHmmss'),
				isSuccess : isSuccess,
				respCode  : code,
				respMsg   : msg?.toString()?:null
		] << map

		log.info "response: $resp"
		resp
	}

    static ret(map, isSuccess, code, msg, Cookie cookie) {
        if (!msg) {
            msg = Constants.error_code_mapping.get(code, "交易失败, 错误码: $code")
        }
        def resp = [
                respTime  : new Date().format('yyyyMMddHHmmss'),
                isSuccess : isSuccess,
                respCode  : code,
                respMsg   : msg?.toString()?:null,
                cookie    : cookie,
        ] << map

        log.info "response: $resp"
        resp
    }

	static getKeyPrefix(ksnNo) {
		"ws.${ksnNo}"
	}

	static genTMK(String ksnNo) {
        log.info("*** ksnNo=${ksnNo} tmk reset ***")
		def base = 'hft@yogapay.com1'
        def oriKsnNo = ksnNo
		if (ksnNo.length() > 14){
            ksnNo = ksnNo[0..13]
        }else{
            ksnNo = ksnNo.padRight(14,'0')
        }
		def clear_kek_bytes = ISOUtil.xor(base.bytes, ISOUtil.hex2byte("${ksnNo + 'e0' + ksnNo + 'e0'}".toString()))
		org.jpos.security.Util.adjustDESParity(clear_kek_bytes)
		def clear_kek = jceHandler.formDESKey(SMAdapter.LENGTH_DES3_2KEY, clear_kek_bytes)

		def ks = NameRegistrar.get(KEYSTORE_KEY) as SecureKeyStore
		def sm = NameRegistrar.get(SMADAPTER_KEY) as SoftSecurityModule

		def kek = sm.encryptToLMK(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_ZMK, clear_kek)

		def clear_tmk = jceHandler.generateDESKey(SMAdapter.LENGTH_DES3_2KEY)
		def tmk = sm.encryptToLMK(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_TMK, clear_tmk)
		ks.setKey("${getKeyPrefix(oriKsnNo)}.tmk", tmk)
		[ISOUtil.hexString(sm.exportKey(tmk, kek)), ISOUtil.hexString(tmk.keyCheckValue)]
	}

	static updateTPK(ksnNo, tid) {
		def ks = NameRegistrar.get(KEYSTORE_KEY) as SecureKeyStore
		def sm = NameRegistrar.get(SMADAPTER_KEY) as SoftSecurityModule

		def prefix = getKeyPrefix(ksnNo)
		def tmk = ks.getKey("${prefix}.tmk")

		def clear_tpk = jceHandler.generateDESKey(SMAdapter.LENGTH_DES3_2KEY)
		def tpk = sm.encryptToLMK(SMAdapter.LENGTH_DES3_2KEY, SMAdapter.TYPE_TPK, clear_tpk)
		ks.setKey("tid.${tid}.tpk", tpk)
		[ISOUtil.hexString(sm.exportKey(tpk, tmk)), ISOUtil.hexString(tpk.keyCheckValue)]
	}

	static parseRequest(HttpServletRequest req) {
		def params = [:]
		if (req.contentType.toLowerCase().indexOf('multipart/form-data') != -1) {
			def factory = new DiskFileItemFactory()
			def tempDir = new File('tomcat/tmp_upload')
			if (!tempDir.exists()) {
				log.warn 'upload temp_dir not found create it'
				tempDir.mkdirs()
				log.info "tempDir created ${tempDir.absolutePath}"
			}
			factory.setRepository(tempDir)
			def upload = new ServletFileUpload(factory)
			upload.parseParameterMap(req).each { k, v ->
				def value = null
				if (v.size() > 1) {
					value = []
					v.each {
						if (it.isFormField()) {
							value << it.getString("UTF-8")
						} else {
							value << it
						}
					}
				} else {
					def item = v.get(0)
					if (item.isFormField()) {
						value = item.getString("UTF-8")
					} else {
						value = item
					}
				}
				params[k] = value
			}
		} else {
			req.getParameterMap().each {k, v ->
				if (v.size() > 1) {
					params[k] = v
				} else {
					params[k] = v[0]
				}
			}
		}

		params
	}

	static decodeTracks(trackKsn, bdk, encTracks, model) {
		def cardHolder = null
		if (model == Constants.MPOS_MODEL || model == Constants.SHUA_MODEL) {
            println("trackKsn=${trackKsn}   bdk=${bdk}")
            byte[] trackksnByte = ISOUtil.hex2byte(trackKsn);
            byte[] bdkByte = ISOUtil.hex2byte(bdk);
            //第一步根据ksn和bdk获取到IPEK
            byte[] ipekByte = generateIPEK(trackksnByte, bdkByte);

            //第二步根据ksn和IPEK获取DUKPT Key
            byte[] dukptByte = getDUKPTKey(trackksnByte, ipekByte);

            //第三步根据DUKPT KEY得到Data Key Variant
            byte[] dataKeyVariantByte = getDataKeyVariant(dukptByte);
            int count = encTracks.length() / 16;
            String tracks2 = "";
            for(int i=0;i<count;i++){
                String temp = encTracks.substring(i*16, (i+1)*16);
//		    System.out.println("第"+i+"组密文磁道="+temp);
//		    System.out.println("第"+i+"组解密之后的磁道="+ISOUtil.hexString(JCEHandler.decryptData(ISOUtil.hex2byte(temp), dataKeyVariantByte)));
                tracks2 += ISOUtil.hexString(JCEHandler.decryptData(ISOUtil.hex2byte(temp), dataKeyVariantByte));
            }
            System.out.println("tracks2="+tracks2);
            String track2Data = tracks2.substring(0, tracks2.indexOf("F"))
                    .replace("D", "=");
			cardHolder = new CardHolder(track2Data)
		}

		cardHolder
	}

	static decodeTracks2(trackKsn, bdk, encTracks, model) {
		def tracks2 = ''
		if (model == Constants.MPOS_MODEL || model == Constants.SHUA_MODEL) {
            println("trackKsn=${trackKsn}   bdk=${bdk}")
            byte[] trackksnByte = ISOUtil.hex2byte(trackKsn);
            byte[] bdkByte = ISOUtil.hex2byte(bdk);
            //第一步根据ksn和bdk获取到IPEK
            byte[] ipekByte = generateIPEK(trackksnByte, bdkByte);

            //第二步根据ksn和IPEK获取DUKPT Key
            byte[] dukptByte = getDUKPTKey(trackksnByte, ipekByte);

            //第三步根据DUKPT KEY得到Data Key Variant
            byte[] dataKeyVariantByte = getDataKeyVariant(dukptByte);
            int count = encTracks.length() / 16;
            for(int i=0;i<count;i++){
                String temp = encTracks.substring(i*16, (i+1)*16);
//		    System.out.println("第"+i+"组密文磁道="+temp);
//		    System.out.println("第"+i+"组解密之后的磁道="+ISOUtil.hexString(JCEHandler.decryptData(ISOUtil.hex2byte(temp), dataKeyVariantByte)));
                tracks2 += ISOUtil.hexString(JCEHandler.decryptData(ISOUtil.hex2byte(temp), dataKeyVariantByte));
            }
            System.out.println("tracks2="+tracks2);
		}
        tracks2
	}

    //QPOS解密密码块
    public static String decodePinBlock(String pinKsn, String bdk,  String cardNo, String encPinBlock, String clearTpk,String model){
        String pinBlock = null
        String clearPin = '000000'
        if (model == Constants.MPOS_MODEL) {
            println("pinKsn=${pinKsn},encPinBlock=${encPinBlock}")
            if(pinKsn && encPinBlock){
                byte[] pinKsnByte = ISOUtil.hex2byte(pinKsn);
                byte[] bdkByte = ISOUtil.hex2byte(bdk);
                //第一步根据ksn和bdk获取到IPEK
                byte[] ipekByte = generateIPEK(pinKsnByte, bdkByte);

                //第二步根据ksn和IPEK获取DUKPT Key
                byte[] dukptByte = getDUKPTKey(pinKsnByte, ipekByte);

                //第三步根据DUKPT KEY得到Data Key Variant
                byte[] pinKeyVariant = getPinKeyVariant(dukptByte);

                def clearPk = ISOUtil.hexString(pinKeyVariant);
                clearPin = unPinBlock(cardNo, encPinBlock,
                        clearPk);
            }
            if(clearPin.size() !=6){
                log.info('***密码长度不对***')
                throw Exception
            }
        }else{
            println("encPinBlock=${encPinBlock},clearTpk=${clearTpk}")
            clearPin = Des3.decode(encPinBlock, clearTpk)[0..5]
        }

        cardNo = cardNo.substring(cardNo.length() - 13,
                cardNo.length() - 1);
        // 将卡号与密码进行异或
        byte[] x = ISOUtil.xor(ISOUtil.hex2byte("06" + clearPin + "FFFFFFFF"),
                ISOUtil.hex2byte("0000" + cardNo));
        println("clearTpk====${clearTpk}")
        pinBlock = JCEHandler.encryptData(ISOUtil.hexString(x), clearTpk)
        return pinBlock;
    }

    // 根据明文pk进行密码解密
    public static String unPinBlock(String accountNo, String pinBlock,
                                    String clearPk) {
        String clearPin = "000000";
        if (pinBlock) {
            String dest = JCEHandler.decryptData(pinBlock, clearPk);
            accountNo = accountNo.substring(accountNo.length() - 13,
                    accountNo.length() - 1);

            byte[] x = ISOUtil.xor(ISOUtil.hex2byte(dest),
                    ISOUtil.hex2byte("0000" + accountNo));
            clearPin = ISOUtil.hexString(x);
            if (clearPin.length() == 16) {
                clearPin = clearPin.substring(2, clearPin.indexOf("F"));
            }

        }
        return clearPin;
    }

    public static byte[] generateIPEK(byte[] ksn, byte[] bdk) {
        def ipekByte = new byte[16]
        def temp = new byte[8]
        def temp2 = new byte[8]
        def keyTemp = new byte[16]
        System.arraycopy(bdk, 0, keyTemp, 0, 16)
        System.arraycopy(ksn, 0, temp, 0, 8)
        temp[7] &= 0xE0
        //3des算法
        def keyTempKey = jceHandler.formDESKey(SMAdapter.LENGTH_DES3_2KEY, keyTemp)
        temp2 = jceHandler.encryptData(temp, keyTempKey)
        System.arraycopy(temp2, 0, ipekByte, 0, 8)
        keyTemp[0] ^= 0xC0;
        keyTemp[1] ^= 0xC0;
        keyTemp[2] ^= 0xC0;
        keyTemp[3] ^= 0xC0;
        keyTemp[8] ^= 0xC0;
        keyTemp[9] ^= 0xC0;
        keyTemp[10] ^= 0xC0;
        keyTemp[11] ^= 0xC0;
        //3des算法
        def keyTempKey2 = jceHandler.formDESKey(SMAdapter.LENGTH_DES3_2KEY, keyTemp)
        temp2 = jceHandler.encryptData(temp, keyTempKey2)
        System.arraycopy(temp2, 0, ipekByte, 8, 8)
        println("IPEK="+ISOUtil.hexString(ipekByte))
        return ipekByte
    }

    public static byte[] getDUKPTKey(byte[] ksn, byte[] ipek) {
        def dukptKeyByte = new byte[16]
        def cnt = new byte[3]
        def temp3 = new byte[8]
        int shift
        System.arraycopy(ipek, 0, dukptKeyByte, 0, 16)
        cnt[0] = (ksn[7] & 0x1F);
        cnt[1] = ksn[8];
        cnt[2] = ksn[9];
        System.arraycopy(ksn, 2, temp3, 0, 6)
        temp3[5] &= 0xE0;
        shift = 0x10;
        while(shift > 0){
            if ((cnt[0] & shift) > 0){
                temp3[5] |= shift;
                NRKGP(dukptKeyByte, temp3)
            }
            shift >>= 1;
        }
        shift = 0x80;
        while(shift > 0){
            if ((cnt[1] & shift) > 0){
                temp3[6] |= shift;
                NRKGP(dukptKeyByte, temp3)
            }
            shift >>= 1;
        }
        shift = 0x80;
        while (shift > 0){
            if ((cnt[2] & shift) > 0)
            {
                temp3[7] |= shift;
                NRKGP(dukptKeyByte, temp3)
            }
            shift >>= 1;
        }
        println("dukptKeyByte="+ISOUtil.hexString(dukptKeyByte))
        return dukptKeyByte
    }

    static void NRKGP(byte[] key, byte[] ksn) {
        def temp = new byte[8]
        def key_l = new byte[8]
        def key_r = new byte[8]
        def key_temp = new byte[8]
        int i;
        System.arraycopy(key, 0, key_temp, 0, 8)
        for (i = 0; i < 8; i++){
            temp[i] = (byte)(ksn[i] ^ key[8 + i]);
        }
        //3des算法
        def keyTempKey = jceHandler.formDESKey(SMAdapter.LENGTH_DES, key_temp)
        key_r = jceHandler.encryptData(temp, keyTempKey)
        for (i = 0; i < 8; i++){
            key_r[i] ^= key[8 + i];
        }
        key_temp[0] = key_temp[0] ^ 0xC0;
        key_temp[1] = key_temp[1] ^ 0xC0;
        key_temp[2] = key_temp[2] ^ 0xC0;
        key_temp[3] = key_temp[3] ^ 0xC0;
        key[8] = key[8] ^ 0xC0;
        key[9] = key[9] ^ 0xC0;
        key[10] = key[10] ^ 0xC0;
        key[11] = key[11] ^ 0xC0;
        for (i = 0; i < 8; i++){
            temp[i] = (byte)(ksn[i] ^ key[8 + i]);
        }
        //3des算法
        keyTempKey = jceHandler.formDESKey(SMAdapter.LENGTH_DES, key_temp)
        key_l = jceHandler.encryptData(temp, keyTempKey)
        for (i = 0; i < 8; i++)
            key[i] = (byte)(key_l[i] ^ key[8 + i]);
        System.arraycopy(key_r, 0, key, 8, 8);
    }

    public static byte[] getDataKeyVariant(byte[] dukpt) {
        dukpt[5] = dukpt[5] ^ 0xFF
        dukpt[13] = dukpt[13] ^ 0xFF
        println("dataKeyVariant="+ISOUtil.hexString(dukpt))
        return dukpt
    }

    public static byte[] getPinKeyVariant(byte[] dukpt) {
        dukpt[7] = dukpt[7] ^ 0xFF
        dukpt[15] = dukpt[15] ^ 0xFF
        println("dataKeyVariant="+ISOUtil.hexString(dukpt))
        return dukpt
    }

	private static translatePIN(ISOMsg msg) {
		if (msg.getMTI() in ['0400', '0420']) return
		def ks = NameRegistrar.get(KEYSTORE_KEY) as SecureKeyStore
		def sm = NameRegistrar.get(SMADAPTER_KEY) as SMAdapter

		def pk_alias = "tid.${msg.getString(41)}.tpk"

		def front_zpk = getFrontZPK()
		def pk = ks.getKey(pk_alias) as SecureDESKey

		def cardholder = new CardHolder(msg)
		def epin = new EncryptedPIN(msg.getBytes(52), SMAdapter.FORMAT01, cardholder.PAN)
		def bpin = sm.translatePIN(epin, pk, front_zpk, SMAdapter.FORMAT01)
		msg.set 52, bpin.getPINBlock()
	}

	static String dumpISOMsg(ISOMsg m) {
		if (m == null) return 'NULL'

        def msg = m.clone() as ISOMsg
        def protect = [2, 35, 36, 45, 55]
        def wipe = [48, 52, 62]
        for(def it : wipe){
            if (msg.hasField(it)) {
                def v = null
                try {
                    v = msg.getValue(it)
                } catch (ignore) {
                    // ignore ISOException
                }
                if (v) {
                    if (v instanceof String) {
                        msg.set(it, '[WIPED]')
                    } else {
                        msg.set(it, '[WIPED]'.bytes)
                    }
                }
            }
        }
        for(def it : protect){
            if (msg.hasField(it)) {
                def v = null
                try {
                    v = msg.getValue(it)
                } catch (ignore) {
                    // ignore ISOException
                }
                if (v) {
                    if (v instanceof String) {
                        msg.set(it, ISOUtil.protect(v))
                    } else {
                        msg.set(it, '[WIPED]'.bytes)
                    }
                }
            }
        }
        def baos = new ByteArrayOutputStream()
        def ps = new PrintStream(baos, false)
        try {
            msg.dump(ps, '')
            ps.flush()
            baos.flush()
            return new String(baos.toByteArray())
        } finally {
            ps.close()
            baos.close()
        }
	}

	static def sendAndRecive(ISOMsg msg ,String clearTak) {
		def mux = NameRegistrar.get(ACQ_MUX_KEY) as MUX
		def channel = NameRegistrar.getIfExists('channel.ts_channel') as BaseChannel
		if (channel == null) {
			channel = NameRegistrar.get('channel.ts_channel0') as BaseChannel
		}
		msg.packager = channel.packager
		msg.setDirection(ISOMsg.OUTGOING)

        msg.set 64, JCEHandler.genECBMAC(msg.pack(), clearTak)

		def bytesDump = ISOUtil.hexdump(msg.pack())

		log.info 'send to posp:\n' + bytesDump
		ISOMsg resp = mux.request(msg, 60000L)

		if (resp && resp.hasField(38)) {
			def f38 = resp.getString(38).trim()
			if (!f38) resp.unset(38)
		}
		resp
	}

    static encodeAsSHA1(value){
        ISOUtil.hexString(DigestUtils.sha(value.toString())).toUpperCase()
    }

    static xor(String value){
        byte[] vb = ISOUtil.hex2byte(value)
        byte[] vt = new byte[vb.length + 8]
        System.arraycopy(vb, 0, vt, 0,vb.length)
        int size = vt.length / 8
        byte[] res = new byte[8]
        for(int i=0;i<size;i++){
            byte[] temp = new byte[8]
            System.arraycopy(vt, i*8, temp, 0, 8)
            res = ISOUtil.xor(res, temp)
        }
        res
    }

    static newSendMsg(String mobileNo, String content){
        try {
         String smsServiceUrl = getConfig().commons.sendMsgAddr
//        String smsServiceUrl = "http://127.0.0.1/message/index";
//            String smsServiceUrl = "http://183.62.232.130:8090/message/post"
            println("smsServiceUrl=${smsServiceUrl}")
            HttpClient client = new HttpClient();
            PostMethod method = new PostMethod();
            method.setRequestHeader("Content-type", "text/xml; charset=GB2312");
            method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                    new DefaultHttpMethodRetryHandler());
            method.getParams().setParameter(HttpMethodParams.SO_TIMEOUT,
                    new Integer(60000));
            String response = "";

            Map<String, Object> mp = new HashMap<String, Object>();
            URL url = new URL(smsServiceUrl);
            method.setPath(url.getPath());
            if (smsServiceUrl.indexOf("https") == 0) {// https
            } else {
                client.getHostConfiguration().setHost(url.getHost(),
                        url.getPort(), url.getProtocol());
            }
            def check = Commons.encodeAsSHA1(mobileNo+'yogapayHFT'+content+'PTSD')
            println(mobileNo+'yogapayHFT'+content+'PTSD')
            println("check="+check)
            NameValuePair[] param = [
                    new NameValuePair('target',mobileNo),
                    new NameValuePair('content',content),
                    new NameValuePair('operation','S'),
                    new NameValuePair('note.businessCode','PTSD'),
                    new NameValuePair('note.usage','验证码'),
                    new NameValuePair('check',check),
            ]
            method.setQueryString(param);

            int status  = client.executeMethod(method)
            println('status='+status)
            response = method.getResponseBodyAsString()
            println("response=${response}")
            def result = JSONArray.parse(response)
            if(result && result.rescode == '01'){
                return true
            }else {
                return false
            }
        }catch (e){
            e.printStackTrace()
            return false
        }
    }

    static saveInterfaceCount(interfaceName, wssession){
        def merchantNo = getDAO().findMerchantById(wssession.merchant_id).merchant_no
        getDAO().saveWsInterfaceCount(interfaceName.toString()?.split(/\./)[1], merchantNo)
    }

    static getModelByKsnNo(String ksnNo){

//         return  getDAO().findKsnbin(ksnNo)?.product_model
    }

    public static String getPinYinHeadChar(String str) {
        String convert = "";
        for (int j = 0; j < str.length(); j++) {
            char word = str.charAt(j);
            // 提取汉字的首字母
            String[] pinyinArray = PinyinHelper.toHanyuPinyinStringArray(word);
            if (pinyinArray != null) {
                convert += pinyinArray[0].charAt(0);
            } else {
                convert += word;
            }
        }
        return convert;
    }

    static sendEamil(params) {
        //subject, content, attachemt, to, cc = null, bcc = null
        emailExecutor.execute(new Runnable() {
            @Override
            void run() {
                def cfg = getConfig().email
                def email = new HtmlEmail()
                email.setHostName cfg.host
                email.setSmtpPort cfg.port
                email.setAuthentication cfg.username, new String(new BASE64Decoder().decodeBuffer(cfg.password))
//                email.setAuthentication cfg.username, "123qwe"
                email.setSSLOnConnect cfg.ssl
                email.setFrom cfg.from, 'salesslip'
                email.setCharset 'UTF-8'
                email.setSubject params.subject
                email.setHtmlMsg params.content

                if (params.to instanceof String) {
                    email.addTo(params.to)
                } else {
                    params.to.each {email.addTo(it)}
                }
                if (params.cc) {
                    if (params.cc instanceof String) {
                        email.addCc(params.cc)
                    } else {
                        params.cc.each {email.addCc(it)}
                    }
                }
                if (params.bcc) {
                    if (params.bcc instanceof String) {
                        email.addBcc(params.bcc)
                    } else {
                        params.bcc.each {email.addBcc(it)}
                    }
                }

                if (params.attachment) {
                    params.attachment?.each {
                        email.attach(new EmailAttachment(
                                name: "=?UTF-8?B?${new BASE64Encoder().encode(it.name?.toString().bytes)}?=",
                                path: it.file.absolutePath))
                    }
                }

                try {
                    def r = email.send()
                    log.info "email send result: ${r}"
                } catch (e) {
                    println(e)
                    log.error 'send fail', e
                }
            }
        })
    }

    static boolean checkAmount(amount){
        if((long)amount in [100000l,200000l,300000l,400000l,500000l,600000l,700000l,800000l,900000l,1000000l,1100000l,1200000l,1300000l,1400000l,1500000l,1600000l,1700000l,1800000l,1900000l,2000000l]){
            return false
        }else{
            return true
        }
    }

    static char2Hex( _char){
        _char = _char.toString()
        if(_char in ['0','1','2','3','4','5','6','7','8','9']){
            _char = Integer.valueOf(_char)
        }else if('a'==(_char=_char.toLowerCase())){
            _char = 10
        }else if('b'==_char){
            _char = 11
        }else if('c'==_char){
            _char = 12
        }else if('d'==_char){
            _char = 13
        }else if('e'==_char){
            _char = 14
        }else if('f'==_char){
            _char = 15
        }else{
            throw new IllegalAccessException('不合法16进制字符字符'+_char)
        }
    }
}
