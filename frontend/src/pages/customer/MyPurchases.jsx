import { api } from '../../api'
import PurchaseTable from '../../components/PurchaseTable'
import { Async } from '../../components/ui'
import { useLoad } from '../../useLoad'

export default function MyPurchases() {
  const state = useLoad(api.listMyPurchases)
  return (
    <>
      <h1>My purchases</h1>
      <Async state={state}>
        {(list) => <PurchaseTable purchases={list} basePath="/me/purchases" />}
      </Async>
    </>
  )
}
