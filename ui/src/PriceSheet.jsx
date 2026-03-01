import { useEffect, useRef, useState } from 'react'
import { createUniver, defaultTheme, LocaleType, merge } from '@univerjs/presets'
import { UniverSheetsCorePreset } from '@univerjs/preset-sheets-core'
import SheetsLocaleEnUS from '@univerjs/preset-sheets-core/locales/en-US'
import '@univerjs/presets/lib/styles/preset-sheets-core.css'
import '@univerjs/preset-sheets-core/lib/index.css'
import { AsyncCustomFunction, IFunctionService } from '@univerjs/engine-formula'
import { JSONPath } from 'jsonpath-plus'

// ---------------------------------------------------------------------------
// Custom spreadsheet functions
//
// Each class extends AsyncCustomFunction. Return a plain JS value (number,
// string, boolean, or a 2-D array) from calculateCustom — no wrapper needed.
// Add as many classes as you like and include them in registerCustomFunctions.
// ---------------------------------------------------------------------------

// Shared cache: full URL string (including query params) → raw parsed JSON.
// Populated by FETCH; read by CACHE.
const fetchCache = new Map()

// Convert parsed JSON to a 2-D array suitable for UniverJS to spill:
//   • Array of objects  → header row + one data row per object
//   • Plain object      → two-column [key, value] table
//   • Array of scalars  → single column, one value per row
//   • Scalar / text     → single cell (returned as-is)
function toSpreadsheetValue(data) {
  if (Array.isArray(data) && data.length > 0 && typeof data[0] === 'object' && data[0] !== null) {
    const keys = Object.keys(data[0])
    return [keys, ...data.map(row => keys.map(k => row[k] ?? ''))]
  }
  if (typeof data === 'object' && data !== null && !Array.isArray(data)) {
    return Object.entries(data).map(([k, v]) => [k, typeof v === 'object' ? JSON.stringify(v) : v])
  }
  if (Array.isArray(data)) {
    return data.map(v => [v])
  }
  return data
}

function applyJsonPath(data, jsonPathArg) {
  if (jsonPathArg == null) return data
  const expr = typeof jsonPathArg?.getValue === 'function' ? String(jsonPathArg.getValue()) : String(jsonPathArg)
  return expr ? JSONPath({ path: expr, json: data }) : data
}

// Apply a mustache-style template to each element of an array of objects.
// e.g. template "{code}:{market}" applied to {code:"BHP",market:"ASX"} → "BHP:ASX"
// Non-array data is returned unchanged.
function applyTemplate(data, templateArg) {
  if (templateArg == null) return data
  const template = typeof templateArg?.getValue === 'function' ? String(templateArg.getValue()) : String(templateArg)
  if (!template || !Array.isArray(data)) return data
  return data.map(item => template.replace(/\{(\w+)\}/g, (_, key) => item?.[key] ?? ''))
}

class DoubleFunction extends AsyncCustomFunction {
  constructor() {
    super('DOUBLE')
    this.minParams = 1
    this.maxParams = 1
  }

  calculateCustom(arg) {
    const n = typeof arg?.getValue === 'function' ? Number(arg.getValue()) : Number(arg)
    return n * 2
  }
}

// =FETCH(url [, jsonPath [, template]]) — calls a backend endpoint and spills
// the result into the sheet.  The raw JSON response is cached under the URL
// key so that =CACHE(url …) can retrieve it without another network round-trip.
//
// Arguments:
//   url      — backend URL including any query-string parameters
//   jsonPath — (optional) JSONPath expression to narrow the response
//   template — (optional) mustache-style template applied to each object after
//              the JSONPath step, producing a single derived column.
//              Use {fieldName} placeholders, e.g. "{code}:{market}".
//
// Examples:
//   =FETCH("/api/prices/BHP")                         — spills entire response
//   =FETCH("/api/prices/BHP","$[*].close")             — single column of close prices
//   =FETCH("/api/prices","$[*]","{code}:{market}")     — derived "BHP:ASX" column
//
// NOTE: ValueObjectFactory.create() interprets any string containing "{...}"
// as a spreadsheet array literal, which causes #SPILL!.  Returning a real
// PrimitiveValueType[][] bypasses that path entirely.
class FetchFunction extends AsyncCustomFunction {
  constructor() {
    super('FETCH')
    this.minParams = 1
    this.maxParams = 3
  }

  async calculateCustom(urlArg, jsonPathArg, templateArg) {
    const url = typeof urlArg?.getValue === 'function' ? String(urlArg.getValue()) : String(urlArg)
    const res = await fetch(url, { credentials: 'include' })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const text = await res.text()

    let data
    try { data = JSON.parse(text) } catch { return text }

    // Cache raw JSON (before JSONPath) so CACHE() can apply its own path.
    fetchCache.set(url, data)

    return toSpreadsheetValue(applyTemplate(applyJsonPath(data, jsonPathArg), templateArg))
  }
}

// =CACHE(key [, jsonPath [, template]]) — reads previously fetched JSON from
// the in-memory cache without making a network request.  The key must match a
// URL previously loaded by =FETCH (including any query-string parameters).
//
// Arguments mirror =FETCH's second and third parameters exactly, so you can
// apply a different projection or template to the same cached payload:
//   =CACHE("/api/prices")                         — full cached response
//   =CACHE("/api/prices","$[*].close")            — only the close column
//   =CACHE("/api/prices","$[*]","{code}:{market}") — derived column from cache
class CacheFunction extends AsyncCustomFunction {
  constructor() {
    super('CACHE')
    this.minParams = 1
    this.maxParams = 3
  }

  calculateCustom(keyArg, jsonPathArg, templateArg) {
    const key = typeof keyArg?.getValue === 'function' ? String(keyArg.getValue()) : String(keyArg)
    const data = fetchCache.get(key)
    if (data === undefined) return `#CACHE! Key not found: ${key}`
    return toSpreadsheetValue(applyTemplate(applyJsonPath(data, jsonPathArg), templateArg))
  }
}

function registerCustomFunctions(univer) {
  const functionService = univer.__getInjector().get(IFunctionService)
  functionService.registerExecutors(
    new DoubleFunction(),
    new FetchFunction(),
    new CacheFunction(),
  )
}

const SETTINGS_KEY = 'SPREADSHEET_CONFIG'
const SAVE_DEBOUNCE_MS = 1500

async function loadSpreadsheetConfig() {
  try {
    const res = await fetch(`/api/settings/${SETTINGS_KEY}`, { credentials: 'include' })
    if (res.ok) return await res.json()
  } catch {
    // network error — proceed with default
  }
  return null
}

async function saveSpreadsheetConfig(snapshot) {
  await fetch(`/api/settings/${SETTINGS_KEY}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify(snapshot),
  })
}

export default function PriceSheet() {
  const univerRef = useRef(null)
  const anchorRef = useRef(null)
  const [saveStatus, setSaveStatus] = useState(null) // null | 'saving' | 'saved'

  useEffect(() => {
    let cancelled = false
    const disposables = []
    let saveTimer = null
    let savedTimer = null

    // Create the container ourselves outside React's management so React
    // reconciliation can never touch the children UniverJS adds to it.
    const container = document.createElement('div')
    container.className = 'sheet-container'
    anchorRef.current.insertAdjacentElement('afterend', container)

    let univerAPI = null

    // Debounced persist: exports the workbook snapshot and POSTs to user settings
    const persist = () => {
      clearTimeout(saveTimer)
      clearTimeout(savedTimer)
      if (!cancelled) setSaveStatus('saving')

      saveTimer = setTimeout(async () => {
        const workbook = univerAPI?.getActiveWorkbook()
        if (!workbook || cancelled) return
        try {
          await saveSpreadsheetConfig(workbook.save())
          if (!cancelled) {
            setSaveStatus('saved')
            savedTimer = setTimeout(() => {
              if (!cancelled) setSaveStatus(null)
            }, 2000)
          }
        } catch (err) {
          console.error('Failed to persist spreadsheet config:', err)
          if (!cancelled) setSaveStatus(null)
        }
      }, SAVE_DEBOUNCE_MS)
    }

    const init = async () => {
      // Load saved config before creating the workbook so we can restore it
      const savedConfig = await loadSpreadsheetConfig()
      if (cancelled) return

      const { univerAPI: api, univer } = createUniver({
        locale: LocaleType.EN_US,
        locales: { [LocaleType.EN_US]: merge({}, SheetsLocaleEnUS) },
        theme: defaultTheme,
        presets: [UniverSheetsCorePreset({ container })],
      })
      univerAPI = api
      univerRef.current = univerAPI

      registerCustomFunctions(univer)

      // Restore from saved snapshot, or create a blank workbook
      univerAPI.createWorkbook(savedConfig ?? { name: 'Prices' })

      // Listen for any sheet mutation (value edits, structural changes, formatting…)
      // after a short settling delay so workbook initialisation doesn't trigger a save.
      const listenTimer = setTimeout(() => {
        if (cancelled) return
        disposables.push(
          univerAPI.addEvent(univerAPI.Event.CommandExecuted, ({ id }) => {
            if (typeof id === 'string' && id.startsWith('sheet.mutation.')) {
              persist()
            }
          })
        )
      }, 300)
      disposables.push({ dispose: () => clearTimeout(listenTimer) })
    }

    init()

    return () => {
      cancelled = true
      clearTimeout(saveTimer)
      clearTimeout(savedTimer)
      disposables.forEach(d => d.dispose())
      if (univerAPI) univerAPI.dispose()
      container.remove()
      univerRef.current = null
    }
  }, [])

  return (
    <>
      <h1>Prices</h1>
      <div className="sheet-toolbar">
        {saveStatus === 'saving' && <span className="sheet-save-status">Saving…</span>}
        {saveStatus === 'saved' && <span className="sheet-save-status sheet-save-status--saved">Saved</span>}
      </div>
      {/* anchor: UniverJS container is inserted after this div by useEffect */}
      <div ref={anchorRef} style={{ display: 'none' }} />
    </>
  )
}
