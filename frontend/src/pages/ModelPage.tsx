import { useEffect, useState } from 'react'
import { Alert, Card, CardContent, Grid, LinearProgress, Stack, Typography } from '@mui/material'
import { api, errorMessage } from '../api'
import type { ModelInfo } from '../types'

export default function ModelPage(){
  const [model,setModel]=useState<ModelInfo>(),[error,setError]=useState('')
  useEffect(()=>{api.get('/api/model/info').then(r=>setModel(r.data)).catch(e=>setError(errorMessage(e)))},[])
  if(!model)return <Stack spacing={2}>{error?<Alert severity="error">{error}</Alert>:<LinearProgress/>}</Stack>
  const metrics=[['Accuracy',model.accuracy],['Precision',model.precision],['Recall',model.recall]] as const
  return <Stack spacing={3}><div><Typography variant="h4" fontWeight={800}>Decision Tree model</Typography><Typography color="text.secondary">Pruned WEKA J48 classifier · {model.version} · {model.trainingRows} documented synthetic rows</Typography></div><Grid container spacing={2}>{metrics.map(([label,value])=><Grid size={{xs:12,sm:4}} key={label}><Card><CardContent><Typography color="text.secondary">{label}</Typography><Typography variant="h4" fontWeight={800}>{(value*100).toFixed(1)}%</Typography><LinearProgress variant="determinate" value={value*100}/></CardContent></Card></Grid>)}</Grid><Card><CardContent><Typography variant="h6" fontWeight={750}>Generated tree</Typography><pre className="model-output">{model.tree}</pre></CardContent></Card><Card><CardContent><Typography variant="h6" fontWeight={750}>Confusion matrix</Typography><pre className="model-output">{model.confusionMatrix}</pre></CardContent></Card><Alert severity="info">Cryptographic validity, revocation, expiry, user status, door permission, and schedule remain mandatory security controls. The model cannot override them.</Alert></Stack>
}
