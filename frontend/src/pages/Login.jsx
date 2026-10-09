import { useState } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { HOME, useAuth } from '../auth'
import { ErrorText } from '../components/ui'
import { useForm } from '../useForm'

export default function Login() {
  const { auth, login } = useAuth()
  const navigate = useNavigate()
  const { values, field } = useForm({ email: '', password: '' })
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  if (auth) return <Navigate to={HOME[auth.role]} replace />

  async function submit(e) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      const next = await login(values.email, values.password)
      navigate(HOME[next.role], { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login">
      <h1>EMI Tracker</h1>
      <form onSubmit={submit} className="stack">
        <label>Email <input type="email" required autoFocus {...field('email')} /></label>
        <label>Password <input type="password" required {...field('password')} /></label>
        <ErrorText message={error} />
        <button type="submit" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
      </form>
    </div>
  )
}
