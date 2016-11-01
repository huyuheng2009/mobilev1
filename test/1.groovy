import com.alibaba.fastjson.JSONArray
import org.apache.commons.codec.digest.DigestUtils
import org.jpos.iso.ISOUtil
import sun.misc.BASE64Decoder
import sun.misc.BASE64Encoder
import util.Commons

/**
 * Created by lei on 14-4-8.
 */
println(new String(new BASE64Encoder().encodeBuffer('yogapay2014'.bytes)))
println(new String(new BASE64Decoder().decodeBuffer('MTIzcXdl')))
//File file = new File('D:\\workspace\\posboss\\src\\main\\webapp\\images\\logo_login.png')
//println(file.exists())
//Commons.sendEamil(subject: "测试发送邮件",
//        to: ['hanlei830@126.com',],
//        cc:['279689178@qq.com'],
//        content: '您尾号1234的银行卡于2014年05月08日 14时56分00秒在XXX消费人民币1231231231.00元，附件中是签购单',
//        attachment: [
//                [name: "交易签购单", file: file]
//        ]
//)