import { ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import {
  AppBar, Box, Button, Container, Divider, Drawer, List, ListItemButton,
  ListItemIcon, ListItemText, Toolbar, Typography,
} from '@mui/material'
import DashboardOutlined from '@mui/icons-material/DashboardOutlined'
import PeopleOutlined from '@mui/icons-material/PeopleOutlined'
import QrCodeScannerOutlined from '@mui/icons-material/QrCodeScannerOutlined'
import ReceiptLongOutlined from '@mui/icons-material/ReceiptLongOutlined'
import SensorDoorOutlined from '@mui/icons-material/SensorDoorOutlined'
import AccountTreeOutlined from '@mui/icons-material/AccountTreeOutlined'
import { api } from '../api'

const drawerWidth = 248
const nav = [
  ['/', 'Dashboard', <DashboardOutlined />],
  ['/users', 'Users & QR', <PeopleOutlined />],
  ['/scanner/door-01', 'Webcam Scanner', <QrCodeScannerOutlined />],
  ['/access-logs', 'Access Logs', <ReceiptLongOutlined />],
  ['/door-status', 'Door Status', <SensorDoorOutlined />],
  // ['/model', 'Decision Tree', <AccountTreeOutlined />],
] as const

export default function AppShell({ children, onLoggedOut }: { children: ReactNode; onLoggedOut: () => void }) {
  const location = useLocation()
  const logout = async () => { await api.post('/api/auth/logout'); onLoggedOut() }
  return <Box display="flex" minHeight="100vh">
    <AppBar position="fixed" sx={{ zIndex: theme => theme.zIndex.drawer + 1 }}>
      <Toolbar><SensorDoorOutlined sx={{ mr: 1.5 }} /><Typography variant="h6" sx={{ flexGrow: 1 }}>SmartDoor Control</Typography><Button color="inherit" onClick={logout}>Sign out</Button></Toolbar>
    </AppBar>
    <Drawer variant="permanent" sx={{ width: drawerWidth, [`& .MuiDrawer-paper`]: { width: drawerWidth, boxSizing: 'border-box' } }}>
      <Toolbar /><Box sx={{ p: 2 }}><Typography variant="overline" color="text.secondary">Local access system</Typography></Box><Divider />
      <List>{nav.map(([to, label, icon]) => <ListItemButton component={Link} to={to} key={to} selected={location.pathname === to || (to === '/users' && location.pathname.startsWith('/users/'))}><ListItemIcon>{icon}</ListItemIcon><ListItemText primary={label} /></ListItemButton>)}</List>
    </Drawer>
    <Box component="main" sx={{ flexGrow: 1, ml: `${drawerWidth}px`, minWidth: 0 }}><Toolbar /><Container maxWidth="xl" sx={{ py: 4 }}>{children}</Container></Box>
  </Box>
}

