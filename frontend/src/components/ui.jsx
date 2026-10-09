export function Loading() {
  return <p className="muted">Loading…</p>
}

export function ErrorText({ message }) {
  return message ? <p className="error" role="alert">{message}</p> : null
}

export function Async({ state, children }) {
  if (state.loading && !state.data) return <Loading />
  if (state.error) return <ErrorText message={state.error} />
  return children(state.data)
}

export function StatusBadge({ instalment }) {
  if (instalment.status === 'PAID') return <span className="badge paid">Paid</span>
  if (instalment.overdue) return <span className="badge overdue">Overdue</span>
  return <span className="badge upcoming">Upcoming</span>
}
