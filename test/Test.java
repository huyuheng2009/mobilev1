import http.CommonHttpConnection;
import http.HttpConnectionParameters;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Test{
    public static void main(String[] args) throws ParseException {
        userInfo();
    }

    public static void login(){
        SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String url = "http://127.0.0.1:29009/user/login";
        Map<String,String> params = new HashMap<String, String>();
        params.put("reqTime",sf.format(new Date()));
        params.put("reqNo","2657442");
        params.put("loginName","18988790466");
        params.put("password","123456");
        params.put("appVersion","android.qrpos.1.2.7424");
        params.put("product","HFT");
        HttpConnectionParameters connectionParameters = new HttpConnectionParameters(url,"POST",202000,true,true,true,new HashMap<String, String>());
        try{
            String response = CommonHttpConnection.proccess(connectionParameters, params);
            System.out.println(response);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public  static void userInfo(){
        //String url = "http://127.0.0.1:29002/user/info";
         String url = "http://192.168.1.30:29002/user/info";
        Map<String,String> requestProperty = new HashMap<String, String>();
        Map<String,String> params = new HashMap<String, String>();
        requestProperty.put("HFTNO","fec4f522125141829d8d5bbeb30de185");
        HttpConnectionParameters connectionParameters = new HttpConnectionParameters(url,"POST",2002000,true,true,true,requestProperty);
        try{
            String response = CommonHttpConnection.proccess(connectionParameters, params);
            System.out.println(response);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public  static void changeMobile(){
        String url = "http://127.0.0.1:29002/user/changeMobileNo";
        Map<String,String> requestProperty = new HashMap<String, String>();
        Map<String,String> params = new HashMap<String, String>();
       requestProperty.put("HFTNO","00dc5c56564145299d93f62f0cd7f1fc");
        //  params.put("newMobile","18988790461");
        params.put("password","123456");
        params.put("idCode","1234");
        HttpConnectionParameters connectionParameters = new HttpConnectionParameters(url,"POST",2002000,true,true,true,requestProperty);
        try{
            String response = CommonHttpConnection.proccess(connectionParameters, params);
            System.out.println(response);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public static void getIdCode(){
        String url = "http://192.168.1.30:29002/getIdCode";
        Map<String,String> requestProperty = new HashMap<String, String>();
        Map<String,String> params = new HashMap<String, String>();
        params.put("reqTime","2014-08-25 11:17:56");
        params.put("mobile","15820724996");
        params.put("appVersion","android.qrpos.1.2.7424");
        params.put("ksnNo","12000011214031100026");
        params.put("product","HFT");
        HttpConnectionParameters connectionParameters = new HttpConnectionParameters(url,"POST",2002000,true,true,true,requestProperty);
        try{
            String response = CommonHttpConnection.proccess(connectionParameters, params);
            System.out.println(response);
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}