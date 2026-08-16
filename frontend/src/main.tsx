import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material'
import App from './App'
import './styles.css'

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#075985' },
    secondary: { main: '#0f766e' },
    background: { default: '#f4f7f9' },
  },
  shape: { borderRadius: 12 },
  typography: { fontFamily: 'Inter, ui-sans-serif, system-ui, -apple-system, sans-serif' },
})

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}><CssBaseline /><BrowserRouter><App /></BrowserRouter></ThemeProvider>
  </React.StrictMode>,
)

