datasource {
	jdbc.driver   = "com.mysql.jdbc.Driver"
    //jdbc.url      = "jdbc:mysql://127.0.0.1:3306/posp?userUnicode=true&characterEncoding=UTF-8"
	jdbc.url      = "jdbc:mysql://192.168.1.30:3306/posp?userUnicode=true&characterEncoding=UTF-8"
    //jdbc.url      = "jdbc:mysql://183.62.232.130:3306/posp?userUnicode=true&characterEncoding=UTF-8"
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
//   sendMsgAddr = 'http://183.62.232.130:8090/message/post'
   sendMsgAddr = 'http://192.168.1.30:9448/message/post'
//    sendMsgAddr = 'http://192.168.1.38:8080/EIMS/inAccess/sms'
//    sendMsgAddr = 'http://192.168.250.51:13002/EIMS/inAccess/sms'
}

email {
    host = 'smtp.exmail.qq.com'
    port = 465
    from = 'salesslip@yogapay.com'
    username = 'salesslip@yogapay.com'
    password = 'eW9nYXBheTIwMTQ='
    ssl = true
}