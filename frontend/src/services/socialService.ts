import { api } from './api'
import type {
  SocialRoom,
  SocialRoomSummary,
  SocialRoomType,
  SocialSpinRequest,
  SocialSpinResponse,
  UpdateSocialPreferenceRequest,
} from '../types/social'

export async function createSocialRoom(type: SocialRoomType): Promise<SocialRoom> {
  const { data } = await api.post<SocialRoom>('/api/v1/social/rooms', { type })
  return data
}

export async function joinSocialRoom(inviteCode: string): Promise<SocialRoom> {
  const { data } = await api.post<SocialRoom>('/api/v1/social/rooms/join', { inviteCode })
  return data
}

export async function getSocialRoom(roomId: string): Promise<SocialRoom> {
  const { data } = await api.get<SocialRoom>(`/api/v1/social/rooms/${roomId}`)
  return data
}

export async function listSocialRooms(): Promise<SocialRoomSummary[]> {
  const { data } = await api.get<SocialRoomSummary[]>('/api/v1/social/rooms')
  return data
}

export async function spinSocialRoom(
  roomId: string,
  payload: SocialSpinRequest,
): Promise<SocialSpinResponse> {
  const { data } = await api.post<SocialSpinResponse>(
    `/api/v1/social/rooms/${roomId}/spin`,
    payload,
  )
  return data
}

export async function leaveSocialRoom(roomId: string): Promise<void> {
  await api.delete(`/api/v1/social/rooms/${roomId}/members/me`)
}

export async function updateSocialPreference(
  roomId: string,
  payload: UpdateSocialPreferenceRequest,
): Promise<SocialRoom> {
  const { data } = await api.put<SocialRoom>(
    `/api/v1/social/rooms/${roomId}/members/me/preferences`,
    payload,
  )
  return data
}
