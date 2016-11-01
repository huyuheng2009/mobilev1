package op

import util.Commons

/**
 * Created with IntelliJ IDEA.
 * User: hanlei
 * Date: 14-1-23
 * Time: 上午10:57
 * To change this template use File | Settings | File Templates.
 */
def cardNo = req.getParameter("cardNo")
def cardbin = dao.findCardbin(cardNo)
def result = [
    reqNo: req.getParameter('reqNo')?:null,
    cardType : cardbin?.card_type,
    issuerName : cardbin?.issuer_name,
    cardbinName : cardbin?.cardbin_name
]
render Commons.success(result)