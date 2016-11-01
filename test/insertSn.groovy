import com.alibaba.druid.pool.DruidDataSource
import groovy.sql.Sql

/**
 * Created by lei on 14-5-4.
 */
def ds = new DruidDataSource()
ds.url = 'jdbc:mysql://192.168.2.111:3306/posp?userUnicode=true&characterEncoding=UTF-8'
ds.username = 'root'
ds.password = '123qwe'
def sql     = new Sql(ds)
def file1 = new File('D:\\资料\\设备号\\A14D 和付通SN号\\第十三箱.txt')
def i = 1069
file1.eachLine('gbk') {
    def sb = new StringBuffer()
    sb.append("insert into ksn_info (id,ksn_no,enable,create_time,is_activated) values (${i},${it},1,${new Date()},0);\n")
    sb.append("insert into secret_key (id,device_id,bdk,device_type,create_time) values (${i},${it},'0123456789ABCDEFFEDCBA9876543210','qpos',${new Date()});\n")
    println(sb.toString())
    i++
}