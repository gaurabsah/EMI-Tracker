import { api } from '../../api'
import PurchaseDetail from '../../components/PurchaseDetail'

export default function CompanyPurchaseDetail() {
  return <PurchaseDetail load={api.getPurchase} backTo="/company/purchases" canPay />
}
