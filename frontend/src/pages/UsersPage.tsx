import { FormEvent, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Add, AutoAwesome, Search } from '@mui/icons-material'
import { Alert, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, InputAdornment, InputLabel, MenuItem, Paper, Select, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from '@mui/material'
import { api, errorMessage } from '../api'
import type { UserRecord, UserRole } from '../types'

const emptyForm = { publicId: '', fullName: '', email: '', phone: '', role: 'STAFF' as UserRole }
const generateUserId = () => `USR-${crypto.randomUUID().slice(0, 8).toUpperCase()}`

export default function UsersPage() {
  const [users, setUsers] = useState<UserRecord[]>([]), [query, setQuery] = useState(''), [open, setOpen] = useState(false), [form, setForm] = useState(emptyForm), [error, setError] = useState('')
  const load = () => api.get('/api/users').then(r => setUsers(r.data)).catch(e => setError(errorMessage(e)))
  useEffect(() => { void load() }, [])
  const submit = async (event: FormEvent) => { event.preventDefault(); setError(''); try { await api.post('/api/users', form); setOpen(false); setForm(emptyForm); load() } catch (e) { setError(errorMessage(e)) } }
  const openCreate = () => { setForm({...emptyForm, publicId: generateUserId()}); setOpen(true) }
  const filtered = users.filter(u => `${u.publicId} ${u.fullName} ${u.email || ''}`.toLowerCase().includes(query.toLowerCase()))
  return <Stack spacing={3}>
    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}><div><Typography variant="h4" fontWeight={800}>Users</Typography><Typography color="text.secondary">Create users, assign access, and issue QR credentials.</Typography></div><Button variant="contained" startIcon={<Add />} onClick={openCreate}>Create user</Button></Stack>
    {error && <Alert severity="error" onClose={() => setError('')}>{error}</Alert>}
    <TextField placeholder="Search users" value={query} onChange={e => setQuery(e.target.value)} InputProps={{ startAdornment: <InputAdornment position="start"><Search /></InputAdornment> }} />
    <Paper><Table><TableHead><TableRow><TableCell>User ID</TableCell><TableCell>Name</TableCell><TableCell>Role</TableCell><TableCell>Status</TableCell><TableCell align="right">Manage</TableCell></TableRow></TableHead><TableBody>{filtered.map(user => <TableRow key={user.id} hover><TableCell>{user.publicId}</TableCell><TableCell><Typography fontWeight={650}>{user.fullName}</Typography><Typography variant="body2" color="text.secondary">{user.email || 'No email'}</Typography></TableCell><TableCell>{user.role}</TableCell><TableCell><Chip size="small" color={user.status === 'ACTIVE' ? 'success' : 'default'} label={user.status} /></TableCell><TableCell align="right"><Button component={Link} to={`/users/${user.id}`}>Open</Button></TableCell></TableRow>)}</TableBody></Table></Paper>
    <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm"><Stack component="form" onSubmit={submit}><DialogTitle>Create user</DialogTitle><DialogContent><Stack spacing={2} mt={1}><TextField label="User ID" required value={form.publicId} onChange={e => setForm({...form, publicId:e.target.value})} InputProps={{endAdornment:<InputAdornment position="end"><Button type="button" size="small" startIcon={<AutoAwesome/>} onClick={()=>setForm({...form,publicId:generateUserId()})}>Generate</Button></InputAdornment>}}/><TextField label="Full name" required value={form.fullName} onChange={e => setForm({...form, fullName:e.target.value})}/><TextField label="Email" type="email" value={form.email} onChange={e => setForm({...form, email:e.target.value})}/><TextField label="Phone" value={form.phone} onChange={e => setForm({...form, phone:e.target.value})}/><FormControl><InputLabel>Role</InputLabel><Select label="Role" value={form.role} onChange={e => setForm({...form, role:e.target.value as UserRole})}><MenuItem value="ADMIN">Admin user</MenuItem><MenuItem value="STAFF">Staff</MenuItem><MenuItem value="VISITOR">Visitor</MenuItem></Select></FormControl></Stack></DialogContent><DialogActions><Button onClick={() => setOpen(false)}>Cancel</Button><Button type="submit" variant="contained">Create</Button></DialogActions></Stack></Dialog>
  </Stack>
}
