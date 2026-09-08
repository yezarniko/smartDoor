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
    <Card sx={{ width:'100%', maxWidth:440, alignSelf:'center' }}><CardContent><Stack spacing={2}>
      <FormControl fullWidth disabled={running}><InputLabel id="scanner-camera-label">Camera</InputLabel><Select labelId="scanner-camera-label" value={camera} label="Camera" onChange={e=>setCamera(e.target.value)}>{cameras.map((item,index)=><MenuItem value={item.deviceId} key={item.deviceId}>{item.label||`Camera ${index+1}`}</MenuItem>)}</Select></FormControl>
      <Box sx={{ display:'flex', flexWrap:'wrap', gap:1, '& .MuiButton-root':{ flex:'1 1 140px', minHeight:44, whiteSpace:'nowrap' } }}>
        {running?<Button color="error" variant="contained" startIcon={<StopCircleOutlined/>} onClick={stop}>Stop camera</Button>:<Button variant="contained" startIcon={<VideocamOutlined/>} onClick={start} disabled={!cameras.length}>Start camera</Button>}
        <Button component="label" variant="outlined" startIcon={<UploadFileOutlined/>}>Upload QR<input type="file" accept="image/*" hidden onChange={upload}/></Button>
      </Box>
      <Box className="scanner-preview">
        <video className="scanner-video" ref={videoRef} muted playsInline aria-label="Live QR scanner camera preview" />
        <div className="scanner-overlay" aria-hidden="true"><div className="scanner-guide" /></div>
        <Typography className="scanner-status" variant="body2" role="status">{running?'Camera live · Align QR code here':'Start camera to scan a QR code'}</Typography>
      </Box>
      <Typography variant="body2" color="text.secondary" textAlign="center">Keep the entire QR code inside the square. Hold steady in good light to scan automatically.</Typography>
    </Stack></CardContent></Card>
    {busy&&<Alert severity="info">Verifying credential…</Alert>}
    {decision&&<Card sx={{borderLeft:6,borderColor:decision.decision==='GRANTED'?'success.main':'error.main'}}><CardContent><Stack direction="row" justifyContent="space-between" alignItems="center"><div><Typography variant="h5" fontWeight={800}>{decision.decision==='GRANTED'?'Access granted':'Access denied'}</Typography><Typography color="text.secondary">{decision.userName||'Unknown user'} · {decision.resultCode}</Typography></div><Chip color={decision.decision==='GRANTED'?'success':'error'} label={decision.executionStatus}/></Stack><Box component="ol" sx={{pl:2.5,mb:0}}>{decision.path.map((step,index)=><li key={`${step}-${index}`}><Typography variant="body2">{step}</Typography></li>)}</Box></CardContent></Card>}
  </Stack>
}
