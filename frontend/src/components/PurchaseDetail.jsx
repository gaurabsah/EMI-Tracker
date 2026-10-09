import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api } from '../api'
import { formatBps, formatDate, formatPaise } from '../money'
import { useLoad } from '../useLoad'
import { Async, ErrorText, StatusBadge } from './ui'

/** Purchase header + schedule table. Company passes canPay to get "Mark paid" buttons; Customer view is read-only. */
export default function PurchaseDetail({ load, backTo, canPay = false }) {
  const { id } = useParams()
  const state = useLoad(() => load(id), [id])
  const [payingId, setPayingId] = useState(null)
  const [payError, setPayError] = useState('')

  async function markPaid(instalmentId) {
    setPayingId(instalmentId)
    setPayError('')
    try {
      await api.payInstalment(instalmentId)
      state.reload()
    } catch (e) {
      setPayError(e.message)
    } finally {
      setPayingId(null)
    }
  }

  return (
    <Async state={state}>
      {(p) => (
        <>
          <p><Link to={backTo}>← Back to purchases</Link></p>
          <h1>{p.itemName}</h1>
          <dl className="facts">
            <div><dt>Customer</dt><dd>{p.customerName}</dd></div>
            <div><dt>Price</dt><dd>{formatPaise(p.price)}</dd></div>
            <div><dt>Down payment</dt><dd>{formatPaise(p.downPayment)}</dd></div>
            <div><dt>Financed</dt><dd>{formatPaise(p.financed)}</dd></div>
            <div><dt>Plan</dt><dd>{p.method === 'FLAT' ? 'Flat' : 'Reducing'} · {formatBps(p.annualRateBps)} · {p.tenureMonths} months</dd></div>
            <div><dt>Start date</dt><dd>{formatDate(p.startDate)}</dd></div>
            <div><dt>Outstanding balance</dt><dd className="big">{formatPaise(p.outstandingBalance)}</dd></div>
            <div><dt>Overdue instalments</dt><dd>{p.overdueCount}</dd></div>
          </dl>

          <ErrorText message={payError} />
          <table>
            <thead>
              <tr>
                <th>#</th><th>Due date</th>
                <th className="num">Principal</th><th className="num">Interest</th><th className="num">Amount</th>
                <th>Status</th><th>Paid on</th>
                {canPay && <th />}
              </tr>
            </thead>
            <tbody>
              {p.instalments.map((i) => (
                <tr key={i.id}>
                  <td>{i.seq}</td>
                  <td>{formatDate(i.dueDate)}</td>
                  <td className="num">{formatPaise(i.principal)}</td>
                  <td className="num">{formatPaise(i.interest)}</td>
                  <td className="num">{formatPaise(i.amount)}</td>
                  <td><StatusBadge instalment={i} /></td>
                  <td>{formatDate(i.paidOn)}</td>
                  {canPay && (
                    <td>
                      {i.status === 'PENDING' && (
                        <button disabled={payingId === i.id} onClick={() => markPaid(i.id)}>
                          {payingId === i.id ? 'Saving…' : 'Mark paid'}
                        </button>
                      )}
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </>
      )}
    </Async>
  )
}
