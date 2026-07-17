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
            <van-button size="small" plain @click="showConclusion(topic)">查看</van-button>
          </div>
        </article>
      </section>
    </main>

    <van-popup v-model:show="peopleVisible" round position="bottom" closeable>
      <div class="popup-panel">
        <h3>本次会议参会人员</h3>
        <van-cell v-for="person in attendees" :key="person.id" :title="person.realName || person.username" :label="`${person.departmentName || '-'} / ${person.employeeNo || person.id}`" />
        <van-empty v-if="attendees.length === 0" description="暂无人员" />
      </div>
    </van-popup>

    <van-popup v-model:show="selectorVisible" round position="bottom" closeable>
      <div class="popup-panel">
        <h3>{{ selectorTitle }}</h3>
        <p class="selector-tip">{{ activeType === "REPORT" ? "\u6c47\u62a5\u4eba\u5fc5\u987b\u5c5e\u4e8e\u6c47\u62a5\u90e8\u95e8" : "\u53c2\u4f1a\u4eba\u5fc5\u987b\u5c5e\u4e8e\u53c2\u4f1a\u90e8\u95e8" }}</p>
        <van-search v-model="keyword" placeholder="搜索姓名或工号" />
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
        <van-button block type="primary" :loading="submitting" @click="submitSelection">保存</van-button>
      </div>
    </van-popup>

    <van-dialog v-model:show="conclusionVisible" title="议题详情" confirm-button-text="关闭">
      <div class="dialog-content">
        <h4>{{ activeConclusion?.title || '-' }}</h4>
        <p>议题类型：{{ activeConclusion?.topicType || '-' }}</p>
        <p>会议纪要：{{ activeConclusion?.summary || '暂无会议纪要' }}</p>
        <p>会议结论：{{ activeConclusion?.conclusion || '暂未录入会议结论' }}</p>
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
import { showFailToast, showSuccessToast } from 'vant'
import { useStore } from 'vuex'
import { api, MeetingItem, TopicItem, UserItem } from '@/api/meeting'

const route = useRoute()
const store = useStore()
const meetingId = computed(() => Number(route.params.id))
const meeting = ref<MeetingItem | null>(null)
const users = ref<UserItem[]>([])
const peopleVisible = ref(false)
const selectorVisible = ref(false)
const conclusionVisible = ref(false)
const submitting = ref(false)
const activeTopic = ref<TopicItem | null>(null)
const activeType = ref<'REPORT' | 'PARTAKE'>('PARTAKE')
const selectedIds = ref<string[]>([])
const keyword = ref('')
const activeConclusion = ref<TopicItem | null>(null)
const currentUser = computed(() => store.state.user)
const topics = computed<TopicItem[]>(() => meeting.value?.topics || [])
const attendees = computed<UserItem[]>(() => meeting.value?.attendees || [])
const selectorTitle = computed(() => (activeType.value === 'REPORT' ? '选择汇报人' : '选择参会人'))
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
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
    topics: (data.topics || []).map((topic: any) => ({
      ...topic,
      reportDepartmentIds: normalizeIds(topic.reportDepartmentIds || topic.reportDepartmentId),
      participantDepartmentIds: normalizeIds(topic.participantDepartmentIds || topic.participantDeptId)
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

function openSelector(topic: TopicItem, type: 'REPORT' | 'PARTAKE') {
  activeTopic.value = topic
  activeType.value = type
  selectedIds.value = topicPeople(topic, type).map((item: any) => String(item.userId || item.id))
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
    await api.submitAttendees(activeTopic.value.id as number, { userIds: selectedIds.value, attendeeType: activeType.value })
    showSuccessToast('保存成功')
    selectorVisible.value = false
    await load()
  } catch (error: any) {
    showFailToast(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
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
  conclusionVisible.value = true
}

function currentUserId() {
  return String(currentUser.value?.userId || currentUser.value?.user_id || currentUser.value?.id || currentUser.value?.employeeNo || '')
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
    departmentName: user.departmentName || user.department_name
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

.popup-panel {
  max-height: 76vh;
  padding: 14px 10px 18px;
  overflow: auto;
}

.selector-tip {
  margin: 8px 0 12px;
  color: #f59e0b;
  font-size: 13px;
}

.popup-panel h3 {
  margin: 0 0 12px 4px;
}

.dialog-content {
  padding: 18px 24px;
  color: #475467;
  line-height: 1.7;
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
