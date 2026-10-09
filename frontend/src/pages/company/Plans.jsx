import { useState } from 'react'
import { api } from '../../api'
import { Async, ErrorText } from '../../components/ui'
import { formatBps, toMinorUnits } from '../../money'
import { useForm } from '../../useForm'
import { useLoad } from '../../useLoad'

export default function Plans() {
  const state = useLoad(api.listPlans)
  const { values, field, reset } = useForm({ name: '', method: 'REDUCING', ratePercent: '', tenureMonths: '' })
  const [error, setError] = useState('')

  async function create(e) {
    e.preventDefault()
    setError('')
    const annualRateBps = toMinorUnits(values.ratePercent)
    if (annualRateBps === null) {
      setError('Rate must be a number such as 12 or 12.5')
      return
    }
    try {
      await api.createPlan({
        name: values.name,
        method: values.method,
        annualRateBps,
        tenureMonths: Number(values.tenureMonths),
      })
      reset()
      state.reload()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <h1>Plans</h1>
      <form onSubmit={create} className="row card">
        <input placeholder="Plan name" required {...field('name')} />
        <select {...field('method')}>
          <option value="REDUCING">Reducing balance</option>
          <option value="FLAT">Flat rate</option>
        </select>
        <input placeholder="Annual rate %" required inputMode="decimal" {...field('ratePercent')} />
        <input type="number" placeholder="Tenure (months)" required min="1" max="360" {...field('tenureMonths')} />
        <button type="submit">Create plan</button>
      </form>
      <ErrorText message={error} />

      <Async state={state}>
        {(plans) => (
          <table>
            <thead><tr><th>Name</th><th>Method</th><th className="num">Annual rate</th><th className="num">Tenure</th></tr></thead>
            <tbody>
              {plans.length === 0 && <tr><td colSpan="4" className="muted">No plans yet.</td></tr>}
              {plans.map((p) => (
                <tr key={p.id}>
                  <td>{p.name}</td>
                  <td>{p.method === 'FLAT' ? 'Flat' : 'Reducing balance'}</td>
                  <td className="num">{formatBps(p.annualRateBps)}</td>
                  <td className="num">{p.tenureMonths} months</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Async>
    </>
  )
}
