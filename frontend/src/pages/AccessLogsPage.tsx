import { useEffect, useState } from 'react'
import { Alert, Chip, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, TextField, Typography } from '@mui/material'
import { api, errorMessage } from '../api'
import type { AccessEvent } from '../types'

export default function AccessLogsPage(){
  const [events,setEvents]=useState<AccessEvent[]>([]),[filter,setFilter]=useState(''),[error,setError]=useState('')
  const load=()=>api.get('/api/access-events').then(r=>setEvents(r.data)).catch(e=>setError(errorMessage(e)))
  useEffect(()=>{load();const timer=setInterval(load,5000);return()=>clearInterval(timer)},[])
  const shown=events.filter(e=>JSON.stringify(e).toLowerCase().includes(filter.toLowerCase()))
  return <Stack spacing={3}><div><Typography variant="h4" fontWeight={800}>Access logs</Typography><Typography color="text.secondary">The latest 200 authorization and device outcomes.</Typography></div>{error&&<Alert severity="error">{error}</Alert>}<TextField label="Filter logs" value={filter} onChange={e=>setFilter(e.target.value)}/><Paper sx={{overflow:'auto'}}><Table size="small"><TableHead><TableRow><TableCell>Time</TableCell><TableCell>User</TableCell><TableCell>Door</TableCell><TableCell>Decision</TableCell><TableCell>Execution</TableCell><TableCell>Model</TableCell></TableRow></TableHead><TableBody>{shown.map(e=><TableRow key={e.id}><TableCell sx={{whiteSpace:'nowrap'}}>{new Date(e.occurredAt).toLocaleString()}</TableCell><TableCell>{e.userName||'Unknown'}<Typography variant="caption" display="block" color="text.secondary">{e.userPublicId}</Typography></TableCell><TableCell>{e.doorPublicId||'—'}</TableCell><TableCell><Chip size="small" color={e.resultCode==='GRANTED'?'success':'error'} label={e.resultCode}/></TableCell><TableCell>{e.executionStatus}</TableCell><TableCell>{e.modelResult||'Not evaluated'}</TableCell></TableRow>)}</TableBody></Table></Paper></Stack>
}

