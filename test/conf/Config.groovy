package conf

datasource {
	jdbc.driver   = "com.mysql.jdbc.Driver"
	jdbc.url      = "jdbc:mysql://192.168.1.30:3306/posp?userUnicode=true&characterEncoding=UTF-8"
	jdbc.user     = "root"
	jdbc.password = "123qwe"

	pool.MinIdle                            = 2
	pool.MaxActive                          = 10
	pool.MaxWait                            = 20000
	pool.TestWhileIdle                      = true
	pool.TimeBetweenEvictionRunsMillis      = 60000
	pool.MinEvictableIdleTimeMillis         = 1800000
	pool.ValidationQuery                    = 'select 1 from dual'
}

commons {
	register.position = true
	reqTimeCheck = false
	defaulAgencyCode = '1000'
	maxFileUpload = 1024 * 1024 * 2
   sendMsgAddr = 'http://192.168.1.30:9448/message/post'
//    sendMsgAddr = 'http://192.168.1.38:8080/EIMS/inAccess/sms'
//    sendMsgAddr = 'http://192.168.250.51:13002/EIMS/inAccess/sms'
}

email {
    host = 'smtp.exmail.qq.com'
    port = 465
    from = 'hl@yogapay.com'
    username = 'hl@yogapay.com'
    password = 'MTIzcXdl'
    ssl = true
}