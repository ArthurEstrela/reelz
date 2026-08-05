import type { RouletteMovie, RouletteSpinRequest, SpinQuota } from './roulette'

export type SocialRoomType = 'COUPLE' | 'GROUP'
export type SocialRoomStatus = 'OPEN' | 'CLOSED'

export interface SocialProvider {
  id: string
  name: string
  logoPath: string | null
}

export interface SocialRoomMember {
  userId: string
  displayName: string
  host: boolean
  joinedAt: string
  providers: SocialProvider[]
}

export interface SocialRoom {
  id: string
  type: SocialRoomType
  status: SocialRoomStatus
  inviteCode: string
  hostUserId: string
  hostDisplayName: string
  currentUserHost: boolean
  capacity: number
  members: SocialRoomMember[]
  commonProviders: SocialProvider[]
  lastMovie: RouletteMovie | null
  lastSpinNumber: number
  updatedAt: string
}

export interface SocialRoomSummary {
  id: string
  type: SocialRoomType
  status: SocialRoomStatus
  currentUserHost: boolean
  memberCount: number
  capacity: number
  lastSpinNumber: number
  updatedAt: string
}

export interface SocialSpinResponse {
  room: SocialRoom
  movie: RouletteMovie
  quota: SpinQuota
}

export type SocialSpinRequest = RouletteSpinRequest
