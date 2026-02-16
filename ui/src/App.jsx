import { useState, useEffect } from 'react'

export default function App() {
  const [state, setState] = useState({ status: 'loading', trades: [], error: null })

  useEffect(() => {
    fetch('/api/trades', { credentials: 'include' })
      .then(res => {
        if (res.status === 401 || res.redirected) {
          setState({ status: 'unauthenticated', trades: [], error: null })
          return null
        }
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(data => {
        if (data !== null) setState({ status: 'authenticated', trades: data, error: null })
      })
      .catch(err => setState({ status: 'error', trades: [], error: err.message }))
  }, [])

  if (state.status === 'loading') return <p>Loading…</p>

  if (state.status === 'error') return <p style={{ color: 'red' }}>Error: {state.error}</p>

  if (state.status === 'unauthenticated') {
    return (
      <div style={{ textAlign: 'center', marginTop: '4rem' }}>
        <h1>Trade UI</h1>
        <a href="/oauth2/authorization/google">
          <button style={{ fontSize: '1.2rem', padding: '0.6rem 1.2rem' }}>
            Login with Google
          </button>
        </a>
      </div>
    )
  }

  return (
    <div style={{ margin: '2rem' }}>
      <header style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h1>Trades — ASX</h1>
        <a href="/logout">Logout</a>
      </header>

      {state.trades.length === 0 ? (
        <p>No trades found.</p>
      ) : (
        <table border="1" cellPadding="6" cellSpacing="0">
          <thead>
            <tr>
              <th>Date</th>
              <th>Code</th>
              <th>Side</th>
              <th>Size</th>
              <th>Price</th>
            </tr>
          </thead>
          <tbody>
            {state.trades.map((t, i) => (
              <tr key={t.id ?? i}>
                <td>{t.tradeDate ?? t.date ?? '—'}</td>
                <td>{t.code}</td>
                <td>{t.side}</td>
                <td>{t.size}</td>
                <td>{t.price}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  )
}
