import http from './http'

export interface LoginPayload {
  userId?: string
  username?: string
  password?: string
}

export interface DepartmentItem {
  id: string
  deptId?: string
  name: string
  parentId?: string
  sortNo?: number
}

export interface UserItem {
  id: string
  userId?: string
  username: string
  employeeNo?: string
  realName?: string
  role: string
  departmentId?: string
  departmentName?: string
  mobile?: string
}

export interface TopicPayload {
  id?: number
  topicType: string
  title: string
  reportDepartmentId?: string
  reportDepartmentName?: string
  participantDepartments?: string
  participantDepartmentIds?: string[]
  summary?: string
  sortNo: number
}

export interface TopicItem extends TopicPayload {
  status?: string
  conclusion?: string
  actualMinutes?: number
  startTime?: string
  endTime?: string
  attendees?: UserItem[]
  participantDeptId?: string
  participantDeptName?: string
  reportDepartmentSigned?: boolean | number
}

export interface MeetingItem {
  id: number
  meetingDate: string
  meetingTime: string
  periodNo: string
  location: string
  leaders?: string
  content?: string
  status: string
  topics?: TopicItem[]
  attendees?: UserItem[]
  topicSignStats?: Array<Record<string, unknown>>
}

export interface SignQrCodeResult {
  meetingId: number
  token: string
  expiresAt: string
  url: string
}

export interface SignInPreview {
  meeting: MeetingItem
  topics: Array<TopicItem & {
    attendeeId?: number
    attendeeType?: string
    signStatus?: string
    signed?: boolean
    signedAt?: string
  }>
  canSign: boolean
  message?: string
  expiresAt?: string
}

export interface MeetingPayload {
  meetingDate: string
  meetingTime: string
  periodNo: string
  location: string
  leaders?: string
  content: string
  topics: TopicPayload[]
}

export interface ImportResult {
  meetingId: number
  fileName: string
  parserStatus: string
  parserMessage: string
  topicCount: number
  topics: TopicPayload[]
}

export const api = {
  login: (payload: LoginPayload) => http.post('/auth/login', payload),
  me: () => http.get('/auth/me'),
  departments: () => http.get('/departments'),
  users: (params?: Record<string, unknown>) => http.get('/users', { params }),
  meetings: () => http.get('/meetings'),
  meeting: (id: number) => http.get(`/meetings/${id}`),
  meetingDetail: (id: number) => http.get(`/meetings/${id}/detail`),
  createMeeting: (payload: MeetingPayload) => http.post('/meetings', payload),
  updateMeeting: (id: number, payload: MeetingPayload) => http.put(`/meetings/${id}`, payload),
  publish: (id: number) => http.post(`/meetings/${id}/publish`),
  importTopics: (id: number, file: File) => {
    const form = new FormData()
    form.append('file', file)
    return http.post(`/meetings/${id}/topics/import`, form, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  selectionTasks: (id: number) => http.get(`/meetings/${id}/selection-tasks`),
  submitAttendees: (topicId: number, payload: { userIds: string[]; attendeeType?: 'LEADER' | 'SHARE' | 'REPORT' | 'PARTAKE' | 'PARTICIPANT' }) => http.post(`/topics/${topicId}/attendees`, payload),
  confirmAttendees: (meetingId: number) => http.post(`/meetings/${meetingId}/attendees/confirm`),
  createSignQrCode: (meetingId: number) => http.post(`/meetings/${meetingId}/sign-qrcode`),
  signInPreview: (token: string) => http.get('/sign-in/preview', { params: { token } }),
  signIn: (payload: { token: string }) => http.post('/sign-in', payload),
  startTopic: (topicId: number) => http.post(`/topics/${topicId}/start`),
  endTopic: (topicId: number, payload?: { actualMinutes?: number }) => http.post(`/topics/${topicId}/end`, payload || {}),
  saveConclusion: (topicId: number, payload: { conclusion: string; actualMinutes?: number }) => http.put(`/topics/${topicId}/conclusion`, payload),
  dashboard: (meetingId: number) => http.get(`/meetings/${meetingId}/dashboard`),
  signinDashboard: (meetingId: number) => http.get(`/meetings/${meetingId}/signin-dashboard`)
}
