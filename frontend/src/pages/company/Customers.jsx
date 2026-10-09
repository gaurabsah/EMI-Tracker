import { useState } from 'react'
import { api } from '../../api'
import { Async, ErrorText } from '../../components/ui'
import { useForm } from '../../useForm'
import { useLoad } from '../../useLoad'

export default function Customers() {
  const state = useLoad(api.listCustomers)
  const { values, field, reset } = useForm({ name: '', phone: '', email: '', password: '' })
  const [error, setError] = useState('')

  async function create(e) {
    e.preventDefault()
    setError('')
    try {
      await api.createCustomer(values)
      reset()
      state.reload()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <h1>Customers</h1>
      <form onSubmit={create} className="row card">
        <input placeholder="Name" required {...field('name')} />
        <input placeholder="Phone" {...field('phone')} />
        <input type="email" placeholder="Login email" required {...field('email')} />
        <input type="password" placeholder="Password (min 8)" required minLength={8} {...field('password')} />
        <button type="submit">Create customer</button>
      </form>
      <ErrorText message={error} />

      <Async state={state}>
        {(customers) => (
          <table>
            <thead><tr><th>Name</th><th>Phone</th><th>Login</th></tr></thead>
            <tbody>
              {customers.length === 0 && <tr><td colSpan="3" className="muted">No customers yet.</td></tr>}
              {customers.map((c) => (
                <tr key={c.id}><td>{c.name}</td><td>{c.phone}</td><td>{c.email}</td></tr>
              ))}
            </tbody>
          </table>
        )}
      </Async>
    </>
  )
}
