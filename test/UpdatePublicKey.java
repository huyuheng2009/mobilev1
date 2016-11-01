/*
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbutils.QueryRunner;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.tlv.TLVList;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidDataSourceFactory;
import com.yogapay.core.server.PackagerNavigator;
import com.yogapay.core.server.jpos.HEXChannel;
import com.yogapay.core.server.jpos.Utils;

public class UpdatePublicKey {
	
	static String batchNo = "100000";
	static String merchantNo = "131245111203458";
	static String terminalNo = "21009503";
	
//	static String batchNo = "000001";
//	static String merchantNo = "270440053991236";
//	static String terminalNo = "10000584";
	ISOMsg request = new ISOMsg();
	static QueryRunner  dao = null;
	static HEXChannel channel = new HEXChannel("120.132.177.194", 4000,
			PackagerNavigator.COMMON_PACKAGER);
//	static HEXChannel channel = new HEXChannel("127.0.0.1", 4001,
//			PackagerNavigator.COMMON_PACKAGER);
	
	static{
		DruidDataSource dataSource = null;
		
		try {
			Properties props = new Properties();
			InputStream is = UpdatePublicKey.class
					.getResourceAsStream("/config/jdbcTest.properties");
			props.load(is);
			dataSource = (DruidDataSource) DruidDataSourceFactory
					.createDataSource(props);
			dao = new QueryRunner(dataSource);
			
			channel.setHeader(ISOUtil.hex2byte("6000060000602200000000"));
			channel.setTimeout(50000);
			channel.connect();
			channel.setLogger(null, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws ISOException, IOException, SQLException {
		downloadPublicKey();
//		downloadParams();
		
	}
	
	public static void downloadPublicKey() throws ISOException, IOException, SQLException{
		//pos状态上送公钥下载
		ISOMsg request = new ISOMsg();
		request.setPackager(PackagerNavigator.COMMON_PACKAGER);
		request.setMTI("0820");
		request.set(41, terminalNo);
		request.set(42, merchantNo);
		request.set(60, "00"+batchNo+"372");
		request.set(62, "100".getBytes());
		
		System.out.println(ISOUtil.hexdump(request.pack()));
		System.out.println(Utils.dump(request));
		
		//发送
		
		channel.send(request);
		ISOMsg pkUpResponse = channel.receive();
		
		System.out.println(Utils.dump(pkUpResponse));

		System.out.println(ISOUtil.hexdump(pkUpResponse.pack()));
		
		String f62 = pkUpResponse.getString(62);
		String[] arrs = f62.split("9F06");
		if(arrs != null && arrs.length >0){
			if(!"31".equals(arrs[0])){
				return;
			}
			String sql = "delete from rid_struct";
			dao.update(sql);
			for(int i=1; i<arrs.length; i++){
				TLVList tlvList = new TLVList();
				tlvList.unpack(ISOUtil.hex2byte(("9F06"+arrs[i])));
				String rid = tlvList.getString(0x9F06);
				String ind = tlvList.getString(0x9F22);	
				String exp = tlvList.getString(0xDF05);
				
				TLVList t = new TLVList();
				t.append(0x9F06, rid);
				t.append(0x9F22, ind);
				
				//POS参数传递
				ISOMsg req = new ISOMsg();
				req.setPackager(PackagerNavigator.COMMON_PACKAGER);
				req.setMTI("0800");
				req.set(41, terminalNo);
				req.set(42, merchantNo);
				req.set(60, "00"+batchNo+"370");
				req.set(62, t.pack());
				
				System.out.println("************rid="+rid+"&ind="+ind);
				
				System.out.println(ISOUtil.hexdump(req.pack()));
				System.out.println(Utils.dump(req));
				
				channel.send(req);
				ISOMsg pSResponse = channel.receive();
				System.out.println(Utils.dump(pSResponse));
				
				f62 = pSResponse.getString(62);
				if(pSResponse != null && pSResponse.getString(62) != null){
					if(pSResponse.getString(62).startsWith("31")){
						TLVList tlvlist = new TLVList();
						tlvlist.unpack(ISOUtil.hex2byte(f62), 1);
						String tlv_9f06 = tlvlist.getString(0x9F06);
						String tlv_9f22 = tlvlist.getString(0x9F22);
						String tlv_df05 = tlvlist.getString(0xdf05);
						String tlv_df06 = tlvlist.getString(0xdf06);
						String tlv_df07 = tlvlist.getString(0xdf07);
						String tlv_df02 = tlvlist.getString(0xdf02);
						String tlv_df04 = tlvlist.getString(0xdf04);
						String tlv_df03 = tlvlist.getString(0xdf03);
						
						sql = "insert into rid_struct (`rid`,`ind`,`exp`,`hash_alg`,`rid_alg`,`mod`,`idx`,`ck`) values (?,?,?,?,?,?,?,?)";
						dao.update(sql, new Object[]{tlv_9f06, tlv_9f22, tlv_df05, tlv_df06, tlv_df07, tlv_df02, tlv_df04, tlv_df03});
					}
				}
			}
		}	
		//发送公钥下载结束报文
		request.setMTI("0800");
		request.set(60, "00"+batchNo+"371");
		request.unset(62);
		System.out.println(ISOUtil.hexdump(request.pack()));
		System.out.println(Utils.dump(request));
		
		//发送
		channel.send(request);
		ISOMsg pkDownloadResponse = channel.receive();
		
		System.out.println(Utils.dump(pkDownloadResponse));
		System.out.println(ISOUtil.hexdump(pkDownloadResponse.pack()));
		
	}
	
	public static void downloadParams() throws ISOException, IOException, SQLException{
		//pos状态上送参数下载
		ISOMsg request = new ISOMsg();
		request.setPackager(PackagerNavigator.COMMON_PACKAGER);
		request.setMTI("0820");
		request.set(41, terminalNo);
		request.set(42, merchantNo);
		request.set(60, "00"+batchNo+"382");
		request.set(62, "100".getBytes());
		System.out.println(ISOUtil.hexdump(request.pack()));
		System.out.println(Utils.dump(request));
		channel.send(request);
		ISOMsg pdResponse = channel.receive();
		System.out.println(Utils.dump(pdResponse));
		if(pdResponse != null && pdResponse.getString(62) != null){
			String f62 = pdResponse.getString(62);
			String[] strs = f62.split("9F06");
			if(strs != null && strs.length >0){
				if(!"31".equals(strs[0])){
					return;
				}
				String sql = "delete from aid_struct";
				dao.update(sql);
				for(int i=1; i<strs.length; i++){				
					TLVList tlvList = new TLVList();
					tlvList.unpack(ISOUtil.hex2byte(("9F06"+strs[i])));
					String aid = tlvList.getString(0x9F06);
					TLVList t = new TLVList();
					tappend.(0x9F06, aid);
					
					
					//POS参数传递
					ISOMsg req = new ISOMsg();
					req.setPackager(PackagerNavigator.COMMON_PACKAGER);
					req.setMTI("0800");
					req.set(41, terminalNo);
					req.set(42, merchantNo);
					req.set(60, "00"+batchNo+"380");
					req.set(62, t.pack());
					
					System.out.println("&&&&&&&&&&&&&&aid="+aid);					
					System.out.println(ISOUtil.hexdump(req.pack()));
					System.out.println(Utils.dump(req));
					
					channel.send(req);
					ISOMsg pSResponse = channel.receive();
					System.out.println(Utils.dump(pSResponse));
					
					f62 = pSResponse.getString(62);
					if(pSResponse != null && pSResponse.getString(62) != null){
						if(pSResponse.getString(62).startsWith("31")){
							TLVList tlvlist = new TLVList();
							tlvlist.unpack(ISOUtil.hex2byte(f62), 1);
							String tlv_9f06 = tlvlist.getString(0x9f06);
							String tlv_df01 = tlvlist.getString(0xdf01);
							String tlv_9f08 = tlvlist.getString(0x9f08);
							String tlv_df13 = tlvlist.getString(0xdf13);
							String tlv_df12 = tlvlist.getString(0xdf12);
							String tlv_df11 = tlvlist.getString(0xdf11);
							String tlv_9f1b = tlvlist.getString(0x9f1b);
							String tlv_df15 = tlvlist.getString(0xdf15);
							String tlv_df16 = tlvlist.getString(0xdf16);
							String tlv_df17 = tlvlist.getString(0xdf17);
							String tlv_df14 = tlvlist.getString(0xdf14);
							String tlv_df18 = tlvlist.getString(0xdf18);
							String tlv_9f7b = tlvlist.getString(0x9f7b);
							String tlv_df19 = tlvlist.getString(0xdf19);
							String tlv_df20 = tlvlist.getString(0xdf20);
							String tlv_df21 = tlvlist.getString(0xdf21);
							
							
							sql = "insert into aid_struct (`aid`,`asi`,`ver`,`tac_deninal`,`tac_online`,`tac_default`,`floor_limit`,`threshold`,`threshold_percent`,`threshold_val`,`ddol`,`online_pin`,`terminal_limit`,`no_touch_lowest_limit`,`no_touch_sale_limit`,`validate_method`) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
							dao.update(sql, new Object[]{tlv_9f06, tlv_df01, tlv_9f08, tlv_df13, tlv_df12, tlv_df11, tlv_9f1b, tlv_df15, tlv_df16, tlv_df17, tlv_df14, tlv_df18, tlv_9f7b, tlv_df19, tlv_df20, tlv_df21});
						}
					}
				}
			}
		}
		
		//发送参数下载结束报文
		request.setMTI("0800");
		request.set(60, "00"+batchNo+"381");
		request.unset(62);
		System.out.println(ISOUtil.hexdump(request.pack()));
		System.out.println(Utils.dump(request));
		
		//发送
		channel.send(request);
		ISOMsg pDownloadResponse = channel.receive();
		
		System.out.println(Utils.dump(pDownloadResponse));
		System.out.println(ISOUtil.hexdump(pDownloadResponse.pack()));
	}

}
*/
