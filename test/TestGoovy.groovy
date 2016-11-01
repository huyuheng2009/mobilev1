import com.alibaba.druid.pool.DruidDataSource
import groovy.sql.Sql
import util.Commons
import util.Dao

import java.text.SimpleDateFormat

/**
 * Created by zfj on 2014/7/24.
 */


def ds = new DruidDataSource()
ds.url = 'jdbc:mysql://192.168.1.30:3306/posp?userUnicode=true&characterEncoding=UTF-8'
ds.username = 'root'
ds.password = '123qwe'
def db     = new Sql(ds)

def sql = "select * from mobile_banner where  product_code='HFT'"
def banner = []
db.rows(sql, 0, 10)?.each {
    banner << [
            bannerId: it.id,
            bannerName: it.name,
            imageURI: it.uri,
            jumpType: it.jump_type,
            jumpAddr: it.jump_address,
    ]
}

print banner