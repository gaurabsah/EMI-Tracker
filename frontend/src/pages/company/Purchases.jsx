import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../../api'
import PurchaseTable from '../../components/PurchaseTable'
import { Async, ErrorText } from '../../components/ui'
import { formatBps, toMinorUnits } from '../../money'
import { useForm } from '../../useForm'
import { useLoad } from '../../useLoad'

const EMPTY = { customerId: '', planId: '', itemName: '', price: '', downPayment: '0', startDate: '' }

function CreatePurchase({ customers, plans }) {
  const navigate = useNavigate()
  const { values, field } = useForm(EMPTY)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError('')
    const price = toMinorUnits(values.price)
    const downPayment = toMinorUnits(values.downPayment)
    if (price === null || downPayment === null) {
      setError('Price and down payment must be amounts such as 70000 or 70000.50')
      return
    }
    if (downPayment >= price) {
      setError('Down payment must be less than the price')
      return
    }
    setBusy(true)
    try {
      const created = await api.createPurchase({
        customerId: Number(values.customerId),
        planId: Number(values.planId),
        itemName: values.itemName,
        price,
        downPayment,
        startDate: values.startDate || null,
      })
      navigate(`/company/purchases/${created.id}`)
    } catch (err) {
      setError(err.message)
      setBusy(false)
    }
  }

  if (customers.length === 0 || plans.length === 0) {
    return <p className="card muted">Create at least one plan and one customer before registering a purchase.</p>
  }

  return (
    <form onSubmit={submit} className="row card">
      <select required {...field('customerId')}>
        <option value="">Customer…</option>
        {customers.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
      </select>
      <select required {...field('planId')}>
        <option value="">Plan…</option>
        {plans.map((p) => (
          <option key={p.id} value={p.id}>
            {p.name} ({p.method === 'FLAT' ? 'Flat' : 'Reducing'} {formatBps(p.annualRateBps)}, {p.tenureMonths} mo)
          </option>
        ))}
      </select>
      <input placeholder="Item" required {...field('itemName')} />
      <input placeholder="Price (₹)" required inputMode="decimal" {...field('price')} />
      <input placeholder="Down payment (₹)" required inputMode="decimal" {...field('downPayment')} />
      <label className="inline">Start date <input type="date" {...field('startDate')} /></label>
      <button type="submit" disabled={busy}>{busy ? 'Creating…' : 'Create purchase'}</button>
      <ErrorText message={error} />
    </form>
  )
}

export default function Purchases() {
  const purchases = useLoad(api.listPurchases)
  const options = useLoad(() => Promise.all([api.listCustomers(), api.listPlans()]))

  return (
    <>
      <h1>Purchases</h1>
      <Async state={options}>{([customers, plans]) => <CreatePurchase customers={customers} plans={plans} />}</Async>
      <Async state={purchases}>
        {(list) => <PurchaseTable purchases={list} basePath="/company/purchases" showCustomer />}
      </Async>
    </>
  )
}
