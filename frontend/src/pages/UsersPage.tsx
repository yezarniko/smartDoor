import { FormEvent, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Add, Delete, ManageAccounts, Search } from '@mui/icons-material'
import { Alert, Button, Chip, Dialog, DialogActions, DialogContent, DialogContentText, DialogTitle, FormControl, InputAdornment, InputLabel, MenuItem, Paper, Select, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from '@mui/material'
import RoleManagerDialog from '../components/RoleManagerDialog'
import { api, errorMessage } from '../api'
import type { RoleRecord, UserRecord } from '../types'

const emptyForm = { fullName: '', email: '', phone: '', role: '' }

export default function UsersPage() {
  const [users, setUsers] = useState<UserRecord[]>([])
  const [roles, setRoles] = useState<RoleRecord[]>([])
  const [nextCode, setNextCode] = useState<number>()
  const [query, setQuery] = useState('')
  const [open, setOpen] = useState(false)
  const [rolesOpen, setRolesOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<UserRecord>()
  const [form, setForm] = useState(emptyForm)
  const [error, setError] = useState('')

  const load = async () => {
    try {
      const [userResult, roleResult, codeResult] = await Promise.all([
        api.get<UserRecord[]>('/api/users'), api.get<RoleRecord[]>('/api/roles'), api.get<{ code: number }>('/api/users/next-code')
      ])
      setUsers(userResult.data); setRoles(roleResult.data); setNextCode(codeResult.data.code)
      setForm(current => ({ ...current, role: roleResult.data.some(role => role.code === current.role) ? current.role : (roleResult.data[0]?.code || '') }))
    } catch (e) { setError(errorMessage(e)) }
  }
  useEffect(() => { void load() }, [])

  const openCreate = async () => {
    setError('')
    try {
      const response = await api.get<{ code: number }>('/api/users/next-code')
      setNextCode(response.data.code)
      setForm({ ...emptyForm, role: roles[0]?.code || '' })
      setOpen(true)
    } catch (e) { setError(errorMessage(e)) }
  }
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setError('')
    try { await api.post('/api/users', form); setOpen(false); setForm(emptyForm); await load() }
    catch (e) { setError(errorMessage(e)) }
  }
  const confirmDelete = async () => {
    if (!deleteTarget) return
    setError('')
    try { await api.delete(`/api/users/${deleteTarget.id}`); setDeleteTarget(undefined); await load() }
    catch (e) { setError(errorMessage(e)); setDeleteTarget(undefined) }
  }

  const roleName = (code: string) => roles.find(role => role.code === code)?.name || code
  const filtered = users.filter(user => `${user.code} ${user.fullName} ${user.email || ''} ${roleName(user.role)}`.toLowerCase().includes(query.toLowerCase()))

  return <Stack spacing={3}>
    <Stack direction={{ xs: 'column', sm: 'row' }} justifyContent="space-between" gap={2}>
      <div><Typography variant="h4" fontWeight={800}>Users</Typography><Typography color="text.secondary">Create users, assign access, and issue QR credentials.</Typography></div>
      <Stack direction="row" spacing={1}><Button variant="outlined" startIcon={<ManageAccounts />} onClick={() => setRolesOpen(true)}>Manage roles</Button><Button variant="contained" startIcon={<Add />} onClick={openCreate}>Create user</Button></Stack>
    </Stack>
    {error && <Alert severity="error" onClose={() => setError('')}>{error}</Alert>}
    <TextField placeholder="Search users" value={query} onChange={e => setQuery(e.target.value)} InputProps={{ startAdornment: <InputAdornment position="start"><Search /></InputAdornment> }} />
    <Paper><Table><TableHead><TableRow><TableCell>Code</TableCell><TableCell>Name</TableCell><TableCell>Role</TableCell><TableCell>Status</TableCell><TableCell align="right">Manage</TableCell></TableRow></TableHead><TableBody>{filtered.map(user => <TableRow key={user.id} hover><TableCell><Typography fontWeight={700}>{user.code}</Typography></TableCell><TableCell><Typography fontWeight={650}>{user.fullName}</Typography><Typography variant="body2" color="text.secondary">{user.email || 'No email'}</Typography></TableCell><TableCell>{roleName(user.role)}</TableCell><TableCell><Chip size="small" color={user.status === 'ACTIVE' ? 'success' : 'default'} label={user.status} /></TableCell><TableCell align="right"><Button component={Link} to={`/users/${user.id}`}>Open</Button><Button color="error" startIcon={<Delete />} onClick={() => setDeleteTarget(user)}>Delete</Button></TableCell></TableRow>)}</TableBody></Table></Paper>

    <Dialog open={open} onClose={() => setOpen(false)} fullWidth maxWidth="sm"><Stack component="form" onSubmit={submit}><DialogTitle>Create user</DialogTitle><DialogContent><Stack spacing={2} mt={1}><TextField label="User code" value={nextCode ?? ''} InputProps={{ readOnly: true }} helperText="Assigned by the server when the user is created"/><TextField label="Full name" required value={form.fullName} onChange={e => setForm({ ...form, fullName: e.target.value })}/><TextField label="Email" type="email" value={form.email} onChange={e => setForm({ ...form, email: e.target.value })}/><TextField label="Phone" value={form.phone} onChange={e => setForm({ ...form, phone: e.target.value })}/><FormControl required><InputLabel>Role</InputLabel><Select label="Role" value={form.role} onChange={e => setForm({ ...form, role: e.target.value })}>{roles.map(role => <MenuItem key={role.code} value={role.code}>{role.name}</MenuItem>)}</Select></FormControl></Stack></DialogContent><DialogActions><Button onClick={() => setOpen(false)}>Cancel</Button><Button type="submit" variant="contained" disabled={!form.role}>Create</Button></DialogActions></Stack></Dialog>

    <Dialog open={Boolean(deleteTarget)} onClose={() => setDeleteTarget(undefined)}><DialogTitle>Delete user?</DialogTitle><DialogContent><DialogContentText>Delete {deleteTarget?.fullName} (code {deleteTarget?.code}) and all QR credentials, schedules, permissions, and user access events? This code becomes available for the next user.</DialogContentText></DialogContent><DialogActions><Button onClick={() => setDeleteTarget(undefined)}>Cancel</Button><Button color="error" variant="contained" onClick={confirmDelete}>Delete user</Button></DialogActions></Dialog>
    <RoleManagerDialog open={rolesOpen} onClose={() => setRolesOpen(false)} onChanged={load} />
  </Stack>
}
