import { useEffect, useState } from 'react'
import { Alert, Card, CardContent, Chip, Grid, Stack, Typography } from '@mui/material'
import PeopleOutlined from '@mui/icons-material/PeopleOutlined'
import SensorDoorOutlined from '@mui/icons-material/SensorDoorOutlined'
import ReceiptLongOutlined from '@mui/icons-material/ReceiptLongOutlined'
import { api, errorMessage } from '../api'
import type { AccessEvent, Device, UserRecord } from '../types'

export default function DashboardPage() {
  const [users, setUsers] = useState<UserRecord[]>([]), [events, setEvents] = useState<AccessEvent[]>([]), [devices, setDevices] = useState<Device[]>([]), [error, setError] = useState('')
  useEffect(() => { Promise.all([api.get('/api/users'), api.get('/api/access-events'), api.get('/api/devices')]).then(([u,e,d]) => { setUsers(u.data); setEvents(e.data); setDevices(d.data) }).catch(err => setError(errorMessage(err))) }, [])
  const cards = [
    ['Registered users', users.length, <PeopleOutlined color="primary" />],
    ['Active users', users.filter(u => u.status === 'ACTIVE').length, <PeopleOutlined color="success" />],
    ['Recent attempts', events.length, <ReceiptLongOutlined color="primary" />],
    ['Door actuator', devices.some(d => d.type !== 'TERMINAL' && d.online) ? 'Online' : 'Offline', <SensorDoorOutlined color={devices.some(d => d.online) ? 'success' : 'disabled'} />],
  ] as const
  return <Stack spacing={3}>
    <div><Typography variant="h4" fontWeight={800}>System overview</Typography><Typography color="text.secondary">Live health and access activity for DOOR-01.</Typography></div>
    {error && <Alert severity="error">{error}</Alert>}
    <Grid container spacing={2}>{cards.map(([label,value,icon]) => <Grid size={{ xs: 12, sm: 6, lg: 3 }} key={label}><Card><CardContent><Stack direction="row" justifyContent="space-between">{icon}<Chip label="Live" size="small" /></Stack><Typography variant="h4" fontWeight={800} mt={2}>{value}</Typography><Typography color="text.secondary">{label}</Typography></CardContent></Card></Grid>)}</Grid>
    <Card><CardContent><Typography variant="h6" fontWeight={700} gutterBottom>Latest access decisions</Typography><Stack spacing={1.2}>{events.slice(0,6).map(event => <Stack key={event.id} direction="row" justifyContent="space-between" alignItems="center"><div><Typography fontWeight={600}>{event.userName || 'Unknown credential'}</Typography><Typography variant="body2" color="text.secondary">{new Date(event.occurredAt).toLocaleString()} · {event.doorPublicId || 'Unknown door'}</Typography></div><Chip color={event.resultCode === 'GRANTED' ? 'success' : 'error'} label={event.resultCode} /></Stack>)}{events.length === 0 && <Typography color="text.secondary">No access attempts yet.</Typography>}</Stack></CardContent></Card>
  </Stack>
}

