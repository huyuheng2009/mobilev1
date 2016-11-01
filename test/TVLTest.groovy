/**
 * 项目: mobile_v1
 * 包名：PACKAGE_NAME
 * 文件名: TVLTest
 * 创建时间: 2014/9/11 15:50
 * 支付界科技有限公司版权所有，保留所有权利
 */


import org.jpos.core.CardHolder
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOUtil;
import org.jpos.tlv.TLVList;
import util.Commons;

String data = "4F08A0000003330101015F24032303319F160F4243544553542031323334353637389F21031417439A031409119F02060000000000009F03060000000000009F34034203009F120AD5D0D0D0D2BBBFA8CDA8C408621483FFFFFF3879C10A01214040800051E000D2C70820D58FD9C94AD8ECC00A01314040800051E00010C28201483E7EA455331A00BCB6AE01660C67F86AD9F5768C647CD19DD3C16B3F0C0239252105911396AA8A26D6CBD1A225F839C81BB384288D6D4AC0763B27482C15EB709E552243A9C512D812E5DA20379F5BF5BDABC2E86B1F1B527C537CCA45A555086F8534CC306AD848590551B972C51B929BC3FD2BB8D4F2ECE52C3AC7FFD42BE4F7BE3E93C03A836ACB3F3F28C4954963A1DC326F33BC55277FE058B65B123A38CEA95F7389EFB389C0BA97E7D7178467C6B5C5E839ECDED99159A5FAF5922A821DCCC62072F69840746B6646ECDFA8910D0C9599C6E82D1FF5C0C339A3A238CFE20C03A07ADBF24ABEF9E30F31AA578A7CE2702A39C319657DC12EE3AD99E0514824066C3862E98392FE821E6193502C74B23F2D0060DB0793A6DEA57A157E2B2B5478DC62FB942A43D420F4B838104F29552A0AE198826CE9600C18C9564FF48727014F66AFAC74";
//String data = "5F201A20202020202020202020202020202020202020202020202020204F08A0000003330101015F24032305289F160F4243544553542031323334353637389F21031543229A031306279F02060000000009559F03060000000000009F34030203009F120A50424F43204445424954C40A622848FFFFFFFFF8874FC00A00000332100300E00010C28201686F7BBEA54288BEA3473E40AE651C10156F9ADCC230449AE1CD1BBFBD6E0AC626C9859A45AAA4BD818A239FC9226B069E90D3818AEFCCFC091622B8E21F5B57B4125D1D80DBEA3F0F7597D7AE7464F52831F7E8C3507F588E6BD2BC7E179C6CCB5E2BC6324E1C933315F8744B1D369D8A36EAD51366BF77CC1434193A0840AB03BC784FBABDFD5F868E8363FE8CD21E69EA60C70A26436AB3C80594A26EE6B5DDC41CF6C3C4CF2D3BFBEFEB1AB88D81446B3A58048A269FDDB62F211FDE4FA5F532C97D0E7AAEF263EF81537F75014589DEAF38BC1ABDB4FC3375A6F84A069A97D34036FCB4A57F2CBC97EFA27524E8AD8E8363FE8CD21E698A55C1F02251791FC6ED94B280E7CA30F176B92F4D2F5A5BBB7F28B41234B7DC44C24A144DF9F742DAC9ECB7EC1CFFA3582D85FF9EE1B4A5857BD609B171240A582CE11A6172E8A2582CE11A6172E8A28836AF5373DC49B7418ADFDEAC5622F5D0AC3F3E6850A08039B8A2ABA329E2F9";
TLVList tlvList = new TLVList();
try {
    tlvList.unpack(ISOUtil.hex2byte(data));
    String tlv_C0 = tlvList.getString(0xC0);
    String tlv_C1 = tlvList.getString(0xC1);
    String tlv_C2 = tlvList.getString(0xC2);
    String tlv_C7 = tlvList.getString(0xC7);
    println(tlv_C2)
    def bdk = '0123456789ABCDEFFEDCBA9876543210'
    def model = 'mpos'
    def tracks2 = Commons.decodeTracks2(tlv_C0, bdk, tlv_C2, model)
    tlvList = new TLVList();
    def hexLen = tracks2.substring(4,8)
    int length = Commons.char2Hex(hexLen.charAt(0))*Math.pow(16,3)+Commons.char2Hex(hexLen.charAt(1))*Math.pow(16,2)+Commons.char2Hex(hexLen.charAt(2))*Math.pow(16,1)+Commons.char2Hex(hexLen.charAt(3))*Math.pow(16,0)
    println("hexLen:"+hexLen)
    println("length:"+length)
    tracks2=tracks2.substring(8,tracks2.length()).substring(0,length*2)
    println tracks2
    tlvList.unpack(ISOUtil.hex2byte(tracks2));
    def trailler=new TLVList();



    trailler.append(0x9F26,tlvList.getString(0x9F26))
    trailler.append(0x9F27,tlvList.getString(0x9F27))
    trailler.append(0x9F10,tlvList.getString(0x9F10))
    trailler.append(0x9F37,tlvList.getString(0x9F37))
    trailler.append(0x9F36,tlvList.getString(0x9F36))
    trailler.append(0x95,tlvList.getString(0x95))
    trailler.append(0x9A,tlvList.getString(0x9A))
    trailler.append(0x9C,tlvList.getString(0x9C))
    trailler.append(0x9F02,tlvList.getString(0x9F02))
    trailler.append(0x5F2A,tlvList.getString(0x5F2A))
    trailler.append(0x82,tlvList.getString(0x82))
    trailler.append(0x9F1A,tlvList.getString(0x9F1A))
    trailler.append(0x9F03,tlvList.getString(0x9F03))
    trailler.append(0x9F33,tlvList.getString(0x9F33))
    trailler.append(0x9F34,tlvList.getString(0x9F34))
    trailler.append(0x9F35,tlvList.getString(0x9F35))
    trailler.append(0x9F1E,tlvList.getString(0x9F1E))
    trailler.append(0x84,tlvList.getString(0x84))
    trailler.append(0x9F09,tlvList.getString(0x9F09))
    trailler.append(0x9F41,tlvList.getString(0x9F41))
    trailler = trailler.pack()
    def region23 = tlvList.getString(0x5F34).padLeft(3,'0')
    println(region23)
/*    iccData.append("9F26").append(tlvList.getString(0x9F26))
            .append("9F27").append(tlvList.getString(0x9F27))
            .append("9F10").append(tlvList.getString(0x9F10))
            .append("9F37").append(tlvList.getString(0x9F37))
            .append("9F36").append(tlvList.getString(0x9F36))
            .append("95").append(tlvList.getString(0x95))
            .append("9A").append(tlvList.getString(0x9A))
            .append("9C").append(tlvList.getString(0x9C))
            .append("9F02").append(tlvList.getString(0x9F02))
            .append("5f2A").append(tlvList.getString(0x5f2A))
            .append("82").append(tlvList.getString(0x82))
            .append("9F1A").append(tlvList.getString(0x9F1A))
            .append("9F03").append(tlvList.getString(0x9F03))
            .append("9F33").append(tlvList.getString(0x9F33))
            .append("9F34").append(tlvList.getString(0x9F34))
            .append("9F35").append(tlvList.getString(0x9F35))
            .append("9F1E").append(tlvList.getString(0x9F1E))
            .append("84").append(tlvList.getString(0x84))
            .append("9F09").append(tlvList.getString(0x9F09))
            .append("9F41").append(tlvList.getString(0x9F41))*/

    track2 = tlvList.getString(0x57).substring(0,tlvList.getString(0x57).indexOf("F")).replace("D","=")
    println('track2:'+track2)
    println('account:'+tlvList.getString(0x5A))
    println('expire:'+tlvList.getString(0x5F24).substring(2,tlvList.getString(0x5F24).length()))
    println('iccData:'+trailler)



    def cardHolder = new CardHolder(track2)
    println("exp:${cardHolder.EXP}")
    println("pan:${cardHolder.pan}")

} catch (ISOException e) {
    e.printStackTrace();
}