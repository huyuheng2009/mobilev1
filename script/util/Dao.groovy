package util

import groovy.sql.Sql

import java.beans.PropertyChangeListener

/**
 * @author hanlei
 */
class Dao {
	Sql db;

	Dao(ds) {
		db = new Sql(ds)
	}

	def wrap(data, table, id = 'id') {
		if (data == null || data instanceof ObservableMap) {
			return data
		} else {
			def om = new ObservableMap(data)
			om._table = table
			om._id = id
			om._update_fields = [] as Set
			om.addPropertyChangeListener( { evt ->
				evt.source._update_fields << evt.propertyName
			} as PropertyChangeListener)
			return om
		}
	}

	def update(ObservableMap data) {
		if (!data || !data.id || !data._table) return
		if (!data._update_fields) return true
		def params = []
		String updateSql = "update ${data._table} set "
		for (String field in data._update_fields) {
			if (data[field] != null) {
				updateSql += (params ? ', ' : '') + " $field = ?"
				params << data[field]
			}
		}
		updateSql += " where ${data._id} = ?"
		params << data[data._id]

		Commons.log.info "update sql: $updateSql", "params: $params"
		1 == db.executeUpdate(updateSql, params)
	}

	def findKSNByKSNNO(String ksnNo) {
		if (!ksnNo) return null
		wrap(db.firstRow("select * from ksn_info where ksn_no = ${ksnNo}"), 'ksn_info')
	}

	def findKSNByMerchantId(merchantId) {
		if (!merchantId) return null
		db.firstRow("select k.* from mcr_ksn k join merchant_terminal t on k.terminal_id=t.id where t.merchant_id=${merchantId} and k.is_used=${true} and k.is_activated=${true}")
	}

	def findActiveCodeByCode(code) {
		if (!code) return null
		wrap(db.firstRow("select * from active_code where code=${code}"), 'active_code')
	}

	def findActiveCodeByKSNId(ksnId) {
		if (!ksnId) return null
		wrap(db.firstRow("select * from active_code where ksn_id=${ksnId}"), 'active_code')
	}

    def findAgentById(agentId) {
        if (!agentId) return null
        wrap(db.firstRow("select * from agent_info where id=${agentId}"), 'agent_info')
    }

    def findPoductByAgencyId(agencyId) {
        if (!agencyId) return null
        db.firstRow("""select dp.* from dict_product dp left join agency_dict_product adp on adp.dict_product_id=dp.id
            left join agency a on a.id=adp.agency_products_id where a.id = ${agencyId}
    """)
    }

	def findMerchantById(merchantId) {
		if (!merchantId) return null
		db.firstRow("select * from cm_personal c join cm_merchant m on c.id=m.id where m.id=${merchantId}")
	}

	def findMerchantByLoginName(lognName) {
		if (!lognName) return null
		db.firstRow("select * from cm_personal c join cm_merchant m on c.id=m.id where c.mobile_no=${lognName?.toString()}")
	}

	def findTerminalById(terminalId) {
		if (!terminalId) return null
		db.firstRow("select * from merchant_terminal where enabled=${true} and id=${terminalId}")
	}

	def findTerminalByMerchantId(merchantId) {
		if (!merchantId) return null
		wrap(db.firstRow("select * from merchant_terminal where enabled=${true} and merchant_id=${merchantId}"), 'merchant_terminal')
	}

	def findPosTerminalByKsnNo(ksnNo) {
		if (!ksnNo) return null
		wrap(db.firstRow("""select t.* from
			pos_terminal t join ksn_info k on k.terminal_no=t.terminal_no
			where t.open_status=${true} and k.is_activated=${true} and k.enable=${true} and k.ksn_no = ${ksnNo}
		"""), 'pos_terminal')
	}

	def findCustomerUserByLoginName(loginName) {
		if (!loginName) return null
		wrap(db.firstRow("select * from customer_user where user_name=${loginName}"), 'customer_user')
	}

    def findCustomerUserByMerchantNo(merchantNo){
        if (!merchantNo) return null
        wrap(db.firstRow("select * from customer_user where merchant_no=${merchantNo}"), 'customer_user')
    }

	def findCardbin(cardno) {
		db.firstRow("select * from card_bin c where c.card_length=length(${cardno}) and c.verify_code=left(${cardno}, c.verify_length) order by c.verify_length desc")
	}

	def findSessionById(sid) {
		wrap(db.firstRow("select * from mobile_session where id=${sid}"), 'mobile_session')
	}

	def findSessionBySessionNo(sessionNo) {
		wrap(db.firstRow("select * from mobile_session where session_no=${sessionNo}"), 'mobile_session')
	}

    def findOperatorByMerchantID(merchantId){
        wrap(db.firstRow("select * from merchant_operator o where o.merchant_id=${merchantId}"),'merchant_operator')
    }

    def findMerchantBindingCardCountByMerchantNo(merchantNo){
        db.firstRow("select count(id) count from merchant_binding_card mbc where mbc.merchant_no=${merchantNo}")
    }

    def findMerchantBindingCardByMerchantNo(merchantNo){
        db.rows("select * from merchant_binding_card mbc where mbc.merchant_no=${merchantNo}")
    }

    def findTerminalModelByProductModel(model){
        db.firstRow("""select * from dict_terminal_model t where t.product_model =${model}
        """)
    }

    def findeMobileIdentifyCodeByMobile(mobile){
        if(!mobile) return null
        wrap(db.firstRow("""select * from mobile_identify_code i where i.mobile_no=${mobile}"""),'mobile_identify_code')
    }

    def findSumAmountMonthByTerminalNo(terminalNo){
        if(!terminalNo) return null
        def amountHistory = db.firstRow("""
          select IFNULL(sum(t.trans_amount),0) sum from trans_info t where
            t.terminal_no = ${terminalNo}  and t.trans_type='SALE' and t.trans_status='SUCCESS'
            and date_format(t.create_time,'%Y-%m')=date_format(${new Date()},'%Y-%m')
        """).sum as Long
        return  amountHistory

    }

    def findSumAmountDailyByTerminalNo(terminalNo){
        if(!terminalNo) return null
        def amount = db.firstRow("""
          select IFNULL(sum(t.trans_amount),0) sum from trans_info t where
        t.terminal_no = ${terminalNo} and t.trans_type='SALE' and t.trans_status='SUCCESS'
        and date(t.create_time) = curdate()
         """).sum as Long
        return amount
    }

    def findLastTransByTerminalNo(terminalNo, cardNo){
        db.firstRow("""select * from trans_current t where t.terminal_no=${terminalNo} and t.card_no=${cardNo}
        and t.trans_type='sale' and t.trans_status='1' order by id desc""")
    }

    def findFeeRateSettingByMerchantId(merchantId){
        db.firstRow("""select f.* from fee_rate_setting f left join merchant_fee_rate mf on mf.fee_rate_setting_id = f.id
           where mf.merchant_id = ${merchantId}
        """)
    }

    def findBankAccountByAccountNo(accountNo){
        db.firstRow("select * from bank_account ba left join merchant_bank_account mba on ba.id=mba.bank_account_id left join cm_merchant m on mba.merchant_id=m.id where ba.account_no=${accountNo}")
    }

    def findQueryCountByTerminalNo(terminalNo){
        db.firstRow("""select nvl(count(id),0) count from trans_current where terminal_no=${terminalNo} and trans_type='query'
            and comp_status=2
      """)
    }

    def findMobileMerchantById(id){
        if(!id) return null
        wrap(db.firstRow("select * from mobile_merchant p where p.id=${id}"),'mobile_merchant')
    }

    def findMerchantOperatorByMerchantId(merchantId){
         if(!merchantId) return null
        wrap(db.firstRow("select * from merchant_operator o where o.merchant_id=${merchantId}"),'merchant_operator')
    }

    def findMobileMerchantByMobileNo(mobileNo){
        if(!mobileNo)  return null
        db.firstRow("select * from mobile_merchant m where m.mobile_no=${mobileNo}")
    }

    def saveWsInterfaceCount(interfaceName, merchantNo){
        db.execute("insert into ws_interface_count (id,interface_name,date_created,merchant_no) values (seq_wsinterfacecount.nextval,${interfaceName},${new Date().toTimestamp()},${merchantNo})")
    }

    def findLoginNameByMobileNo(mobileNo){
        if(!mobileNo) return null
        def sql = """select o.login_name from cm_personal p left join merchant_operator o on p.id=o.merchant_id where p.mobile_no=${mobileNo}
        and o.login_name like ${mobile} and p.type_code !='01' and p.type_code is not null"""
        db.firstRow(sql)
    }

    def updateLoginNameByOperatorId(loginName, id){
        if(!id) return null
        println("update merchant_operator o set o.login_name=${loginName} where o.id=${id}")
        db.execute("update merchant_operator o set o.login_name=${loginName} where o.id=${id}")
    }

    def findIssuerByIssuerNo(issuerNo){
        if(!issuerNo) return null
        wrap(db.firstRow("select * from dict_issuer i where i.issuer_no=${issuerNo}"),'dict_issuer')
    }

    def findDictTerminalModelByModel(model){
        if(!model) return null
        wrap(db.firstRow("select * from dict_terminal_model t where t.product_model=${model}"), 'dict_terminal_model')
    }

    def findPosMerchantByMerchantNo(merchantNo){
        if(!merchantNo) return null
        wrap(db.firstRow("select * from pos_merchant p where p.merchant_no=${merchantNo}"), 'pos_merchant')
    }

    def findMobileMerchantByMerchantNo(merchantNo){
        if(!merchantNo) return null
        wrap(db.firstRow("select * from mobile_merchant m where m.merchant_no=${merchantNo}"), 'mobile_merchant')
    }

    def findTerminalByMerchantNo(merchantNo){
        if(!merchantNo) return null
        wrap(db.firstRow("select * from pos_terminal t where t.merchant_no=${merchantNo}"), 'pos_terminal')
    }

    def findKeyByKsnNo(ksnNo){
        if(!ksnNo) return null
        wrap(db.firstRow("select * from secret_key s where s.device_id = ${ksnNo}"), 'secret_key')
    }

    def findKsnInfoByTerminalNo(terminalNo){
       if(!terminalNo) return null
        wrap(db.firstRow("select * from ksn_info k where k.terminal_no = ${terminalNo}"), 'ksn_info')
    }

    def findRiskValueByProductCodeAndModel(productCode, model){
        if(!productCode) return null
        wrap(db.firstRow("select * from mobile_risk_value r where r.product_code=${productCode} and r.model=${model}"),'mobile_risk_value')
    }

    def findWhiteListByMerchantNo(merchantNo){
        if(!merchantNo) return null
        wrap(db.firstRow("select * from white_list w where w.merchant_no=${merchantNo}"), 'white_list')
    }
}