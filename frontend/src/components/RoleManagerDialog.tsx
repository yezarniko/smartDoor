import { FormEvent, useEffect, useState } from 'react'
import { Delete, Edit } from '@mui/icons-material'
import { Alert, Button, Dialog, DialogActions, DialogContent, DialogTitle, FormControl, IconButton, InputLabel, MenuItem, Select, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from '@mui/material'
import { api, errorMessage } from '../api'
import type { RoleRecord } from '../types'

type Props = { open: boolean; onClose: () => void; onChanged: () => void }
const emptyForm = { code: '', name: '', modelRole: 'STAFF' as RoleRecord['modelRole'] }

export default function RoleManagerDialog({ open, onClose, onChanged }: Props) {
  const [roles, setRoles] = useState<RoleRecord[]>([])
  const [form, setForm] = useState(emptyForm)
  const [editing, setEditing] = useState<string>()
  const [error, setError] = useState('')
  const load = () => api.get<RoleRecord[]>('/api/roles').then(r => setRoles(r.data)).catch(e => setError(errorMessage(e)))
  useEffect(() => { if (open) void load() }, [open])
  const reset = () => { setEditing(undefined); setForm(emptyForm); setError('') }
  const submit = async (event: FormEvent) => {
    event.preventDefault(); setError('')
    try {
      if (editing) await api.patch(`/api/roles/${editing}`, { name: form.name, modelRole: form.modelRole })
      else await api.post('/api/roles', form)
      reset(); await load(); onChanged()
    } catch (e) { setError(errorMessage(e)) }
  }
  const editRole = (role: RoleRecord) => { setEditing(role.code); setForm({ code: role.code, name: role.name, modelRole: role.modelRole }) }
  const deleteRole = async (role: RoleRecord) => {
    if (!window.confirm(`Delete role “${role.name}”? Roles assigned to users cannot be deleted.`)) return
    setError('')
    try { await api.delete(`/api/roles/${role.code}`); await load(); onChanged() }
    catch (e) { setError(errorMessage(e)) }
  }
  return <Dialog open={open} onClose={onClose} fullWidth maxWidth="md">
    <DialogTitle>Manage roles</DialogTitle>
    <DialogContent><Stack spacing={3} mt={1}>
      {error && <Alert severity="error" onClose={() => setError('')}>{error}</Alert>}
      <Stack component="form" onSubmit={submit} direction={{ xs: 'column', md: 'row' }} spacing={2} alignItems={{ md: 'center' }}>
        <TextField label="Role code" required value={form.code} disabled={Boolean(editing)} helperText="Letters, numbers, underscores" onChange={e => setForm({ ...form, code: e.target.value.toUpperCase().replace(/[^A-Z0-9_]/g, '') })} />
        <TextField label="Display name" required value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} />
        <FormControl sx={{ minWidth: 180 }}><InputLabel>Access profile</InputLabel><Select label="Access profile" value={form.modelRole} onChange={e => setForm({ ...form, modelRole: e.target.value as RoleRecord['modelRole'] })}><MenuItem value="ADMIN">Admin</MenuItem><MenuItem value="STAFF">Staff</MenuItem><MenuItem value="VISITOR">Visitor</MenuItem></Select></FormControl>
        <Stack direction="row" spacing={1}><Button type="submit" variant="contained">{editing ? 'Save' : 'Add role'}</Button>{editing && <Button onClick={reset}>Cancel</Button>}</Stack>
      </Stack>
      <Table size="small"><TableHead><TableRow><TableCell>Code</TableCell><TableCell>Name</TableCell><TableCell>Access profile</TableCell><TableCell align="right">Actions</TableCell></TableRow></TableHead><TableBody>{roles.map(role => <TableRow key={role.code}><TableCell><Typography fontFamily="monospace">{role.code}</Typography></TableCell><TableCell>{role.name}</TableCell><TableCell>{role.modelRole}</TableCell><TableCell align="right"><IconButton aria-label={`Edit ${role.name}`} onClick={() => editRole(role)}><Edit /></IconButton><IconButton color="error" aria-label={`Delete ${role.name}`} onClick={() => deleteRole(role)}><Delete /></IconButton></TableCell></TableRow>)}</TableBody></Table>
    </Stack></DialogContent>
    <DialogActions><Button onClick={onClose}>Close</Button></DialogActions>
  </Dialog>
}
