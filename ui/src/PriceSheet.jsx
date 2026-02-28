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

// =FETCH(url [, jsonPath]) — calls a backend endpoint and spills the result
// into the sheet.
//
// The optional second argument is a JSONPath expression applied to the parsed
// JSON before the result is returned.  Examples:
//   =FETCH("/api/prices/BHP")                    — spills entire response
//   =FETCH("/api/prices/BHP","$[*].close")        — single column of close prices
//   =FETCH("/api/account","$.summary")            — object → two-column table
//
// The response is converted to a 2-D array so UniverJS can spill it cleanly:
//   • JSON array of objects  → header row + one data row per object
//   • JSON object            → two-column table of [key, value] pairs
//   • JSON primitive         → single cell
//   • Non-JSON text          → single cell
//
// NOTE: ValueObjectFactory.create() interprets any string containing "{...}"
// as a spreadsheet array literal, which causes #SPILL!.  Returning a real
// PrimitiveValueType[][] bypasses that path entirely.
class FetchFunction extends AsyncCustomFunction {
  constructor() {
    super('FETCH')
    this.minParams = 1
    this.maxParams = 2
  }

  async calculateCustom(urlArg, jsonPathArg) {
    const url = typeof urlArg?.getValue === 'function' ? String(urlArg.getValue()) : String(urlArg)
    const res = await fetch(url, { credentials: 'include' })
    if (!res.ok) throw new Error(`HTTP ${res.status}`)
    const text = await res.text()

    let data
    try { data = JSON.parse(text) } catch { return text }

    // Apply JSONPath filter if provided
    if (jsonPathArg != null) {
      const expr = typeof jsonPathArg?.getValue === 'function' ? String(jsonPathArg.getValue()) : String(jsonPathArg)
      if (expr) {
        data = JSONPath({ path: expr, json: data })
      }
    }

    // JSON array of objects → header row + data rows
    if (Array.isArray(data) && data.length > 0 && typeof data[0] === 'object' && data[0] !== null) {
      const keys = Object.keys(data[0])
      return [keys, ...data.map(row => keys.map(k => row[k] ?? ''))]
    }

    // JSON object → two-column key/value table
    if (typeof data === 'object' && data !== null && !Array.isArray(data)) {
      return Object.entries(data).map(([k, v]) => [k, typeof v === 'object' ? JSON.stringify(v) : v])
    }

    // JSON primitive or simple array
    return data
  }
}

function registerCustomFunctions(univer) {
  const functionService = univer.__getInjector().get(IFunctionService)
  functionService.registerExecutors(
    new DoubleFunction(),
    new FetchFunction(),
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
