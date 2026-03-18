import request from '@/api/request'

export interface UserBackgroundResponse {
  backgroundUrl: string | null
  backgroundScale?: number
  backgroundPosX?: number
  backgroundPosY?: number
  bgOverlayAlpha?: number
  contentAlpha?: number
  contentBlur?: number
}

export async function getMyBackground() {
  return request.get<UserBackgroundResponse>('/user/background')
}

export async function uploadMyBackground(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request.post<UserBackgroundResponse>('/user/background', form, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export async function resetMyBackground() {
  return request.delete<UserBackgroundResponse>('/user/background')
}

export async function updateMyAppearance(payload: Partial<UserBackgroundResponse>) {
  return request.put<UserBackgroundResponse>('/user/background', payload)
}

