import { useState } from 'react'

/** Controlled-form helper: `field('name')` returns { value, onChange } for an input. */
export function useForm(initial) {
  const [values, setValues] = useState(initial)
  const field = (name) => ({
    value: values[name],
    onChange: (e) => setValues((v) => ({ ...v, [name]: e.target.value })),
  })
  const reset = () => setValues(initial)
  return { values, field, reset }
}
