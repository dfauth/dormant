import { useEffect, useState } from 'react'

const DEFAULT_MARKET = 'ASX'

function api(path, options = {}) {
  return fetch(path, { credentials: 'include', ...options })
}

function jsonPost(path, body, method = 'POST') {
  return api(path, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
}

export default function Watchlists() {
  const [watchlists, setWatchlists] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selected, setSelected] = useState(null)
  const [creating, setCreating] = useState(false)
  const [newName, setNewName] = useState('')
  const [renamingId, setRenamingId] = useState(null)
  const [renameValue, setRenameValue] = useState('')
  const [addMarket, setAddMarket] = useState(DEFAULT_MARKET)
  const [addCode, setAddCode] = useState('')

  useEffect(() => {
    api('/api/watchlists')
      .then(r => r.ok ? r.json() : Promise.reject(`HTTP ${r.status}`))
      .then(data => { setWatchlists(data); setLoading(false) })
      .catch(e => { setError(String(e)); setLoading(false) })
  }, [])

  function handleCreate() {
    const name = newName.trim()
    if (!name) return
    jsonPost('/api/watchlists', { name })
      .then(r => r.json())
      .then(wl => {
        setWatchlists(prev => [...prev, wl])
        setNewName('')
        setCreating(false)
      })
  }

  function handleDelete(id) {
    api(`/api/watchlists/${id}`, { method: 'DELETE' }).then(r => {
      if (r.ok) {
        setWatchlists(prev => prev.filter(w => w.id !== id))
        if (selected?.id === id) setSelected(null)
      }
    })
  }

  function handleRename(id) {
    const name = renameValue.trim()
    if (!name) return
    jsonPost(`/api/watchlists/${id}`, { name }, 'PATCH')
      .then(r => r.json())
      .then(updated => {
        setWatchlists(prev => prev.map(w => w.id === id ? updated : w))
        if (selected?.id === id) setSelected(updated)
        setRenamingId(null)
      })
  }

  function handleAddItem() {
    const code = addCode.trim().toUpperCase()
    if (!code || !selected) return
    jsonPost(`/api/watchlists/${selected.id}/items`, { market: addMarket, code })
      .then(r => {
        if (r.status === 409) return null
        return r.json()
      })
      .then(item => {
        if (!item) return
        const updated = { ...selected, items: [...selected.items, item] }
        setSelected(updated)
        setWatchlists(prev => prev.map(w => w.id === selected.id ? updated : w))
        setAddCode('')
      })
  }

  function handleRemoveItem(itemId) {
    api(`/api/watchlists/${selected.id}/items/${itemId}`, { method: 'DELETE' }).then(r => {
      if (r.ok) {
        const updated = { ...selected, items: selected.items.filter(i => i.id !== itemId) }
        setSelected(updated)
        setWatchlists(prev => prev.map(w => w.id === selected.id ? updated : w))
      }
    })
  }

  if (loading) return <p>Loading…</p>
  if (error) return <p className="error">Error: {error}</p>

  if (selected) {
    return (
      <>
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.25rem' }}>
          <button className="back-btn" style={{ marginBottom: 0 }} onClick={() => setSelected(null)}>← Back</button>
          {renamingId === selected.id ? (
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <input
                className="watchlist-input"
                value={renameValue}
                onChange={e => setRenameValue(e.target.value)}
                onKeyDown={e => { if (e.key === 'Enter') handleRename(selected.id); if (e.key === 'Escape') setRenamingId(null) }}
                autoFocus
              />
              <button className="wl-btn wl-btn--primary" onClick={() => handleRename(selected.id)}>Save</button>
              <button className="wl-btn" onClick={() => setRenamingId(null)}>Cancel</button>
            </div>
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <h1 style={{ margin: 0 }}>{selected.name}</h1>
              <button className="wl-btn" onClick={() => { setRenamingId(selected.id); setRenameValue(selected.name) }}>Rename</button>
            </div>
          )}
        </div>

        <div className="watchlist-add-row">
          <select value={addMarket} onChange={e => setAddMarket(e.target.value)} className="tenor-select">
            <option value="ASX">ASX</option>
          </select>
          <input
            className="watchlist-input"
            placeholder="Code"
            value={addCode}
            onChange={e => setAddCode(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') handleAddItem() }}
          />
          <button className="wl-btn wl-btn--primary" onClick={handleAddItem}>Add</button>
        </div>

        {selected.items.length === 0 ? (
          <p className="empty">No securities in this watchlist yet.</p>
        ) : (
          <table className="trades-table">
            <thead>
              <tr>
                <th>Market</th>
                <th>Code</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {selected.items.map(item => (
                <tr key={item.id}>
                  <td>{item.market}</td>
                  <td>{item.code}</td>
                  <td>
                    <button className="wl-btn wl-btn--danger" onClick={() => handleRemoveItem(item.id)}>Remove</button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </>
    )
  }

  return (
    <>
      <div style={{ display: 'flex', alignItems: 'center', gap: '1rem', marginBottom: '1.25rem' }}>
        <h1 style={{ margin: 0 }}>Watchlists</h1>
        <button className="wl-btn wl-btn--primary" onClick={() => { setCreating(true); setNewName('') }}>+ New</button>
      </div>

      {creating && (
        <div className="watchlist-add-row" style={{ marginBottom: '1rem' }}>
          <input
            className="watchlist-input"
            placeholder="Watchlist name"
            value={newName}
            onChange={e => setNewName(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') handleCreate(); if (e.key === 'Escape') setCreating(false) }}
            autoFocus
          />
          <button className="wl-btn wl-btn--primary" onClick={handleCreate}>Create</button>
          <button className="wl-btn" onClick={() => setCreating(false)}>Cancel</button>
        </div>
      )}

      {watchlists.length === 0 ? (
        <p className="empty">No watchlists yet. Click + New to create one.</p>
      ) : (
        <table className="trades-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Securities</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {watchlists.map(wl => (
              <tr key={wl.id}>
                {renamingId === wl.id ? (
                  <td colSpan={2}>
                    <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                      <input
                        className="watchlist-input"
                        value={renameValue}
                        onChange={e => setRenameValue(e.target.value)}
                        onKeyDown={e => { if (e.key === 'Enter') handleRename(wl.id); if (e.key === 'Escape') setRenamingId(null) }}
                        autoFocus
                      />
                      <button className="wl-btn wl-btn--primary" onClick={() => handleRename(wl.id)}>Save</button>
                      <button className="wl-btn" onClick={() => setRenamingId(null)}>Cancel</button>
                    </div>
                  </td>
                ) : (
                  <>
                    <td className="code-link" onClick={() => setSelected(wl)}>{wl.name}</td>
                    <td>{wl.items?.length ?? 0}</td>
                  </>
                )}
                <td>
                  {renamingId !== wl.id && (
                    <div style={{ display: 'flex', gap: '0.5rem' }}>
                      <button className="wl-btn" onClick={() => { setRenamingId(wl.id); setRenameValue(wl.name) }}>Rename</button>
                      <button className="wl-btn wl-btn--danger" onClick={() => handleDelete(wl.id)}>Delete</button>
                    </div>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  )
}
