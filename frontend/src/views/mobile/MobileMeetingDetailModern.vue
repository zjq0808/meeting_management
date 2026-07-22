<template>
  <div class="mobile-page">
    <van-nav-bar title="会议详情" left-arrow @click-left="$router.back()" />

    <main class="mobile-content">
      <section v-if="meeting" class="mobile-card meeting-summary">
        <div class="detail-title">
          <h2>{{ meeting.periodNo || '未填写期数' }} 三重一大会议</h2>
          <van-tag :type="tagType(meeting.status)">{{ statusLabel(meeting.status) }}</van-tag>
        </div>
        <div class="detail-grid">
          <span>日期<strong>{{ meeting.meetingDate || '-' }}</strong></span>
          <span>时间<strong>{{ meeting.meetingTime || '-' }}</strong></span>
          <span>地点<strong>{{ meeting.location || '-' }}</strong></span>
          <span>议题<strong>{{ topics.length }} 项</strong></span>
        </div>
        <p class="content">{{ meeting.content || '暂无会议内容' }}</p>
        <div class="detail-actions">
          <van-button plain type="primary" class="people-button" @click="peopleVisible = true">查看参会人员（{{ attendees.length }} 人）</van-button>
          <van-button v-if="isAdmin" type="primary" class="people-button" @click="$router.push(`/mobile/meetings/${meetingId}/console`)">进入控制台</van-button>
          <van-button v-if="canNotifyAll" plain type="primary" class="people-button notify-button" :loading="notifying" @click="notifyAllPending">通知参会人员</van-button>
        </div>
      </section>

      <section class="topic-list">
        <h3>参会议题信息</h3>
        <article v-for="topic in topics" :key="topic.id" class="mobile-card topic-card">
          <div class="topic-head">
            <span>{{ pad(topic.sortNo) }}</span>
            <div>
              <h4>{{ topic.title }}</h4>
              <em>{{ topic.topicType || '未填写类型' }}</em>
            </div>
          </div>
          <div class="topic-meta">
            <span>汇报部门</span>
            <strong>{{ topic.reportDepartmentName || '-' }}</strong>
            <span>参会部门</span>
            <strong>{{ topic.participantDepartments || topic.participantDeptName || '-' }}</strong>
          </div>
          <div class="topic-actions">
            <van-button v-if="canSelectReporter(topic)" size="small" type="primary" @click="openSelector(topic, 'REPORT')">选择汇报人</van-button>
            <van-button v-if="canSelectParticipant(topic)" size="small" plain type="primary" @click="openSelector(topic, 'PARTAKE')">选择参会人</van-button>
            <van-button v-if="pendingNotifyIds(topic).length" size="small" plain type="primary" :loading="notifying" @click="notifyTopicPending(topic)">通知参会人</van-button>
            <van-button v-if="canViewTopic(topic)" size="small" plain @click="showConclusion(topic)">查看</van-button>
          </div>
        </article>
      </section>
    </main>

    <van-popup v-model:show="peopleVisible" round position="bottom" closeable class="mobile-bottom-popup">
      <div class="popup-panel">
        <h3>本次会议参会人员</h3>
        <div class="popup-scroll">
          <van-cell v-for="person in attendees" :key="person.id" :title="person.realName || person.username" :label="`${person.departmentName || '-'} / ${person.employeeNo || person.id}`" />
          <van-empty v-if="attendees.length === 0" description="暂无人员" />
        </div>
      </div>
    </van-popup>

    <van-popup v-model:show="selectorVisible" round position="bottom" closeable class="mobile-bottom-popup">
      <div class="popup-panel">
        <h3>{{ selectorTitle }}</h3>
        <p class="selector-tip">{{ activeType === "REPORT" ? "\u6c47\u62a5\u4eba\u5fc5\u987b\u5c5e\u4e8e\u6c47\u62a5\u90e8\u95e8" : "\u53c2\u4f1a\u4eba\u5fc5\u987b\u5c5e\u4e8e\u53c2\u4f1a\u90e8\u95e8" }}</p>
        <van-search v-model="keyword" placeholder="搜索姓名或工号" />
        <div class="popup-scroll selector-scroll">
          <van-checkbox-group v-model="selectedIds">
            <van-cell-group inset>
              <van-cell
                v-for="user in filteredUsers"
                :key="user.id"
                clickable
                :title="user.realName || user.username"
                :label="`${user.departmentName || '-'} / ${user.employeeNo || user.id}`"
                @click="toggleUser(user.id)"
              >
                <template #right-icon><van-checkbox :name="user.id" /></template>
              </van-cell>
            </van-cell-group>
          </van-checkbox-group>
        </div>
        <div class="popup-footer">
          <van-button block type="primary" :loading="submitting" @click="submitSelection">保存</van-button>
        </div>
      </div>
    </van-popup>

    <van-dialog v-model:show="conclusionVisible" title="议题详情" confirm-button-text="关闭" class-name="topic-detail-dialog">
      <div class="dialog-content">
        <template v-if="isAdmin && topicDraft">
          <div class="topic-edit-form">
            <label>议题名称</label>
            <van-field v-model="topicDraft.title" placeholder="请输入议题名称" />
            <label>议题类型</label>
            <van-field v-model="topicDraft.topicType" placeholder="请输入议题类型" />
            <label>会议纪要</label>
            <van-field v-model="topicDraft.summary" type="textarea" rows="5" autosize placeholder="请输入会议纪要" />
            <label>会议结论</label>
            <van-field v-model="topicDraft.conclusion" type="textarea" rows="4" autosize placeholder="请输入会议结论" />
            <van-button block type="primary" :loading="savingTopic" class="topic-save-button" @click="saveTopic">保存</van-button>
          </div>
        </template>
        <template v-else>
          <h4>{{ activeConclusion?.title || '-' }}</h4>
          <p>议题类型：{{ activeConclusion?.topicType || '-' }}</p>
          <p>会议纪要：{{ activeConclusion?.summary || '暂无会议纪要' }}</p>
          <p>会议结论：{{ activeConclusion?.conclusion || '暂未录入会议结论' }}</p>
        </template>
        <section class="dialog-people-block">
          <h5>汇报人</h5>
          <div v-if="activeReporters.length" class="dialog-person-list">
            <div v-for="person in activeReporters" :key="`report-${personKey(person)}`">
              <strong>{{ personName(person) }}</strong>
              <span>{{ personDepartment(person) }}</span>
            </div>
          </div>
          <van-empty v-else image-size="54" description="暂无汇报人" />
        </section>
        <section class="dialog-people-block">
          <h5>参会人</h5>
          <div v-if="activeParticipants.length" class="dialog-person-list">
            <div v-for="person in activeParticipants" :key="`partake-${personKey(person)}`">
              <strong>{{ personName(person) }}</strong>
              <span>{{ personDepartment(person) }}</span>
            </div>
          </div>
          <van-empty v-else image-size="54" description="暂无参会人" />
        </section>
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showConfirmDialog, showFailToast, showSuccessToast } from 'vant'
import { useStore } from 'vuex'
import { api, MeetingItem, TopicItem, UserItem } from '@/api/meeting'

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
const peopleVisible = ref(false)
const selectorVisible = ref(false)
const conclusionVisible = ref(false)
const submitting = ref(false)
const notifying = ref(false)
const savingTopic = ref(false)
const activeTopic = ref<TopicItem | null>(null)
const activeType = ref<'REPORT' | 'PARTAKE'>('PARTAKE')
const selectedIds = ref<string[]>([])
const previousSelectionIds = ref<string[]>([])
const keyword = ref('')
const activeConclusion = ref<TopicItem | null>(null)
const topicDraft = ref<{ title: string; topicType: string; summary: string; conclusion: string } | null>(null)
const pendingNotifyByTopic = ref<Record<string, PendingNotifyPerson[]>>({})
const currentUser = computed(() => store.state.user)
const topics = computed<TopicItem[]>(() => meeting.value?.topics || [])
const attendees = computed<UserItem[]>(() => meeting.value?.attendees || [])
const selectorTitle = computed(() => (activeType.value === 'REPORT' ? '选择汇报人' : '选择参会人'))
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
const canNotifyAll = computed(() => pendingNotifyTopicIds().length > 0)
const activeReporters = computed(() => (activeConclusion.value ? topicPeople(activeConclusion.value, 'REPORT') : []))
const activeParticipants = computed(() => (activeConclusion.value ? topicPeople(activeConclusion.value, 'PARTAKE') : []))

const allowedDepartmentIds = computed(() => {
  if (!activeTopic.value) return [] as string[]
  const reportIds = normalizeIds(activeTopic.value.reportDepartmentIds || activeTopic.value.reportDepartmentId)
  const participantIds = normalizeIds(activeTopic.value.participantDepartmentIds || activeTopic.value.participantDeptId)
  if (currentUser.value?.role === 'ADMIN') {
    return activeType.value === 'REPORT' ? reportIds : participantIds
  }
  const departmentId = String(currentUser.value?.departmentId || currentUser.value?.department_id || currentUser.value?.deptId || '')
  if (activeType.value === 'REPORT' && reportIds.includes(departmentId)) return [departmentId]
  if (activeType.value === 'PARTAKE' && participantIds.includes(departmentId)) return [departmentId]
  return []
})

const filteredUsers = computed(() => {
  const key = keyword.value.trim()
  const allowed = allowedDepartmentIds.value
  return users.value.filter((user) => {
    if (allowed.length && !allowed.includes(user.departmentId || '')) return false
    if (key && !`${user.username || ''}${user.realName || ''}${user.employeeNo || ''}${user.id}`.includes(key)) return false
    return true
  })
})

async function load() {
  try {
    const [detail, userData] = await Promise.all([api.meetingDetail(meetingId.value), api.users()])
    meeting.value = normalizeMeeting(detail as MeetingItem)
    users.value = (userData as UserItem[]).map(normalizeUser)
  } catch (error: any) {
    showFailToast(error.message || '加载失败')
  }
}

function normalizeMeeting(data: MeetingItem) {
  return {
    ...data,
    attendees: (data.attendees || []).map(normalizeUser),
    topics: (data.topics || []).map((topic: any) => ({
      ...topic,
      reportDepartmentIds: normalizeIds(topic.reportDepartmentIds || topic.reportDepartmentId),
      participantDepartmentIds: normalizeIds(topic.participantDepartmentIds || topic.participantDeptId),
      attendees: (topic.attendees || []).map(normalizeUser)
    }))
  }
}

function canSelectReporter(topic: TopicItem) {
  const reportIds = normalizeIds(topic.reportDepartmentIds || topic.reportDepartmentId)
  if (currentUser.value?.role === 'ADMIN') return reportIds.length > 0
  const departmentId = String(currentUser.value?.departmentId || currentUser.value?.department_id || currentUser.value?.deptId || '')
  return currentUser.value?.role === 'SECRETARY' && reportIds.includes(departmentId)
}

function canSelectParticipant(topic: TopicItem) {
  const participantIds = normalizeIds(topic.participantDepartmentIds || topic.participantDeptId)
  if (currentUser.value?.role === 'ADMIN') return participantIds.length > 0
  const departmentId = String(currentUser.value?.departmentId || currentUser.value?.department_id || currentUser.value?.deptId || '')
  if (currentUser.value?.role === 'SECRETARY') return participantIds.includes(departmentId)
  if (currentUser.value?.role === 'LEADER') {
    return topicPeople(topic, 'PARTAKE').some((person) => String(person.userId || person.id) === currentUserId())
  }
  return false
}

function canViewTopic(topic: TopicItem) {
  if (isAdmin.value) return true
  const departmentId = currentDepartmentId()
  if (!departmentId) return false
  const reportIds = normalizeIds(topic.reportDepartmentIds || topic.reportDepartmentId)
  const participantIds = normalizeIds(topic.participantDepartmentIds || topic.participantDeptId)
  return reportIds.includes(departmentId) || participantIds.includes(departmentId)
}

function openSelector(topic: TopicItem, type: 'REPORT' | 'PARTAKE') {
  activeTopic.value = topic
  activeType.value = type
  selectedIds.value = topicPeople(topic, type).map((item: any) => String(item.userId || item.id))
  previousSelectionIds.value = [...selectedIds.value]
  keyword.value = ''
  selectorVisible.value = true
}

function toggleUser(id: string) {
  const index = selectedIds.value.indexOf(id)
  if (index >= 0) selectedIds.value.splice(index, 1)
  else selectedIds.value.push(id)
}

async function submitSelection() {
  if (!activeTopic.value) return
  submitting.value = true
  try {
    const ids = uniqueIds(selectedIds.value)
    await api.submitAttendees(activeTopic.value.id as number, { userIds: ids, attendeeType: activeType.value })
    const addedIds = ids.filter((id) => !previousSelectionIds.value.includes(id))
    if (addedIds.length) {
      mergePendingNotifyItems(activeTopic.value, activeType.value, addedIds)
    }
    showSuccessToast('保存成功')
    selectorVisible.value = false
    await load()
  } catch (error: any) {
    showFailToast(error.message || '保存失败')
  } finally {
    submitting.value = false
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
    showSuccessToast(`已通知 ${result.notifiedCount || 0} 人`)
    await load()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    showFailToast(error.message || '通知失败')
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
    showSuccessToast(`已通知 ${total} 人`)
    await load()
  } catch (error: any) {
    if (error === 'cancel' || error === 'close') return
    showFailToast(error.message || '通知失败')
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
  return showConfirmDialog({
    title,
    message: notifyConfirmMessage(items),
    confirmButtonText: '发送消息',
    cancelButtonText: '取消',
    messageAlign: 'left'
  })
}

function notifyConfirmMessage(items: PendingNotifyPerson[]) {
  const groups = groupPendingNotifyItems(items)
  const lines = [`确认向以下 ${items.length} 人发送消息？`]
  groups.forEach((group) => {
    lines.push('')
    lines.push(`第 ${group.topicSortNo || '-'} 项：${group.topicTitle}`)
    group.items.forEach((item) => {
      lines.push(`${attendeeTypeLabel(item.attendeeType)}：${item.userName}（${item.departmentName || '-'} / ${item.employeeNo || '-'}）`)
    })
  })
  return lines.join('\n')
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
  return type === 'REPORT' ? '汇报人' : '参会人'
}

function topicPeople(topic: TopicItem, type: 'REPORT' | 'PARTAKE') {
  const attendees = ((topic as any).attendees || []) as Array<UserItem & { attendeeType?: string; attendee_type?: string }>
  return attendees.filter((item) => {
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

function showConclusion(topic: TopicItem) {
  activeConclusion.value = topic
  topicDraft.value = {
    title: topic.title || '',
    topicType: topic.topicType || '',
    summary: topic.summary || '',
    conclusion: topic.conclusion || ''
  }
  conclusionVisible.value = true
}

async function saveTopic() {
  if (!activeConclusion.value?.id || !topicDraft.value) return
  savingTopic.value = true
  try {
    const updated = await api.updateTopic(activeConclusion.value.id as number, {
      title: topicDraft.value.title,
      topicType: topicDraft.value.topicType,
      summary: topicDraft.value.summary,
      conclusion: topicDraft.value.conclusion
    }) as TopicItem
    activeConclusion.value = {
      ...activeConclusion.value,
      ...updated,
      title: topicDraft.value.title,
      topicType: topicDraft.value.topicType,
      summary: topicDraft.value.summary,
      conclusion: topicDraft.value.conclusion
    }
    showSuccessToast('议题已保存')
    await load()
  } catch (error: any) {
    showFailToast(error.message || '保存失败')
  } finally {
    savingTopic.value = false
  }
}

function currentUserId() {
  return String(currentUser.value?.userId || currentUser.value?.user_id || currentUser.value?.id || currentUser.value?.employeeNo || '')
}

function currentDepartmentId() {
  return String(currentUser.value?.departmentId || currentUser.value?.department_id || currentUser.value?.deptId || '')
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
    attendeeType: user.attendeeType || user.attendee_type,
    selectedSource: user.selectedSource || user.selected_source,
    selectedDeptId: user.selectedDeptId || user.selected_dept_id
  }
}

function normalizeIds(value: unknown): string[] {
  if (Array.isArray(value)) return value.map((item) => String(item)).filter(Boolean)
  if (typeof value === 'string') return value.split(',').map((item) => item.trim()).filter(Boolean)
  return []
}

function statusLabel(status: string) {
  const map: Record<string, string> = { DRAFT: '待发布', PUBLISHED: '待开始', IN_PROGRESS: '进行中', FINISHED: '已结束' }
  return map[status] || status
}

function tagType(status: string) {
  const map: Record<string, string> = { DRAFT: 'default', PUBLISHED: 'primary', IN_PROGRESS: 'success', FINISHED: 'default' }
  return map[status] || 'default'
}

function pad(value?: number) {
  return String(value || 0).padStart(2, '0')
}

onMounted(load)
</script>

<style scoped>
.detail-title {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

h2,
h3,
h4 {
  margin: 0;
}

.detail-title h2 {
  color: #111827;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.35;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.detail-grid span {
  display: grid;
  gap: 3px;
  padding: 8px 9px;
  color: #8a94a6;
  background: #f7f9fc;
  border: 1px solid #eef2f6;
  border-radius: 8px;
  font-size: 11px;
}

.detail-grid strong {
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content {
  display: -webkit-box;
  margin: 10px 0;
  overflow: hidden;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.detail-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.detail-actions .people-button:only-child {
  grid-column: 1 / -1;
}

.detail-actions .notify-button {
  grid-column: 1 / -1;
}

.people-button {
  height: 34px;
  border-radius: 7px;
}

.topic-list h3 {
  margin: 12px 2px 8px;
  color: #111827;
  font-size: 15px;
  font-weight: 800;
}

.topic-head {
  display: flex;
  gap: 9px;
}

.topic-head > span {
  display: grid;
  flex: none;
  place-items: center;
  width: 30px;
  height: 30px;
  color: #fff;
  font-weight: 800;
  background: #2f7df6;
  border-radius: 7px;
  font-size: 12px;
}

.topic-head h4 {
  color: #172033;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.35;
}

.topic-head em {
  display: block;
  margin-top: 3px;
  color: #98a2b3;
  font-size: 11px;
  font-style: normal;
}

.topic-meta {
  display: grid;
  grid-template-columns: 54px minmax(0, 1fr);
  gap: 5px 8px;
  margin-top: 10px;
  padding: 8px 9px;
  background: #f7f9fc;
  border: 1px solid #eef2f6;
  border-radius: 8px;
}

.topic-meta span {
  color: #8a94a6;
  font-size: 11px;
}

.topic-meta strong {
  overflow: hidden;
  color: #475467;
  font-size: 12px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 10px;
}

.mobile-bottom-popup {
  max-height: 82vh;
  overflow: hidden;
}

.popup-panel {
  display: flex;
  flex-direction: column;
  max-height: 82vh;
  padding: 16px 10px calc(16px + env(safe-area-inset-bottom));
  overflow: hidden;
}

.popup-scroll {
  min-height: 0;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.selector-scroll {
  max-height: 42vh;
}

.selector-tip {
  margin: 8px 0 12px;
  color: #f59e0b;
  font-size: 13px;
}

.popup-panel h3 {
  flex: none;
  margin: 0 44px 12px 4px;
  color: #111827;
  font-size: 18px;
  font-weight: 800;
  line-height: 1.35;
  word-break: break-word;
}

.popup-footer {
  flex: none;
  padding: 12px 6px 0;
  background: #fff;
}

.dialog-content {
  max-height: min(62vh, 520px);
  padding: 18px 24px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  color: #475467;
  line-height: 1.7;
}

.topic-edit-form {
  display: grid;
  gap: 8px;
}

.topic-edit-form label {
  color: #8a94a6;
  font-size: 12px;
  font-weight: 700;
}

.topic-edit-form :deep(.van-cell) {
  padding: 8px 10px;
  background: #f7f9fc;
  border: 1px solid #eef2f6;
  border-radius: 8px;
}

.topic-edit-form :deep(.van-field__control) {
  color: #172033;
  line-height: 1.55;
}

.topic-save-button {
  height: 34px;
  margin-top: 4px;
  border-radius: 8px;
  font-weight: 800;
}

:global(.topic-detail-dialog) {
  max-height: 82vh;
}

:global(.topic-detail-dialog .van-dialog__content) {
  max-height: calc(82vh - 104px);
  overflow: hidden;
}

:global(.topic-detail-dialog .van-dialog__footer) {
  position: sticky;
  bottom: 0;
  background: #fff;
}

:global(.van-dialog__message) {
  white-space: pre-line;
}

.dialog-people-block {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #eef2f6;
}

.dialog-people-block h5 {
  margin: 0 0 8px;
  color: #111827;
  font-size: 13px;
}

.dialog-person-list {
  display: grid;
  gap: 7px;
  max-height: 160px;
  overflow: auto;
}

.dialog-person-list div {
  display: grid;
  grid-template-columns: minmax(64px, 0.8fr) minmax(0, 1.2fr);
  gap: 8px;
  padding: 8px;
  background: #f7f9fc;
  border: 1px solid #eef2f6;
  border-radius: 7px;
}

.dialog-person-list strong,
.dialog-person-list span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dialog-person-list strong {
  color: #172033;
  font-size: 12px;
}

.dialog-person-list span {
  color: #667085;
  font-size: 12px;
}
</style>
