import { describe, expect, it } from 'vitest'
import { validateCommand } from './protocol.js'

describe('device command validation', () => {
  it('accepts a fresh command for the configured door', () => {
    const now = Date.now()
    expect(validateCommand({ requestId:'r1', doorId:'DOOR-01', command:'UNLOCK', durationMs:5000, issuedAt:new Date(now).toISOString() }, 'DOOR-01', now).requestId).toBe('r1')
  })
  it('rejects stale commands', () => {
    const now = Date.now()
    expect(() => validateCommand({ requestId:'r1', doorId:'DOOR-01', command:'UNLOCK', durationMs:5000, issuedAt:new Date(now-15000).toISOString() }, 'DOOR-01', now)).toThrow('Stale')
  })
})
