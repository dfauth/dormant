import { useEffect, useState } from 'react'

const CONSENSUS_LABEL = {
  STRONG_BUY: 'Strong Buy',
  BUY: 'Buy',
  HOLD: 'Hold',
  SELL: 'Sell',
  STRONG_SELL: 'Strong Sell',
}

function consensusClass(consensus) {
  if (consensus === 'STRONG_BUY' || consensus === 'BUY') return 'consensus-buy'
  if (consensus === 'STRONG_SELL' || consensus === 'SELL') return 'consensus-sell'
  return 'consensus-hold'
}

export default function Valuations() {
  const [state, setState] = useState({ data: [], loading: true, error: null })

  useEffect(() => {
    fetch('/api/valuations/recent', { credentials: 'include' })
      .then(res => {
        if (!res.ok) throw new Error(`HTTP ${res.status}`)
        return res.json()
      })
      .then(data => setState({ data, loading: false, error: null }))
      .catch(err => setState({ data: [], loading: false, error: err.message }))
  }, [])

  if (state.loading) return <p>Loading…</p>
  if (state.error) return <p className="error">Error: {state.error}</p>

  return (
    <>
      <h1>Valuations</h1>
      {state.data.length === 0 ? (
        <p className="empty">No valuations updated in the last 3 months.</p>
      ) : (
        <table className="trades-table">
          <thead>
            <tr>
              <th>Market</th>
              <th>Code</th>
              <th>Date</th>
              <th>Consensus</th>
              <th>Price</th>
              <th>Target</th>
              <th>Potential</th>
              <th>Buy</th>
              <th>Hold</th>
              <th>Sell</th>
            </tr>
          </thead>
          <tbody>
            {state.data.map(v => (
              <tr key={`${v.market}-${v.code}`}>
                <td>{v.market}</td>
                <td>{v.code}</td>
                <td>{v.date}</td>
                <td className={consensusClass(v.consensus)}>
                  {CONSENSUS_LABEL[v.consensus] ?? v.consensus}
                </td>
                <td>{v.price ?? '—'}</td>
                <td>{v.target}</td>
                <td className={v.potential == null ? '' : Number(v.potential) >= 0 ? 'pnl-positive' : 'pnl-negative'}>
                  {v.potential == null ? '—' : `${Number(v.potential) >= 0 ? '+' : ''}${v.potential}%`}
                </td>
                <td>{v.buy}</td>
                <td>{v.hold}</td>
                <td>{v.sell}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
