package op

import util.Commons
import util.Dao

import javax.servlet.http.HttpServletRequest

/**
 * Created by zhanggc on 2014/7/24.
 */

//HttpServletRequest req;

def sessionNO = req.getHeader('HFTNO')
log.info "params:HFTNO->${sessionNO}"
if(sessionNO){
    def session = dao.findSessionBySessionNo(sessionNO)
    def merchantNo = session?.merchant_no
    def mobileMerchant = dao.findMobileMerchantByMerchantNo(merchantNo)
    def posMerchant = dao.findPosMerchantByMerchantNo(merchantNo)
    def posTerminal = dao.findTerminalByMerchantNo(merchantNo)
    log.info "params:posTerminal->${posTerminal}"
    if(mobileMerchant&&posMerchant){
        if(posTerminal){
            def ksnInfo = dao.findKsnInfoByTerminalNo(posTerminal.terminal_no)
            log.info "params:ksnInfo->${ksnInfo}"
            if(ksnInfo){
                render (Commons.success([
                        mobile:mobileMerchant.mobile_no,
                        name:posMerchant.merchant_name,
                        accountName:posMerchant.settle_account_name,
                        cardNo:posMerchant.settle_bank_no,
                        bankDeposit:posMerchant.settle_bank_name,
                        ksnNo:ksnInfo.ksn_no
                ])); return
            }else{
                render Commons.fail(null,'ILLEGAL_ARGUMENT','设备号ksn不存在');return
            }
        }else{
            render Commons.fail(null,'ILLEGAL_ARGUMENT','终端号terminal不存在');return
        }
    }else{
        render Commons.fail(null,'ILLEGAL_ARGUMENT','商户信息出错');return
    }
}
render Commons.fail(null,'ILLEGAL_ARGUMENT','缺少参数 HFTNO');

