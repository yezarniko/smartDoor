export type UserRole = 'ADMIN' | 'STAFF' | 'VISITOR'
export type UserStatus = 'ACTIVE' | 'INACTIVE'

export interface UserRecord {
  id: string; publicId: string; fullName: string; email?: string; phone?: string
  role: UserRole; status: UserStatus; createdAt: string; updatedAt: string
}
export interface Door { id: string; publicId: string; name: string; status: string }
export interface Permission { doorId: string; doorPublicId: string; doorName: string }
export interface Schedule { id?: string; dayOfWeek: string; startTime: string; endTime: string }
export interface Credential {
  id: string; userId: string; status: string; usageMode: 'ONE_TIME' | 'MULTI_USE'
  usageCount: number; maxUses?: number; issuedAt: string; expiresAt: string
  token?: string; qrDataUrl?: string
}
export interface AccessDecision {
  requestId: string; decision: 'GRANTED' | 'DENIED'; resultCode: string; executionStatus: string
  path: string[]; userPublicId?: string; userName?: string
}
export interface AccessEvent {
  id: string; requestId: string; userPublicId?: string; userName?: string; doorPublicId?: string
  devicePublicId?: string; resultCode: string; executionStatus: string; modelResult?: string
  modelVersion?: string; evaluationPath: string[]; occurredAt: string
}
export interface Device {
  id: string; publicId: string; doorPublicId: string; type: string; status: string
  lastHeartbeatAt?: string; online: boolean
}
export interface ModelInfo {
  version: string; accuracy: number; precision: number; recall: number
  confusionMatrix: string; tree: string; trainingRows: number
}

