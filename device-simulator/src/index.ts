import mqtt from 'mqtt'
import { DoorState, UnlockCommand, validateCommand } from './protocol.js'

const host = process.env.MQTT_HOST || 'localhost'
const port = Number(process.env.MQTT_PORT || 1883)
const username = process.env.MQTT_USERNAME || 'smartdoor'
const password = process.env.MQTT_PASSWORD || 'smartdoor_mqtt_password'
const doorId = process.env.DOOR_ID || 'DOOR-01'
const deviceId = process.env.DEVICE_ID || 'SIM-DOOR-01'
const baseTopic = `smartdoor/${doorId.toLowerCase()}`
const handled = new Set<string>()
let state: DoorState = 'OFFLINE'

const client = mqtt.connect(`mqtt://${host}:${port}`, {
  username, password, clientId: `${deviceId}-${Math.random().toString(16).slice(2)}`,
  clean: true, reconnectPeriod: 2000,
  will: { topic: `${baseTopic}/status`, payload: JSON.stringify({ deviceId, state: 'OFFLINE' }), qos: 1, retain: true },
})

function publish(topic: 'status' | 'event' | 'heartbeat', requestId?: string) {
  client.publish(`${baseTopic}/${topic}`, JSON.stringify({ deviceId, doorId, state, requestId, at: new Date().toISOString() }), { qos: 1, retain: topic === 'status' })
}

function setState(next: DoorState, requestId?: string) {
  state = next
  publish('status', requestId)
  publish('event', requestId)
  console.log(`${new Date().toISOString()} ${doorId} ${state}${requestId ? ` ${requestId}` : ''}`)
}

const delay = (ms: number) => new Promise(resolve => setTimeout(resolve, ms))

async function execute(command: UnlockCommand) {
  if (handled.has(command.requestId)) return
  handled.add(command.requestId)
  if (handled.size > 500) handled.delete(handled.values().next().value!)
  try {
    setState('UNLOCKING', command.requestId)
    await delay(350)
    setState('UNLOCKED', command.requestId)
    await delay(command.durationMs)
    setState('RELOCKING', command.requestId)
    await delay(350)
    setState('LOCKED', command.requestId)
  } catch {
    setState('ERROR', command.requestId)
  }
}

client.on('connect', () => {
  client.subscribe(`${baseTopic}/command`, { qos: 1 })
  setState('LOCKED')
})

client.on('message', (_topic, payload) => {
  try { void execute(validateCommand(JSON.parse(payload.toString()), doorId)) }
  catch (error) { console.error(`Rejected command: ${(error as Error).message}`) }
})

client.on('offline', () => { state = 'OFFLINE' })
client.on('error', error => console.error(`MQTT error: ${error.message}`))
setInterval(() => { if (client.connected) publish('heartbeat') }, 5000)

function shutdown() {
  if (client.connected) { state = 'LOCKED'; publish('status'); client.end(false, {}, () => process.exit(0)) }
  else process.exit(0)
}
process.on('SIGTERM', shutdown)
process.on('SIGINT', shutdown)
