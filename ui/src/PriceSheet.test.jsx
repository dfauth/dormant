import { describe, it, expect, vi, beforeAll, afterAll, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, act, cleanup } from '@testing-library/react'
import { setupServer } from 'msw/node'
import { http, HttpResponse } from 'msw'

// ---------------------------------------------------------------------------
// UniverJS mocks
//
// vi.mock() calls are hoisted to the top of the module before any variable
// initialisations, so we use vi.hoisted() to create the shared mock state and
// objects that the factory functions can safely close over.
// ---------------------------------------------------------------------------
const {
  mockRange,
  mockSheet,
  mockWorkbook,
  mockAPI,
  state,
} = vi.hoisted(() => {
  const state = {
    commandHandler: null,
    workbookCreatedWith: null,
  }

  const mockRange = {
    getFormulas: vi.fn(() => [['']]),
    setValues: vi.fn(),
  }

  const mockSheet = {
    getRange: vi.fn(() => mockRange),
    clear: vi.fn(),
  }

  const mockWorkbook = {
    getActiveSheet: vi.fn(() => mockSheet),
    save: vi.fn(() => ({ name: 'Prices', sheets: {} })),
  }

  const mockDisposable = { dispose: vi.fn() }

  const mockAPI = {
    createWorkbook: vi.fn(data => { state.workbookCreatedWith = data }),
    getActiveWorkbook: vi.fn(() => mockWorkbook),
    addEvent: vi.fn((event, handler) => {
      if (event === 'CommandExecuted') state.commandHandler = handler
      return mockDisposable
    }),
    dispose: vi.fn(),
    Event: { CommandExecuted: 'CommandExecuted' },
  }

  return { mockRange, mockSheet, mockWorkbook, mockAPI, state }
})

vi.mock('@univerjs/presets', () => ({
  createUniver: vi.fn(() => ({ univerAPI: mockAPI })),
  defaultTheme: {},
  LocaleType: { EN_US: 'en-US' },
  merge: vi.fn((a, b) => ({ ...a, ...b })),
}))

vi.mock('@univerjs/preset-sheets-core', () => ({
  UniverSheetsCorePreset: vi.fn(() => ({})),
}))

vi.mock('@univerjs/preset-sheets-core/locales/en-US', () => ({ default: {} }))

// ---------------------------------------------------------------------------
// MSW server
// ---------------------------------------------------------------------------
const MOCK_SNAPSHOT = { name: 'Prices', unitId: 'saved-1', sheets: { s1: {} } }

let lastSavedBody = null

const server = setupServer(
  http.get('/api/settings/SPREADSHEET_CONFIG', () =>
    HttpResponse.json(MOCK_SNAPSHOT)
  ),
  http.post('/api/settings/SPREADSHEET_CONFIG', async ({ request }) => {
    lastSavedBody = await request.json()
    return HttpResponse.json({})
  }),
  http.get('/api/prices/:code', () =>
    HttpResponse.json([
      { date: '2024-01-01', open: 30, high: 32, low: 29, close: 31, volume: 100_000 },
    ])
  ),
)

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

// Advance fake timers far enough for: the fetch microtask to settle, createUniver
// to run, createWorkbook to be called, and the 300 ms listener-setup delay to fire.
async function renderAndInit() {
  const result = render(<PriceSheet />)
  await vi.advanceTimersByTimeAsync(500)
  return result
}

function fireSheetMutation(id = 'sheet.mutation.set-range-values') {
  act(() => state.commandHandler?.({ id }))
}

// ---------------------------------------------------------------------------
// Import component (after mocks are declared)
// ---------------------------------------------------------------------------
// eslint-disable-next-line import/first
import PriceSheet from './PriceSheet'

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------
describe('PriceSheet', () => {
  beforeAll(() => server.listen({ onUnhandledRequest: 'warn' }))
  afterAll(() => server.close())

  beforeEach(() => {
    vi.useFakeTimers()
    state.commandHandler = null
    state.workbookCreatedWith = null
    lastSavedBody = null
    vi.clearAllMocks()
    // Restore implementations cleared by clearAllMocks
    mockSheet.getRange.mockReturnValue(mockRange)
    mockRange.getFormulas.mockReturnValue([['']])
    mockWorkbook.getActiveSheet.mockReturnValue(mockSheet)
    mockWorkbook.save.mockReturnValue({ name: 'Prices', sheets: {} })
    mockAPI.getActiveWorkbook.mockReturnValue(mockWorkbook)
    mockAPI.addEvent.mockImplementation((event, handler) => {
      if (event === 'CommandExecuted') state.commandHandler = handler
      return { dispose: vi.fn() }
    })
    mockAPI.createWorkbook.mockImplementation(data => { state.workbookCreatedWith = data })
  })

  afterEach(async () => {
    cleanup()
    server.resetHandlers()
    await vi.runAllTimersAsync()
    vi.useRealTimers()
  })

  // ── Static render ──────────────────────────────────────────────────────────

  describe('static render', () => {
    it('renders the page heading', () => {
      render(<PriceSheet />)
      expect(screen.getByRole('heading', { name: 'Prices' })).toBeInTheDocument()
    })

    it('renders enabled Fetch and Clear buttons', () => {
      render(<PriceSheet />)
      expect(screen.getByRole('button', { name: 'Fetch' })).toBeEnabled()
      expect(screen.getByRole('button', { name: 'Clear' })).toBeEnabled()
    })

    it('shows no save status indicator initially', () => {
      render(<PriceSheet />)
      expect(screen.queryByText('Saving…')).not.toBeInTheDocument()
      expect(screen.queryByText('Saved')).not.toBeInTheDocument()
    })
  })

  // ── Loading saved config on mount ─────────────────────────────────────────

  describe('loading saved config', () => {
    it('creates the workbook from the saved snapshot when settings returns 200', async () => {
      await renderAndInit()
      expect(mockAPI.createWorkbook).toHaveBeenCalledWith(MOCK_SNAPSHOT)
    })

    it('falls back to a blank workbook when settings returns 404', async () => {
      server.use(
        http.get('/api/settings/SPREADSHEET_CONFIG', () =>
          new HttpResponse(null, { status: 404 })
        )
      )
      await renderAndInit()
      expect(mockAPI.createWorkbook).toHaveBeenCalledWith({ name: 'Prices' })
    })

    it('falls back to a blank workbook on network error', async () => {
      server.use(
        http.get('/api/settings/SPREADSHEET_CONFIG', () => HttpResponse.error())
      )
      await renderAndInit()
      expect(mockAPI.createWorkbook).toHaveBeenCalledWith({ name: 'Prices' })
    })
  })

  // ── CommandExecuted listener registration ─────────────────────────────────

  describe('CommandExecuted listener', () => {
    it('is not registered before the 300 ms settling delay', async () => {
      render(<PriceSheet />)
      await vi.advanceTimersByTimeAsync(50)
      expect(mockAPI.addEvent).not.toHaveBeenCalledWith(
        'CommandExecuted',
        expect.anything()
      )
    })

    it('is registered after the 300 ms settling delay', async () => {
      await renderAndInit()
      expect(mockAPI.addEvent).toHaveBeenCalledWith(
        'CommandExecuted',
        expect.any(Function)
      )
    })

    it('ignores commands that do not start with sheet.mutation.', async () => {
      await renderAndInit()
      fireSheetMutation('sheet.operation.selection-moved')
      await vi.advanceTimersByTimeAsync(2000)
      expect(lastSavedBody).toBeNull()
    })

    it('ignores mutations from other document types', async () => {
      await renderAndInit()
      fireSheetMutation('doc.mutation.set-range-values')
      await vi.advanceTimersByTimeAsync(2000)
      expect(lastSavedBody).toBeNull()
    })
  })

  // ── Debounced persistence ─────────────────────────────────────────────────

  describe('debounced save', () => {
    it('does not save before the 1 500 ms debounce window', async () => {
      await renderAndInit()
      fireSheetMutation()
      await vi.advanceTimersByTimeAsync(1000)
      expect(lastSavedBody).toBeNull()
    })

    it('saves the workbook snapshot after the debounce window', async () => {
      await renderAndInit()
      fireSheetMutation()
      await vi.advanceTimersByTimeAsync(1500)
      expect(lastSavedBody).toMatchObject({ name: 'Prices' })
    })

    it('sends whatever workbook.save() returns as the POST body', async () => {
      const snapshot = { name: 'Prices', sheets: { s1: { data: 'custom' } } }
      mockWorkbook.save.mockReturnValue(snapshot)
      await renderAndInit()
      fireSheetMutation()
      await vi.advanceTimersByTimeAsync(1500)
      expect(lastSavedBody).toEqual(snapshot)
    })

    it('collapses rapid-fire mutations into a single save', async () => {
      let saveCount = 0
      server.use(
        http.post('/api/settings/SPREADSHEET_CONFIG', () => {
          saveCount++
          return HttpResponse.json({})
        })
      )
      await renderAndInit()
      for (let i = 0; i < 8; i++) fireSheetMutation()
      await vi.advanceTimersByTimeAsync(1500)
      expect(saveCount).toBe(1)
    })

    it('resets the debounce timer on each new mutation', async () => {
      await renderAndInit()
      fireSheetMutation()
      await vi.advanceTimersByTimeAsync(1000)   // partway through debounce
      fireSheetMutation()                        // reset the timer
      await vi.advanceTimersByTimeAsync(1000)   // still inside the new window
      expect(lastSavedBody).toBeNull()
      await vi.advanceTimersByTimeAsync(500)    // now we cross the 1 500 ms threshold
      expect(lastSavedBody).not.toBeNull()
    })
  })

  // ── Save status indicator ─────────────────────────────────────────────────

  describe('save status indicator', () => {
    it('shows "Saving…" immediately when a mutation fires', async () => {
      await renderAndInit()
      fireSheetMutation()
      expect(screen.getByText('Saving…')).toBeInTheDocument()
    })

    it('shows "Saved" once the POST resolves', async () => {
      await renderAndInit()
      fireSheetMutation()
      await act(async () => { await vi.advanceTimersByTimeAsync(1500) })
      expect(screen.getByText('Saved')).toBeInTheDocument()
    })

    it('clears the status 2 s after showing "Saved"', async () => {
      await renderAndInit()
      fireSheetMutation()
      await vi.advanceTimersByTimeAsync(1500 + 2000)
      expect(screen.queryByText('Saved')).not.toBeInTheDocument()
      expect(screen.queryByText('Saving…')).not.toBeInTheDocument()
    })

    it('shows "Saving…" again when a second mutation arrives during the "Saved" window', async () => {
      await renderAndInit()
      fireSheetMutation()
      await act(async () => { await vi.advanceTimersByTimeAsync(1500) })  // first save completes
      expect(screen.getByText('Saved')).toBeInTheDocument()
      fireSheetMutation()                        // second change while still showing "Saved"
      expect(screen.getByText('Saving…')).toBeInTheDocument()
    })
  })

  // ── Fetch button — parseFormula + buildUrl behaviour ─────────────────────

  describe('Fetch button', () => {
    it('does nothing when the sheet contains no PRICES() formula', async () => {
      mockRange.getFormulas.mockReturnValue([['']])
      await renderAndInit()
      fireEvent.click(screen.getByRole('button', { name: 'Fetch' }))
      await vi.advanceTimersByTimeAsync(100)
      expect(screen.queryByText('Fetching…')).not.toBeInTheDocument()
    })

    it('calls the price API with the correct path and market param', async () => {
      let captured = null
      server.use(
        http.get('/api/prices/:code', ({ request }) => {
          captured = new URL(request.url)
          return HttpResponse.json([])
        })
      )
      mockRange.getFormulas.mockReturnValue([['=PRICES(ASX,BHP)']])
      await renderAndInit()
      fireEvent.click(screen.getByRole('button', { name: 'Fetch' }))
      await vi.advanceTimersByTimeAsync(200)
      expect(captured).not.toBeNull()
      expect(captured.pathname).toBe('/api/prices/BHP')
      expect(captured.searchParams.get('market')).toBe('ASX')
      expect(captured.searchParams.has('tenor')).toBe(false)
    })

    it('appends the tenor query param when provided', async () => {
      let captured = null
      server.use(
        http.get('/api/prices/:code', ({ request }) => {
          captured = new URL(request.url)
          return HttpResponse.json([])
        })
      )
      mockRange.getFormulas.mockReturnValue([['=PRICES(ASX,BHP,1Y)']])
      await renderAndInit()
      fireEvent.click(screen.getByRole('button', { name: 'Fetch' }))
      await vi.advanceTimersByTimeAsync(200)
      expect(captured.searchParams.get('tenor')).toBe('1Y')
    })

    it('is case-insensitive — =prices(asx,bhp,6m) is recognised', async () => {
      let captured = null
      server.use(
        http.get('/api/prices/:code', ({ request }) => {
          captured = new URL(request.url)
          return HttpResponse.json([])
        })
      )
      mockRange.getFormulas.mockReturnValue([['=prices(asx,bhp,6m)']])
      await renderAndInit()
      fireEvent.click(screen.getByRole('button', { name: 'Fetch' }))
      await vi.advanceTimersByTimeAsync(200)
      expect(captured).not.toBeNull()
    })

    it('writes the header row to the sheet', async () => {
      server.use(
        http.get('/api/prices/:code', () =>
          HttpResponse.json([
            { date: '2024-01-01', open: 30, high: 32, low: 29, close: 31, volume: 100000 },
          ])
        )
      )
      mockRange.getFormulas.mockReturnValue([['=PRICES(ASX,BHP)']])
      await renderAndInit()
      fireEvent.click(screen.getByRole('button', { name: 'Fetch' }))
      await vi.advanceTimersByTimeAsync(200)
      expect(mockRange.setValues).toHaveBeenCalledWith([
        ['Date', 'Open', 'High', 'Low', 'Close', 'Volume'],
      ])
    })

    it('writes the data rows to the sheet', async () => {
      const prices = [
        { date: '2024-01-01', open: 30, high: 32, low: 29, close: 31, volume: 100_000 },
        { date: '2024-01-02', open: 31, high: 33, low: 30, close: 32, volume: 90_000 },
      ]
      server.use(http.get('/api/prices/:code', () => HttpResponse.json(prices)))
      mockRange.getFormulas.mockReturnValue([['=PRICES(ASX,BHP)']])
      await renderAndInit()
      fireEvent.click(screen.getByRole('button', { name: 'Fetch' }))
      await vi.advanceTimersByTimeAsync(200)
      expect(mockRange.setValues).toHaveBeenCalledWith(
        prices.map(p => [p.date, p.open, p.high, p.low, p.close, p.volume])
      )
    })

    it('shows an error message when the price API responds with non-2xx', async () => {
      server.use(
        http.get('/api/prices/:code', () => new HttpResponse(null, { status: 500 }))
      )
      mockRange.getFormulas.mockReturnValue([['=PRICES(ASX,BHP)']])
      await renderAndInit()
      fireEvent.click(screen.getByRole('button', { name: 'Fetch' }))
      await vi.advanceTimersByTimeAsync(200)
      expect(screen.getByText(/Failed to fetch prices: HTTP 500/)).toBeInTheDocument()
    })

    it('shows "Fetching…" label while the request is in-flight', async () => {
      // Never-resolving request so loading stays true
      server.use(http.get('/api/prices/:code', () => new Promise(() => {})))
      mockRange.getFormulas.mockReturnValue([['=PRICES(ASX,BHP)']])
      await renderAndInit()
      fireEvent.click(screen.getByRole('button', { name: 'Fetch' }))
      await vi.advanceTimersByTimeAsync(50)
      expect(screen.getByText('Fetching…')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'Fetching…' })).toBeDisabled()
    })
  })

  // ── Clear button ──────────────────────────────────────────────────────────

  describe('Clear button', () => {
    it('calls sheet.clear() on the active sheet', async () => {
      await renderAndInit()
      fireEvent.click(screen.getByRole('button', { name: 'Clear' }))
      expect(mockSheet.clear).toHaveBeenCalled()
    })

    it('does nothing before the workbook is ready', () => {
      // No renderAndInit — univerRef.current is still null
      render(<PriceSheet />)
      fireEvent.click(screen.getByRole('button', { name: 'Clear' }))
      expect(mockSheet.clear).not.toHaveBeenCalled()
    })
  })

  // ── Cleanup on unmount ────────────────────────────────────────────────────

  describe('cleanup on unmount', () => {
    it('disposes the UniverJS instance', async () => {
      const { unmount } = await renderAndInit()
      unmount()
      expect(mockAPI.dispose).toHaveBeenCalled()
    })

    it('disposes all registered event listeners', async () => {
      const disposeSpy = vi.fn()
      mockAPI.addEvent.mockImplementation((event, handler) => {
        if (event === 'CommandExecuted') state.commandHandler = handler
        return { dispose: disposeSpy }
      })
      const { unmount } = await renderAndInit()
      unmount()
      expect(disposeSpy).toHaveBeenCalled()
    })
  })
})
