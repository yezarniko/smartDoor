export type DoorState = 'OFFLINE' | 'LOCKED' | 'UNLOCKING' | 'UNLOCKED' | 'RELOCKING' | 'ERROR'

export interface UnlockCommand {
  requestId: string
  doorId: string
  command: 'UNLOCK'
  durationMs: number
  issuedAt: string
}

export function validateCommand(value: unknown, doorId: string, now = Date.now()): UnlockCommand {
  if (!value || typeof value !== 'object') throw new Error('Command must be an object')
  const command = value as Partial<UnlockCommand>
  if (!command.requestId || command.command !== 'UNLOCK' || command.doorId !== doorId) throw new Error('Invalid command identity')
  if (!command.issuedAt || Math.abs(now - Date.parse(command.issuedAt)) > 10_000) throw new Error('Stale command')
  if (!Number.isFinite(command.durationMs) || command.durationMs! < 1000 || command.durationMs! > 30_000) throw new Error('Invalid unlock duration')
  return command as UnlockCommand
}

