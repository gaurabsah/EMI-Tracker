import { api } from '../../api'
import PurchaseDetail from '../../components/PurchaseDetail'

export default function MyPurchaseDetail() {
  return <PurchaseDetail load={api.getMyPurchase} backTo="/me/purchases" />
}
