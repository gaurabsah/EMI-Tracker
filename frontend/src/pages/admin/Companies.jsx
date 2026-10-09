import { useState } from 'react'
import { api } from '../../api'
import { Async, ErrorText } from '../../components/ui'
import { useForm } from '../../useForm'
import { useLoad } from '../../useLoad'

export default function Companies() {
  const state = useLoad(api.listCompanies)
  const { values, field, reset } = useForm({ name: '', email: '', password: '' })
  const [error, setError] = useState('')

  async function create(e) {
    e.preventDefault()
    setError('')
    try {
      await api.createCompany(values)
      reset()
      state.reload()
    } catch (err) {
      setError(err.message)
    }
  }

  async function toggle(company) {
    setError('')
    try {
      await api.setCompanyActive(company.id, !company.active)
      state.reload()
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <>
      <h1>Companies</h1>
      <form onSubmit={create} className="row card">
        <input placeholder="Company name" required {...field('name')} />
        <input type="email" placeholder="Login email" required {...field('email')} />
        <input type="password" placeholder="Password (min 8)" required minLength={8} {...field('password')} />
        <button type="submit">Create company</button>
      </form>
      <ErrorText message={error} />

      <Async state={state}>
        {(companies) => (
          <table>
            <thead><tr><th>Name</th><th>Login</th><th>Status</th><th /></tr></thead>
            <tbody>
              {companies.length === 0 && <tr><td colSpan="4" className="muted">No companies yet.</td></tr>}
              {companies.map((c) => (
                <tr key={c.id}>
                  <td>{c.name}</td>
                  <td>{c.email}</td>
                  <td>{c.active ? 'Active' : 'Deactivated'}</td>
                  <td><button onClick={() => toggle(c)}>{c.active ? 'Deactivate' : 'Activate'}</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </Async>
    </>
  )
}
