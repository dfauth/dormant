import { createRoot } from 'react-dom/client'
import App from './App.jsx'

// StrictMode intentionally omitted: UniverJS registers components in a global
// registry that its dispose() does not fully clear, so StrictMode's
// dev-only double-mount causes "Component already exists" errors.
createRoot(document.getElementById('root')).render(<App />)
