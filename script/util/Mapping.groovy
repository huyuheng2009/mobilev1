package util

import op.*

/**
 * @author hanlei
 */
public interface Mapping {
	static conf = [
			// 用户业务
			'/user/login/?'                     : Login,
			'/user/logout/?'                    : Logout,
			'/user/register/?'                  : Register,
			'/user/hello/?'                     : Hello,
			'/user/echo/?'                      : Hello,
			'/user/changePassword/?'            : ChangePassword,
			'/user/resetPassword/?'             : ResetPassword,
            '/user/realnameAuth/?'              :  RealnameAuth,
            '/user/accountAuth/?'               : AccountAuth,
            '/user/info/?'                       : UserInfo,
            '/user/changeMobileNo/?'           : ChangeMobile,

			// 刷卡器业务
            '/swiper/check/?'                   : SwiperCheck,
			'/swiper/register/?'                : SwiperRegister,
			'/swiper/reset/?'                   : SwiperReset,
			'/swiper/change/?'                  : SwiperChange,

			// 交易类业务
			'/transaction/sale/?'               : TransSale,
			'/transaction/status/?'             : TransStatus,
			'/transaction/query/?'              : TransQuery,
            '/transcation/sendSalesSlip/?'     : SendSalesSlip,

			// 交易查询业务
			'/transaction/\\d+/?'               : TransQueryTransById,
			'/transactions/last/?'              : TransLast,
			'/transactions/?'                   : Transactions,
			'/transactions/dailies/\\d+/\\d+/?' : TransDailies,
			'/transactions/\\d+/?'              : TransDay,

			// 辅助类业务
			'/banners/?'                        : Banners,
			'/upload/\\S*/?'                    : Upload,
			'/bank/query/?'                     : BankQuery,
            '/card/query/?'                     : CardQuery,
			'/.*?\\.html'                       : StaticPage,
            '/.*?\\.css'                        :StaticPage,
            '/.*?\\.js'                        :StaticPage,
			'/version/version.txt'          : VersionCheck,
            '/getIdCode'                        : GetIdCode,
            '/validate'                         : Validate,

			// 内部辅助测试
			'/_ws_test_/?'                      : WSTest,
	]
}
