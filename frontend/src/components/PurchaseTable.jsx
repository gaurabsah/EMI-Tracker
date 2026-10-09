import { Link } from 'react-router-dom'
import { formatPaise } from '../money'

export default function PurchaseTable({ purchases, basePath, showCustomer }) {
  return (
    <table>
      <thead>
        <tr>
          {showCustomer && <th>Customer</th>}
          <th>Item</th>
          <th className="num">Price</th>
          <th className="num">Down payment</th>
          <th className="num">Financed</th>
          <th className="num">Outstanding</th>
          <th className="num">Overdue</th>
          <th />
        </tr>
      </thead>
      <tbody>
        {purchases.length === 0 && <tr><td colSpan="8" className="muted">No purchases yet.</td></tr>}
        {purchases.map((p) => (
          <tr key={p.id}>
            {showCustomer && <td>{p.customerName}</td>}
            <td>{p.itemName}</td>
            <td className="num">{formatPaise(p.price)}</td>
            <td className="num">{formatPaise(p.downPayment)}</td>
            <td className="num">{formatPaise(p.financed)}</td>
            <td className="num">{formatPaise(p.outstandingBalance)}</td>
            <td className="num">{p.overdueCount > 0 ? <span className="badge overdue">{p.overdueCount}</span> : 0}</td>
            <td><Link to={`${basePath}/${p.id}`}>View</Link></td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}
