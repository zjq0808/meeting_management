<template>
  <div class="page">
    <PageHeader :title="detailTitle" :subtitle="subtitle" back @back="$router.push('/meetings')">
      <template #actions>
        <el-button v-if="canNotifyAll" type="primary" plain :icon="Bell" :loading="notifying" @click="notifyAllPending">通知参会人员</el-button>
        <el-button v-if="isAdmin" type="primary" :icon="Edit" @click="$router.push(`/meetings/${meetingId}/edit`)">编辑</el-button>
        <el-button @click="$router.push('/meetings')">返回</el-button>
      </template>
    </PageHeader>

    <section v-if="meeting" class="surface info-panel">
      <div class="section-head">
        <h2>会议基本信息</h2>
        <StatusBadge :status="meeting.status" />
      </div>
      <div class="info-grid">
        <div><span>会议日期</span><strong>{{ meeting.meetingDate || '-' }}</strong></div>
        <div><span>会议时间</span><strong>{{ meeting.meetingTime || '-' }}</strong></div>
        <div><span>会议期数</span><strong>{{ meeting.periodNo || '-' }}</strong></div>
        <div><span>会议地点</span><strong>{{ meeting.location || '-' }}</strong></div>
      </div>
      <div class="content-box">
        <span>会议内容</span>
        <p>{{ meeting.content || '暂无会议内容' }}</p>
      </div>
      <el-button link type="primary" :icon="View" @click="attendeeVisible = true">查看本次会议参会人员（{{ visibleAttendees.length }} 人）</el-button>
    </section>

    <section class="surface topic-panel">
      <div class="section-head">
        <h2>参会议题信息</h2>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="topics" border empty-text="暂无议题">
        <el-table-column label="序号" width="74" align="center">
          <template #default="{ row }">{{ pad(row.sortNo) }}</template>
        </el-table-column>
        <el-table-column label="议题名称" min-width="280">
          <template #default="{ row }">
            <strong>{{ row.title }}</strong>
          </template>
        </el-table-column>
        <el-table-column label="汇报部门" width="180">
          <template #default="{ row }">{{ row.reportDepartmentName || '-' }}</template>
        </el-table-column>
        <el-table-column label="参会部门" min-width="180">
          <template #default="{ row }">{{ row.participantDepartments || row.participantDeptName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><StatusBadge :status="row.status" type="topic" /></template>
        </el-table-column>
        <el-table-column label="操作" width="390" fixed="right" align="center">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button v-if="canSelectReporter(row)" size="small" link type="primary" @click="openSelector(row, 'REPORT')">选择汇报人</el-button>
              <el-button v-if="canSelectParticipant(row)" size="small" link type="primary" @click="openSelector(row, 'PARTAKE')">选择参会人</el-button>
              <el-button v-if="canShare(row)" size="small" link type="primary" :icon="Share" :loading="sharing" @click="openShareSelector(row)">分享</el-button>
              <el-button v-if="pendingNotifyIds(row).length" size="small" link type="primary" :loading="notifying" @click="notifyTopicPending(row)">通知参会人</el-button>
              <el-button v-if="canViewTopic(row)" size="small" link type="primary" @click="showTopic(row)">查看</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <PersonListDrawer v-model="attendeeVisible" :title="attendeeDrawerTitle" :people="visibleAttendees" />
    <PersonSelectDialog
      v-model="selectorVisible"
      :title="selectorTitle"
      :departments="visibleDepartments"
      :users="candidateUsers"
      :selected-ids="selectedIds"
      :default-department="defaultDepartment"
      :loading="submitting || sharing"
      :hint="selectionHint"
      :confirm-text="selectorMode === 'SHARE' ? '发送分享' : '保存'"
      @confirm="submitSelection"
    />

    <el-dialog v-model="topicDialogVisible" :title="topicDialogTitle" width="760px" destroy-on-close>
      <el-form v-if="topicDraft" :model="topicDraft" label-position="top">
        <el-form-item label="议题名称">
          <el-input v-model="topicDraft.title" :disabled="!isAdmin" />
        </el-form-item>
        <el-form-item label="议题类型">
          <el-input v-model="topicDraft.topicType" :disabled="!isAdmin" />
        </el-form-item>
        <el-form-item label="会议纪要">
          <el-input v-model="topicDraft.summary" type="textarea" :rows="7" :readonly="!isAdmin" resize="vertical" class="topic-summary-input" />
        </el-form-item>
        <el-form-item label="会议结论">
          <el-input v-model="topicDraft.conclusion" type="textarea" :rows="4" :disabled="!isAdmin" resize="none" />
        </el-form-item>
        <div class="topic-people-panel">
          <section>
            <h3>汇报人</h3>
            <div v-if="activeReporters.length" class="topic-person-list">
              <div v-for="person in activeReporters" :key="`report-${personKey(person)}`" class="topic-person-row">
                <strong>{{ personName(person) }}</strong>
                <span>{{ personDepartment(person) }}</span>
              </div>
            </div>
            <el-empty v-else description="暂无汇报人" :image-size="54" />
          </section>
          <section>
            <h3>参会人</h3>
            <div v-if="activeParticipants.length" class="topic-person-list">
              <div v-for="person in activeParticipants" :key="`partake-${personKey(person)}`" class="topic-person-row">
                <strong>{{ personName(person) }}</strong>
                <span>{{ personDepartment(person) }}</span>
              </div>
            </div>
            <el-empty v-else description="暂无参会人" :image-size="54" />
          </section>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="topicDialogVisible = false">关闭</el-button>
        <el-button v-if="isAdmin" type="primary" :loading="savingTopic" @click="saveTopic">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Bell, Edit, Refresh, Share, View } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import PersonListDrawer from '@/components/meeting/PersonListDrawer.vue'
import PersonSelectDialog from '@/components/meeting/PersonSelectDialog.vue'
import { api, DepartmentItem, MeetingItem, TopicItem, UserItem } from '@/api/meeting'

interface PendingNotifyPerson {
  userId: string
  userName: string
  employeeNo?: string
  departmentName?: string
  attendeeType: 'REPORT' | 'PARTAKE'
  topicId: number
  topicTitle: string
  topicSortNo?: number
}

const route = useRoute()
const store = useStore()
const meetingId = computed(() => Number(route.params.id))
const meeting = ref<MeetingItem | null>(null)
const users = ref<UserItem[]>([])
const departments = ref<DepartmentItem[]>([])
const loading = ref(false)
const notifying = ref(false)
const submitting = ref(false)
const sharing = ref(false)
const savingTopic = ref(false)
const attendeeVisible = ref(false)
const selectorVisible = ref(false)
const topicDialogVisible = ref(false)
const activeTopic = ref<TopicItem | null>(null)
const activeAction = ref<'REPORT' | 'PARTAKE'>('REPORT')
const selectorMode = ref<'ATTENDEE' | 'SHARE'>('ATTENDEE')
const selectedIds = ref<string[]>([])
const previousSelectionIds = ref<string[]>([])
const defaultDepartment = ref('')
const topicDraft = ref<{ title: string; topicType: string; summary: string; conclusion: string } | null>(null)
const pendingNotifyByTopic = ref<Record<string, PendingNotifyPerson[]>>({})

const currentUser = computed(() => store.state.user)
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
const topics = computed<TopicItem[]>(() => meeting.value?.topics || [])
const attendees = computed<UserItem[]>(() => meeting.value?.attendees || [])
const currentDepartmentId = computed(() => String(currentUser.value?.departmentId || currentUser.value?.department_id || currentUser.value?.deptId || ''))
const currentUserId = computed(() => String(currentUser.value?.userId || currentUser.value?.user_id || currentUser.value?.id || currentUser.value?.employeeNo || ''))

const visibleAttendees = computed<UserItem[]>(() => {
  const normalized = attendees.value.map(normalizeUser)
  if (isAdmin.value || !currentDepartmentId.value) return normalized
  return normalized.filter((person) => person.departmentId === currentDepartmentId.value)
})

const attendeeDrawerTitle = computed(() => (isAdmin.value ? '本次会议参会人员' : '本部门参会人员'))
const detailTitle = computed(() => {
  if (!meeting.value) return '会议详情'
  return `${meeting.value.meetingDate || ''} ${meeting.value.periodNo || ''} 三重一大会议`.trim()
})
const subtitle = computed(() => (meeting.value ? `${meeting.value.meetingTime || ''} · ${meeting.value.location || ''}` : ''))
const selectorTitle = computed(() => {
  if (selectorMode.value === 'SHARE') return '分享议题'
  return activeAction.value === 'REPORT' ? '选择汇报人' : '选择参会人'
})
const selectionHint = computed(() => {
  if (selectorMode.value === 'SHARE') return '分享只发送处理通知，不会加入正式参会名单'
  return activeAction.value === "REPORT" ? "\u6c47\u62a5\u4eba\u5fc5\u987b\u5c5e\u4e8e\u6c47\u62a5\u90e8\u95e8" : "\u53c2\u4f1a\u4eba\u5fc5\u987b\u5c5e\u4e8e\u53c2\u4f1a\u90e8\u95e8"
})
const canNotifyAll = computed(() => pendingNotifyTopicIds().length > 0)
const activeReporters = computed(() => (activeTopic.value ? topicPeople(activeTopic.value, 'REPORT') : []))
const activeParticipants = computed(() => (activeTopic.value ? topicPeople(activeTopic.value, 'PARTAKE') : []))

const allowedDepartmentIds = computed(() => {
  const topic = activeTopic.value
  if (!topic) return [] as string[]
  const reportIds = normalizeIds(topic.reportDepartmentIds || topic.reportDepartmentId)
  const participantIds = normalizeIds(topic.participantDepartmentIds || topic.participantDeptId)
  if (selectorMode.value === 'SHARE') {
    return currentDepartmentId.value ? [currentDepartmentId.value] : []
  }
  if (isAdmin.value) {
    return activeAction.value === 'REPORT' ? reportIds : participantIds
  }
  if (activeAction.value === 'REPORT' && reportIds.includes(currentDepartmentId.value)) return [currentDepartmentId.value]
  if (activeAction.value === 'PARTAKE' && participantIds.includes(currentDepartmentId.value)) return [currentDepartmentId.value]
  return []
})

const candidateUsers = computed(() => {
  const allowed = allowedDepartmentIds.value
  return users.value.filter((user) => !allowed.length || allowed.includes(user.departmentId || ''))
})

const visibleDepartments = computed(() => {
  const allowed = allowedDepartmentIds.value
  return departments.value.filter((department) => {
    if (!allowed.length) return true
    return allowed.includes(String(department.id || department.deptId || ''))
  })
})

async function load() {
  loading.value = true
  try {
    const [meetingData, userData, departmentData] = await Promise.all([api.meetingDetail(meetingId.value), api.users(), api.departments()])
    meeting.value = normalizeMeeting(meetingData as MeetingItem)
    users.value = (userData as UserItem[]).map(normalizeUser)
    departments.value = (departmentData as DepartmentItem[]).map((department: any) => ({
      ...department,
      id: String(department.id || department.deptId || department.dept_id),
      deptId: String(department.deptId || department.dept_id || department.id)
    }))
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function normalizeMeeting(data: MeetingItem) {
  return {
    ...data,
    topics: (data.topics || []).map((topic: any) => ({
      ...topic,
      reportDepartmentIds: normalizeIds(topic.reportDepartmentIds || topic.reportDepartmentId),
      participantDepartmentIds: normalizeIds(topic.participantDepartmentIds || topic.participantDeptId),
      attendees: (topic.attendees || []).map(normalizeAttendee)
    }))
  }
}

function normalizeAttendee(user: any) {
  return normalizeUser(user)
}

function normalizeUser(user: any) {
  return {
    ...user,
    id: String(user.id || user.userId || user.user_id),
    userId: String(user.userId || user.user_id || user.id),
    username: user.username || user.realName || user.real_name,
    employeeNo: user.employeeNo || user.employee_no,
    realName: user.realName || user.real_name || user.username,
    departmentId: user.departmentId || user.department_id,
    departmentName: user.departmentName || user.department_name,
    attendeeType: user.attendeeType || user.attendee_type
  }
}

function canSelectReporter(topic: TopicItem) {
  const reportIds = normalizeIds(topic.reportDepartmentIds || topic.reportDepartmentId)
  if ((topic as any).canSelectReporter !== undefined) return Boolean((topic as any).canSelectReporter)
  if (isAdmin.value) return reportIds.length > 0
  return currentUser.value?.role === 'SECRETARY' && reportIds.includes(currentDepartmentId.value)
}

function canSelectParticipant(topic: TopicItem) {
  const participantIds = normalizeIds(topic.participantDepartmentIds || topic.participantDeptId)
  if ((topic as any).canSelectParticipant !== undefined) return Boolean((topic as any).canSelectParticipant)
  if (isAdmin.value) return participantIds.length > 0
  if (currentUser.value?.role === 'SECRETARY') {
    return participantIds.includes(currentDepartmentId.value)
  }
  if (currentUser.value?.role === 'LEADER') {
    return topicPeople(topic, 'PARTAKE').some((person) => String(person.userId || person.id) === currentUserId.value)
  }
  return false
}

function canShare(topic: TopicItem) {
  if ((topic as any).canShareTopic !== undefined) return Boolean((topic as any).canShareTopic)
  return !isAdmin.value && currentUser.value?.role === 'SECRETARY' && (canSelectReporter(topic) || canSelectParticipant(topic))
}

function canViewTopic(topic: TopicItem) {
  if (isAdmin.value) return true
  if (!currentDepartmentId.value) return false
  const reportIds = normalizeIds(topic.reportDepartmentIds || topic.reportDepartmentId)
  const participantIds = normalizeIds(topic.participantDepartmentIds || topic.participantDeptId)
  return reportIds.includes(currentDepartmentId.value) || participantIds.includes(currentDepartmentId.value)
}

function openSelector(topic: TopicItem, action: 'REPORT' | 'PARTAKE') {
  activeTopic.value = topic
  activeAction.value = action
  selectorMode.value = 'ATTENDEE'
  defaultDepartment.value = allowedDepartmentIds.value[0] || currentDepartmentId.value
  selectedIds.value = topicPeople(topic, action).map((item: any) => String(item.userId || item.id))
  previousSelectionIds.value = [...selectedIds.value]
  selectorVisible.value = true
}

function openShareSelector(topic: TopicItem) {
  activeTopic.value = topic
  selectorMode.value = 'SHARE'
  defaultDepartment.value = currentDepartmentId.value
  selectedIds.value = []
  previousSelectionIds.value = []
  selectorVisible.value = true
}

async function submitSelection(ids: string[]) {
  if (!activeTopic.value) return
  if (selectorMode.value === 'SHARE') {
    await submitShare(ids)
    return
  }
  submitting.value = true
  try {
    await api.submitAttendees(activeTopic.value.id as number, { userIds: ids, attendeeType: activeAction.value })
    const addedIds = uniqueIds(ids).filter((id) => !previousSelectionIds.value.includes(id))
    if (addedIds.length) {
      mergePendingNotifyItems(activeTopic.value, activeAction.value, addedIds)
    }
    ElMessage.success('人员已保存')
    selectorVisible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

async function submitShare(ids: string[]) {
  if (!activeTopic.value) return
  const shareUserIds = uniqueIds(ids)
  if (!shareUserIds.length) {
    ElMessage.warning('请至少选择一名分享人员')
    return
  }
  sharing.value = true
  try {
    const result = await api.notifyTopicPreparation(activeTopic.value.id as number, { shareUserIds }) as any
    ElMessage.success(`已分享给 ${result.sharedCount || shareUserIds.length} 人`)
    selectorVisible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error.message || '分享失败')
  } finally {
    sharing.value = false
  }
}

function pendingNotifyIds(topic: TopicItem) {
  return pendingNotifyItems(topic).map((item) => item.userId)
}

function pendingNotifyItems(topic: TopicItem) {
  return pendingNotifyByTopic.value[String(topic.id)] || []
}

function pendingNotifyTopicIds() {
  return Object.keys(pendingNotifyByTopic.value).filter((topicId) => pendingNotifyByTopic.value[topicId]?.length)
}

async function notifyTopicPending(topic: TopicItem) {
  const topicId = Number(topic.id)
  const pendingItems = pendingNotifyItems(topic)
  const attendeeUserIds = pendingItems.map((item) => item.userId)
  if (!topicId || attendeeUserIds.length === 0) return
  try {
    await confirmNotify('确认通知参会人', pendingItems)
    notifying.value = true
    const result = await api.notifyTopicPreparation(topicId, { attendeeUserIds }) as any
    delete pendingNotifyByTopic.value[String(topicId)]
    ElMessage.success(`已通知 ${result.notifiedCount || 0} 人`)
    await load()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.message || '通知失败')
  } finally {
    notifying.value = false
  }
}

async function notifyAllPending() {
  const entries = pendingNotifyTopicIds().map((topicId) => ({
    topicId: Number(topicId),
    items: pendingNotifyByTopic.value[topicId]
  }))
  if (entries.length === 0) return
  try {
    await confirmNotify('确认通知参会人员', entries.flatMap((entry) => entry.items))
    notifying.value = true
    let total = 0
    for (const entry of entries) {
      const attendeeUserIds = entry.items.map((item) => item.userId)
      const result = await api.notifyTopicPreparation(entry.topicId, { attendeeUserIds }) as any
      total += Number(result.notifiedCount || 0)
      delete pendingNotifyByTopic.value[String(entry.topicId)]
    }
    ElMessage.success(`已通知 ${total} 人`)
    await load()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error(error.message || '通知失败')
  } finally {
    notifying.value = false
  }
}

function uniqueIds(ids: string[]) {
  return Array.from(new Set((ids || []).map((id) => String(id)).filter(Boolean)))
}

function mergePendingNotifyItems(topic: TopicItem, attendeeType: 'REPORT' | 'PARTAKE', userIds: string[]) {
  const topicId = Number(topic.id)
  const topicKey = String(topicId)
  const current = pendingNotifyByTopic.value[topicKey] || []
  const next = [...current]
  userIds.forEach((userId) => {
    const keyExists = next.some((item) => item.userId === userId && item.attendeeType === attendeeType)
    if (!keyExists) {
      next.push(buildPendingNotifyItem(topic, attendeeType, userId))
    }
  })
  pendingNotifyByTopic.value[topicKey] = next
}

function buildPendingNotifyItem(topic: TopicItem, attendeeType: 'REPORT' | 'PARTAKE', userId: string): PendingNotifyPerson {
  const user = users.value.find((item) => String(item.userId || item.id) === userId)
  return {
    userId,
    userName: user?.realName || user?.username || userId,
    employeeNo: user?.employeeNo,
    departmentName: user?.departmentName,
    attendeeType,
    topicId: Number(topic.id),
    topicTitle: topic.title || '-',
    topicSortNo: topic.sortNo
  }
}

function confirmNotify(title: string, items: PendingNotifyPerson[]) {
  return ElMessageBox.confirm(notifyConfirmHtml(items), title, {
    confirmButtonText: '发送消息',
    cancelButtonText: '取消',
    type: 'warning',
    customClass: 'notify-message-box',
    dangerouslyUseHTMLString: true
  })
}

function notifyConfirmHtml(items: PendingNotifyPerson[]) {
  const topicGroups = groupPendingNotifyItems(items)
  const cards = topicGroups.map((group) => {
    const people = group.items.map((item) => `
      <div style="display:flex;align-items:center;gap:12px;padding:12px 0;border-top:1px solid #eef2f7;">
        <span style="display:inline-flex;align-items:center;justify-content:center;width:56px;height:26px;border-radius:999px;background:${item.attendeeType === 'REPORT' ? '#eff6ff' : '#f0fdf4'};color:${item.attendeeType === 'REPORT' ? '#2563eb' : '#16a34a'};font-size:12px;font-weight:600;flex:0 0 auto;">${escapeHtml(attendeeTypeLabel(item.attendeeType))}</span>
        <div style="min-width:0;flex:1;">
          <div style="font-weight:600;color:#111827;line-height:1.4;">${escapeHtml(item.userName)}</div>
          <div style="margin-top:2px;color:#667085;font-size:12px;line-height:1.4;">${escapeHtml(item.departmentName || '-')} / ${escapeHtml(item.employeeNo || '-')}</div>
        </div>
      </div>
    `).join('')
    return `
      <section style="width:100%;box-sizing:border-box;border:1px solid #e5e7eb;border-radius:8px;background:#fff;margin-top:12px;overflow:hidden;">
        <header style="padding:12px 14px;background:#f8fafc;border-bottom:1px solid #e5e7eb;">
          <div style="display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:start;gap:8px;min-width:0;">
            <strong style="color:#111827;white-space:nowrap;line-height:1.6;">\u7b2c ${escapeHtml(String(group.topicSortNo || '-'))} \u9879</strong>
            <span style="color:#475467;line-height:1.6;white-space:normal;word-break:break-word;">${escapeHtml(group.topicTitle)}</span>
            <span style="margin-left:auto;color:#98a2b3;font-size:12px;white-space:nowrap;">${group.items.length} \u4eba</span>
          </div>
        </header>
        <div style="padding:0 14px;">${people}</div>
      </section>
    `
  }).join('')
  return `
    <div class="notify-confirm" style="width:100%;max-height:min(56vh,460px);overflow-y:auto;overflow-x:hidden;box-sizing:border-box;padding-right:2px;">
      <p style="margin:0;padding:10px 12px;border-radius:8px;background:#f8fafc;color:#475467;line-height:1.6;">\u786e\u8ba4\u5411\u4ee5\u4e0b <strong style="color:#111827;">${items.length}</strong> \u4eba\u53d1\u9001\u6d88\u606f\uff1f</p>
      ${cards}
    </div>
  `
}

function groupPendingNotifyItems(items: PendingNotifyPerson[]) {
  const groups: Array<{ topicId: number; topicTitle: string; topicSortNo?: number; items: PendingNotifyPerson[] }> = []
  items.forEach((item) => {
    let group = groups.find((entry) => entry.topicId === item.topicId)
    if (!group) {
      group = {
        topicId: item.topicId,
        topicTitle: item.topicTitle,
        topicSortNo: item.topicSortNo,
        items: []
      }
      groups.push(group)
    }
    group.items.push(item)
  })
  return groups
}

function attendeeTypeLabel(type: 'REPORT' | 'PARTAKE') {
  return type === 'REPORT' ? '\u6c47\u62a5\u4eba' : '\u53c2\u4f1a\u4eba'
}
function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

function topicPeople(topic: TopicItem, type: 'REPORT' | 'PARTAKE') {
  const items = ((topic as any).attendees || []) as Array<UserItem & { attendeeType?: string; attendee_type?: string }>
  return items.filter((item) => {
    const itemType = item.attendeeType || item.attendee_type
    return itemType === type || (type === 'PARTAKE' && itemType === 'PARTICIPANT')
  })
}

function personKey(person: any) {
  return String(person.userId || person.user_id || person.id || person.employeeNo || person.employee_no || personName(person))
}

function personName(person: any) {
  return person.realName || person.real_name || person.userName || person.user_name || person.username || person.name || person.id || '-'
}

function personDepartment(person: any) {
  return person.departmentName || person.department_name || person.deptName || person.dept_name || '-'
}

function showTopic(topic: TopicItem) {
  activeTopic.value = topic
  topicDraft.value = {
    title: topic.title || '',
    topicType: topic.topicType || '三重一大',
    summary: topic.summary || '',
    conclusion: topic.conclusion || ''
  }
  topicDialogVisible.value = true
}

async function saveTopic() {
  if (!activeTopic.value || !topicDraft.value) return
  savingTopic.value = true
  try {
    const updated = await api.updateTopic(activeTopic.value.id as number, {
      title: topicDraft.value.title,
      topicType: topicDraft.value.topicType,
      summary: topicDraft.value.summary,
      conclusion: topicDraft.value.conclusion
    })
    const next = updated as TopicItem
    activeTopic.value = {
      ...activeTopic.value,
      ...next
    }
    ElMessage.success('议题已保存')
    topicDialogVisible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    savingTopic.value = false
  }
}

function normalizeIds(value: unknown): string[] {
  if (Array.isArray(value)) return value.map((item) => String(item)).filter(Boolean)
  if (typeof value === 'string') return value.split(',').map((item) => item.trim()).filter(Boolean)
  return []
}

function pad(value?: number) {
  return String(value || 0).padStart(2, '0')
}

onMounted(load)
</script>

<style scoped>
.info-panel,
.topic-panel {
  margin-bottom: 18px;
  padding: 20px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 18px;
}

h2 {
  margin: 0;
  font-size: 17px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 20px;
}

.info-grid span,
.content-box span {
  display: block;
  margin-bottom: 6px;
  color: #98a2b3;
  font-size: 12px;
}

.info-grid strong {
  color: #111827;
}

.content-box {
  margin: 20px 0 8px;
  padding-top: 18px;
  border-top: 1px solid #edf1f7;
}

.content-box p {
  margin: 0;
  color: #475467;
  line-height: 1.7;
}

.topic-summary {
  margin: 6px 0 0;
  color: #667085;
  font-size: 12px;
}

.topic-summary-input :deep(.el-textarea__inner) {
  min-height: 168px;
  line-height: 1.6;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.topic-people-panel {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.topic-people-panel section {
  min-width: 0;
  padding: 12px;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.topic-people-panel h3 {
  margin: 0 0 10px;
  color: #111827;
  font-size: 14px;
}

.topic-person-list {
  display: grid;
  gap: 8px;
  max-height: 180px;
  overflow: auto;
}

.topic-person-row {
  display: grid;
  grid-template-columns: minmax(80px, 0.8fr) minmax(0, 1.2fr);
  gap: 8px;
  padding: 8px 9px;
  background: #fff;
  border: 1px solid #edf1f7;
  border-radius: 7px;
}

.topic-person-row strong,
.topic-person-row span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-person-row strong {
  color: #172033;
  font-size: 13px;
}

.topic-person-row span {
  color: #667085;
  font-size: 12px;
}

:global(.notify-message-box) {
  width: min(640px, calc(100vw - 32px));
}

:global(.notify-message-box .el-message-box__content) {
  align-items: flex-start;
  padding: 10px 24px 8px;
}

:global(.notify-message-box .el-message-box__status) {
  margin-top: 18px;
}

:global(.notify-message-box .el-message-box__message) {
  width: 100%;
  min-width: 0;
}

:global(.notify-message-box .el-message-box__message p) {
  margin: 0;
}

:global(.notify-message-box .el-message-box__btns) {
  padding: 12px 24px 0;
}

@media (max-width: 900px) {
  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 600px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .topic-people-panel {
    grid-template-columns: 1fr;
  }
}
</style>
