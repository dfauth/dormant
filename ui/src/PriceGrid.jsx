import { useState, useEffect, useRef } from 'react'
import { ReactGrid } from '@silevis/reactgrid'
import { Parser as FormulaParser } from 'hot-formula-parser'
import '@silevis/reactgrid/styles.css'

const initialColumns = Array.from({ length: 26 }, (_, i) => ({
  columnId: String.fromCharCode(65 + i),
  width: 80,
  resizable: true,
}))

const headerRow = {
  rowId: 'header',
  height: 20,
  cells: initialColumns.map(col => ({ type: 'header', text: col.columnId })),
}

const ROW_HEIGHT = 12

const initialRows = Array.from({ length: 1000 }, (_, i) => ({
  rowId: i,
  height: ROW_HEIGHT,
  cells: initialColumns.map(() => ({ type: 'text', text: '' })),
}))

export default function PriceGrid() {
  const [columns, setColumns] = useState(initialColumns)
  const [rows, setRows] = useState(initialRows)
  const [focusedCoord, setFocusedCoord] = useState(null)
  const [formulaBar, setFormulaBar] = useState('')
  const cellMapRef = useRef({})   // coord → { formula, value }
  const dependenciesRef = useRef({})
  const reverseDepsRef = useRef({})
  const rowsRef = useRef(rows)
  const collectingRefsRef = useRef(null)  // non-null while collecting deps

  // =fetch("/api/positions/open","a => a.map(b => b.code)")
  // =fetch(concat("/api/ta/ema/ASX:",A1), "a => a.payload.8.price")

  useEffect(() => { rowsRef.current = rows }, [rows])

  const parser = useRef(new FormulaParser())

  useEffect(() => {
    parser.current.on('callCellValue', (cellCoord, done) => {
      const key = `${cellCoord.column.label}${cellCoord.row.label}`
      if (collectingRefsRef.current) collectingRefsRef.current.push(key)
      done(cellMapRef.current[key]?.value ?? null)
    })

    parser.current.setFunction('concat', async (params) => {
      return params.join("")
    })

    parser.current.setFunction('fetch', async (params) => {
      const res = blah(params[0], url => fetch(url, { credentials: 'include' }))
      const lambdaStr = params[1]
      const data = res.then(r => r.json())
      const transformed = lambdaStr ? data.then(d => Function(`return (${lambdaStr})`)()(d)) : data
      return await transformed.then(t => {
          if (Array.isArray(t)) return t.map((obj) =>
            typeof obj === 'object' && obj !== null ? Object.values(obj) : [obj]
          )
          return [[JSON.stringify(t)]]
      })
    })
  }, [])

  useEffect(() => {
    fetch('/api/settings/reactgrid', { credentials: 'include' })
      .then(res => res.ok ? res.json() : null)
      .then(data => { if (data?.rows) setRows(data.rows) })
      .catch(() => {})
  }, [])

  const coordToIndex = (coord) => {
    const col = coord.charCodeAt(0) - 65
    const row = parseInt(coord.substring(1), 10) - 1
    return [row, col]
  }

  const evaluateCell = async (coord, formula) => {
    if (!formula.startsWith('=')) return formula
    const result = parser.current.parse(formula.substring(1))
    if (result.error) return result.error
    return result.result instanceof Promise ? await result.result : result.result
  }

  const displayValue = (formula, val) => {
    if (!formula.startsWith('=')) return formula
    if (Array.isArray(val)) {
      const first = val[0]
      return Array.isArray(first) ? String(first[0] ?? '') : String(first ?? '')
    }
    return val == null ? '' : String(val)
  }

  const updateDependencies = (coord, formula) => {
    const deps = reverseDepsRef.current
    // Clear old reverse deps for this coord
    Object.keys(deps).forEach(dep => {
      deps[dep] = deps[dep].filter(c => c !== coord)
    })
    // Collect refs by parsing — callCellValue fires for each referenced cell
    collectingRefsRef.current = []
    parser.current.parse(formula.substring(1))
    const refs = collectingRefsRef.current
    collectingRefsRef.current = null

    dependenciesRef.current[coord] = refs
    refs.forEach(ref => {
      deps[ref] = [...(deps[ref] || []), coord]
    })
  }

  const spillArrayIntoGrid = (startRow, startCol, array2D, sourceCoord, currentRows) => {
    const newRows = currentRows.map(r => ({ ...r, cells: [...r.cells] }))
    array2D.forEach((rowVals, rIdx) => {
      rowVals.forEach((val, cIdx) => {
        if (rIdx === 0 && cIdx === 0) {
          if (newRows[startRow]?.cells[startCol]) {
            newRows[startRow].cells[startCol].value = val
          }
        } else {
          const targetRow = startRow + rIdx
          const targetCol = startCol + cIdx
          if (newRows[targetRow]?.cells[targetCol]) {
            newRows[targetRow].cells[targetCol] = { type: 'text', text: String(val), spillSource: sourceCoord }
          }
        }
      })
    })
    return newRows
  }

  // Apply a formula+value to a coord: updates cellMap and sets display text in rows.
  // Also spills arrays and recalculates dependents.
  const applyChange = async (coord, formula, currentRows) => {
    const val = await evaluateCell(coord, formula)
    cellMapRef.current[coord] = { formula, value: val }

    if (formula.startsWith('=')) updateDependencies(coord, formula)

    // Set display value in source cell
    const [rowIdx, colIdx] = coordToIndex(coord)
    if (currentRows[rowIdx]?.cells[colIdx]) {
      currentRows[rowIdx].cells[colIdx] = { type: 'text', text: formula, value: val }
    }

    // Spill array results into adjacent cells
    if (Array.isArray(val)) {
      currentRows = spillArrayIntoGrid(rowIdx, colIdx, val, coord, currentRows)
    }

    // Recalculate cells that depend on this one
    for (const dep of (reverseDepsRef.current[coord] || [])) {
      const depCell = cellMapRef.current[dep]
      if (depCell?.formula.startsWith('=')) {
        currentRows = await applyChange(dep, depCell.formula, currentRows)
      }
    }

    return currentRows
  }

  const handleChanges = async (changes) => {
    let newRows = rowsRef.current.map(r => ({ ...r, cells: [...r.cells] }))
    for (const change of changes) {
      const { rowId, columnId, newCell } = change
      const coord = `${columnId}${rowId + 1}`
      newRows = await applyChange(coord, newCell.text, newRows)
    }
    setRows(newRows)
    persist(newRows)
  }

  const handleFormulaBarKey = async (e) => {
    if (e.key !== 'Enter' || !focusedCoord) return
    let newRows = rowsRef.current.map(r => ({ ...r, cells: [...r.cells] }))
    newRows = await applyChange(focusedCoord, formulaBar, newRows)
    setRows(newRows)
    persist(newRows)
  }

  const handleFocusChanged = ({ rowId, columnId }) => {
    if (rowId === 'header') return
    const coord = `${columnId}${rowId + 1}`
    setFocusedCoord(coord)
    setFormulaBar(cellMapRef.current[coord]?.formula ?? '')
    console.log("rowsRef["+rowId+"].cells["+columnId+"]: "+JSON.stringify(rowsRef.current[rowId].cells[columnId.codePointAt(0)-"A".codePointAt(0)]))
  }

  const persist = (newRows) => {
    fetch('/api/settings/reactgrid', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ rows: newRows }),
    }).catch(() => {})
  }

  const blah = (param, fn) => {
      if(typeof param === "string") {
          return Promise.resolve(fn(param));
      } else if(param instanceof Promise) {
          return param.then(fn)
      } else {
          throw new Error("Unexpected type: "+typeof(param))
      }
  }

  const ValueCellRenderer = ({ cell }) => {
    return (
      <div style={{ padding: '4px' }}>
        {cell.value ?? cell.text}
      </div>
    );
  }

  return (
    <>
      <h4>scratchpad</h4>
      <div style={{ marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px' }}>
        <span style={{ color: '#6b7280', fontWeight: 600 }}>{focusedCoord ?? ''}</span>
        <input
          value={formulaBar}
          onChange={e => setFormulaBar(e.target.value)}
          onKeyDown={handleFormulaBarKey}
          placeholder="Select a cell — press Enter to apply"
          style={{ width: '400px', fontSize: '12px', padding: '2px 4px' }}
        />
      </div>
      <div style={{ display: 'inline-block', fontSize: '12px' }}>
        <ReactGrid
          rows={[headerRow, ...rows]}
          columns={columns}
          onCellsChanged={handleChanges}
          customCellTemplates={[
              { type: 'text', component: ValueCellRenderer }
            ]}
          onFocusLocationChanged={handleFocusChanged}
          onColumnResized={(columnId, width) =>
            setColumns(cols => cols.map(c => c.columnId === columnId ? { ...c, width } : c))
          }
          enableColumnResizeOnAllHeaders
          stickyTopRows={1}
          enableRangeSelection
        />
      </div>
    </>
  )
}
