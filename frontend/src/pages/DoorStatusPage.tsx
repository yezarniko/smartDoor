import { useEffect, useState } from 'react'
import { Alert, Card, CardContent, Chip, Grid, Stack, Typography } from '@mui/material'
import { API_BASE_URL, api, errorMessage } from '../api'
import type { Device } from '../types'

export default function DoorStatusPage(){
  const [devices,setDevices]=useState<Device[]>([]),[error,setError]=useState('')
  const load=()=>api.get('/api/devices').then(r=>setDevices(r.data)).catch(e=>setError(errorMessage(e)))
  useEffect(()=>{
    load()
    const source=new EventSource(`${API_BASE_URL}/api/events/stream`,{withCredentials:true})
    source.addEventListener('device-status',load)
    source.onopen=()=>setError('')
    source.onerror=()=>setError('Live device stream disconnected; status will refresh periodically.')
    const timer=setInterval(load,5000)
    return()=>{source.close();clearInterval(timer)}
  },[])
  const actuator=devices.find(d=>d.type==='ACTUATOR'&&d.online)
    ?? devices.find(d=>d.type==='ACTUATOR')
    ?? devices.find(d=>d.type==='SIMULATOR'&&d.online)
    ?? devices.find(d=>d.type==='SIMULATOR')
  const unlocked=actuator?.status==='UNLOCKED'||actuator?.status==='UNLOCKING'
  return <Stack spacing={3}><div><Typography variant="h4" fontWeight={800}>Door status</Typography><Typography color="text.secondary">Virtual and physical devices use the same MQTT protocol.</Typography></div>{error&&<Alert severity="warning" onClose={()=>setError('')}>{error}</Alert>}<Grid container spacing={3}><Grid size={{xs:12,md:5}}><Card><CardContent><Stack alignItems="center" spacing={2} py={2}><div className={`door-visual ${unlocked?'unlocked':''}`}><div className="door-handle"/></div><Typography variant="h5" fontWeight={800}>{actuator?.status||'OFFLINE'}</Typography><Chip color={actuator?.online?'success':'error'} label={actuator?.online?'Device online':'Device offline'}/></Stack></CardContent></Card></Grid><Grid size={{xs:12,md:7}}><Stack spacing={2}>{devices.map(device=><Card key={device.id}><CardContent><Stack direction="row" justifyContent="space-between"><div><Typography fontWeight={750}>{device.publicId}</Typography><Typography color="text.secondary">{device.type} · {device.doorPublicId}</Typography></div><Chip color={device.online?'success':'default'} label={device.status}/></Stack><Typography variant="body2" mt={1}>Last heartbeat: {device.lastHeartbeatAt?new Date(device.lastHeartbeatAt).toLocaleString():'Never'}</Typography></CardContent></Card>)}</Stack></Grid></Grid></Stack>
}

