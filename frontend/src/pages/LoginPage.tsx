import { FormEvent, useState } from 'react'
import { Alert, Box, Button, Card, CardContent, CircularProgress, Stack, TextField, Typography } from '@mui/material'
import SensorDoorOutlined from '@mui/icons-material/SensorDoorOutlined'
import { api, ensureCsrf, errorMessage } from '../api'

export default function LoginPage({ onAuthenticated }: { onAuthenticated: () => void }) {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setBusy(true); setError('')
    try { await ensureCsrf(); await api.post('/api/auth/login', { username, password }); onAuthenticated() }
    catch (err) { setError(errorMessage(err)) } finally { setBusy(false) }
  }
  return <Box minHeight="100vh" display="grid" sx={{ placeItems: 'center', background: 'radial-gradient(circle at top right,#bae6fd,#f8fafc 48%)' }}>
    <Card sx={{ width: 420, maxWidth: 'calc(100vw - 32px)', boxShadow: 8 }}><CardContent sx={{ p: 4 }}>
      <Stack component="form" spacing={2.5} onSubmit={submit}>
        <Box><SensorDoorOutlined color="primary" sx={{ fontSize: 44 }} /><Typography variant="h4" fontWeight={800}>SmartDoor</Typography><Typography color="text.secondary">Administrator sign in</Typography></Box>
        {error && <Alert severity="error">{error}</Alert>}
        <TextField label="Username" value={username} onChange={e => setUsername(e.target.value)} required autoFocus />
        <TextField label="Password" type="password" value={password} onChange={e => setPassword(e.target.value)} required />
        <Button type="submit" variant="contained" size="large" disabled={busy}>{busy ? <CircularProgress size={24} /> : 'Sign in'}</Button>
      </Stack>
    </CardContent></Card>
  </Box>
}
