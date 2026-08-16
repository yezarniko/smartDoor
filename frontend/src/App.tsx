import { useEffect, useState } from 'react'
import { Navigate, Route, Routes } from 'react-router-dom'
import { Box, CircularProgress } from '@mui/material'
import { api, ensureCsrf } from './api'
import AppShell from './layout/AppShell'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import UsersPage from './pages/UsersPage'
import UserDetailPage from './pages/UserDetailPage'
import ScannerPage from './pages/ScannerPage'
import AccessLogsPage from './pages/AccessLogsPage'
import DoorStatusPage from './pages/DoorStatusPage'
import ModelPage from './pages/ModelPage'

export default function App() {
  const [session, setSession] = useState<'loading' | 'authenticated' | 'anonymous'>('loading')

  const refresh = async () => {
    try {
      await ensureCsrf()
      await api.get('/api/auth/session')
      setSession('authenticated')
    } catch { setSession('anonymous') }
  }

  useEffect(() => { void refresh() }, [])

  if (session === 'loading') return <Box minHeight="100vh" display="grid" sx={{ placeItems: 'center' }}><CircularProgress /></Box>
  if (session === 'anonymous') return <LoginPage onAuthenticated={() => setSession('authenticated')} />

  return (
    <AppShell onLoggedOut={() => setSession('anonymous')}>
      <Routes>
        <Route path="/" element={<DashboardPage />} />
        <Route path="/users" element={<UsersPage />} />
        <Route path="/users/:id" element={<UserDetailPage />} />
        <Route path="/scanner/door-01" element={<ScannerPage />} />
        <Route path="/access-logs" element={<AccessLogsPage />} />
        <Route path="/door-status" element={<DoorStatusPage />} />
        <Route path="/model" element={<ModelPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </AppShell>
  )
}

