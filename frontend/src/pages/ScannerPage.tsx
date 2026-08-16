import { ChangeEvent, useEffect, useRef, useState } from 'react'
import { BrowserQRCodeReader, IScannerControls } from '@zxing/browser'
import { Alert, Box, Button, Card, CardContent, Chip, FormControl, InputLabel, MenuItem, Select, Stack, Typography } from '@mui/material'
import VideocamOutlined from '@mui/icons-material/VideocamOutlined'
import StopCircleOutlined from '@mui/icons-material/StopCircleOutlined'
import UploadFileOutlined from '@mui/icons-material/UploadFileOutlined'
import { api, errorMessage } from '../api'
import type { AccessDecision } from '../types'

export default function ScannerPage() {
  const videoRef=useRef<HTMLVideoElement>(null), controlsRef=useRef<IScannerControls|null>(null), lastScan=useRef({token:'',at:0})
  const [cameras,setCameras]=useState<MediaDeviceInfo[]>([]), [camera,setCamera]=useState(''), [running,setRunning]=useState(false), [error,setError]=useState(''), [decision,setDecision]=useState<AccessDecision>(), [busy,setBusy]=useState(false)
  useEffect(()=>{BrowserQRCodeReader.listVideoInputDevices().then(devices=>{setCameras(devices);if(devices[0])setCamera(devices[0].deviceId)}).catch(e=>setError(errorMessage(e)));return()=>controlsRef.current?.stop()},[])
  const verify=async(token:string)=>{const now=Date.now();if(lastScan.current.token===token&&now-lastScan.current.at<3000)return;lastScan.current={token,at:now};setBusy(true);setError('');try{const r=await api.post('/api/access/verify',{doorId:'DOOR-01',deviceId:'TERMINAL-01',qrToken:token});setDecision(r.data)}catch(e){setError(errorMessage(e))}finally{setBusy(false)}}
  const start=async()=>{setError('');try{const reader=new BrowserQRCodeReader();controlsRef.current=await reader.decodeFromVideoDevice(camera||undefined,videoRef.current!,result=>{if(result)void verify(result.getText())});setRunning(true)}catch(e){setError(errorMessage(e))}}
  const stop=()=>{controlsRef.current?.stop();controlsRef.current=null;setRunning(false)}
  const upload=async(event:ChangeEvent<HTMLInputElement>)=>{const file=event.target.files?.[0];if(!file)return;const url=URL.createObjectURL(file);try{const result=await new BrowserQRCodeReader().decodeFromImageUrl(url);await verify(result.getText())}catch(e){setError(`Unable to decode image: ${errorMessage(e)}`)}finally{URL.revokeObjectURL(url);event.target.value=''}}
  return <Stack spacing={3}>
    <div><Typography variant="h4" fontWeight={800}>Webcam scanner</Typography><Typography color="text.secondary">Door terminal TERMINAL-01 · DOOR-01</Typography></div>
    {error&&<Alert severity="error" onClose={()=>setError('')}>{error}</Alert>}
    <Card><CardContent><Stack spacing={2}><Stack direction={{xs:'column',sm:'row'}} spacing={2}><FormControl fullWidth><InputLabel>Camera</InputLabel><Select value={camera} label="Camera" onChange={e=>setCamera(e.target.value)}>{cameras.map((item,index)=><MenuItem value={item.deviceId} key={item.deviceId}>{item.label||`Camera ${index+1}`}</MenuItem>)}</Select></FormControl>{running?<Button color="error" variant="contained" startIcon={<StopCircleOutlined/>} onClick={stop}>Stop</Button>:<Button variant="contained" startIcon={<VideocamOutlined/>} onClick={start} disabled={!cameras.length}>Start camera</Button>}<Button component="label" variant="outlined" startIcon={<UploadFileOutlined/>}>Upload QR<input type="file" accept="image/*" hidden onChange={upload}/></Button></Stack><video className="scanner-video" ref={videoRef} muted playsInline /></Stack></CardContent></Card>
    {busy&&<Alert severity="info">Verifying credential…</Alert>}
    {decision&&<Card sx={{borderLeft:6,borderColor:decision.decision==='GRANTED'?'success.main':'error.main'}}><CardContent><Stack direction="row" justifyContent="space-between" alignItems="center"><div><Typography variant="h5" fontWeight={800}>{decision.decision==='GRANTED'?'Access granted':'Access denied'}</Typography><Typography color="text.secondary">{decision.userName||'Unknown user'} · {decision.resultCode}</Typography></div><Chip color={decision.decision==='GRANTED'?'success':'error'} label={decision.executionStatus}/></Stack><Box component="ol" sx={{pl:2.5,mb:0}}>{decision.path.map((step,index)=><li key={`${step}-${index}`}><Typography variant="body2">{step}</Typography></li>)}</Box></CardContent></Card>}
  </Stack>
}
