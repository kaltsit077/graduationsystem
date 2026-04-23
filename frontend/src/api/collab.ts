import request from './request'

export interface StageProgressItem {
  orderIndex: number
  phaseIndex: number
  phaseLabel: string
  stage: string
  stageLabel: string
  windowStart?: string | null
  windowEnd?: string | null
  accessState: string
  timePlanText: string
  submissionStatus: string
  submissionStatusLabel: string
  latestThesisId?: number | null
  latestFileName?: string | null
  latestUpdatedAt?: string | null
}

export interface StageWindowItem {
  stage: string
  windowStart?: string | null
  windowEnd?: string | null
}

export const getCollabProgress = (applicationId: number) => {
  return request.get<StageProgressItem[]>(`/collab/applications/${applicationId}/progress`)
}

export const saveCollabStageWindows = (applicationId: number, items: StageWindowItem[]) => {
  return request.put<void>(`/collab/applications/${applicationId}/stage-windows`, items)
}
