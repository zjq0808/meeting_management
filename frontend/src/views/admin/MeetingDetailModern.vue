<template>
  <div class="page">
    <PageHeader :title="detailTitle" :subtitle="subtitle" back @back="$router.push('/meetings')">
      <template #actions>
        <el-button v-if="isAdmin" type="primary" :icon="Edit" @click="$router.push(`/meetings/${meetingId}/edit`)">编辑</el-button>
        <el-button v-if="canConfirmAttendees" type="primary" :loading="confirming" @click="confirmAttendees">确认参会人员</el-button>
        <el-button :icon="LinkIcon" @click="copyMobileLink">分享移动详情</el-button>
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
        <el-table-column label="序号" width="74" align="center"><template #default="{ row }">{{ pad(row.sortNo) }}</template></el-table-column>
        <el-table-column label="议题名称" min-width="280">
          <template #default="{ row }">
            <strong>{{ row.title }}</strong>
            <p class="topic-summary">{{ row.summary || '暂无简述' }}</p>
          </template>
        </el-table-column>
        <el-table-column label="汇报部门" width="160"><template #default="{ row }">{{ row.reportDepartmentName || '-' }}</template></el-table-column>
        <el-table-column label="参会部门" min-width="170"><template #default="{ row }">{{ row.participantDepartments || row.participantDeptName || '-' }}</template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }"><StatusBadge :status="row.status" type="topic" /></template></el-table-column>
        <el-table-column label="操作" width="330" fixed="right" align="center">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button v-if="canSelectReporter(row)" size="small" link type="primary" @click="openSelector(row, 'REPORT')">选择汇报人</el-button>
              <el-button v-if="canSelectParticipant(row)" size="small" link type="primary" @click="openSelector(row, 'PARTAKE')">选择参会人</el-button>
              <el-button size="small" link type="primary" @click="showConclusion(row)">查看结论</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section class="surface conclusion-panel">
      <h2>会议结论纪要</h2>
      <article v-for="topic in topics" :key="`conclusion-${topic.id}`" class="conclusion-item">
        <strong>第 {{ topic.sortNo }} 项：{{ topic.title }}</strong>
        <p>{{ topic.conclusion || '暂未录入会议结论' }}</p>
      </article>
      <el-empty v-if="topics.length === 0" description="暂无结论" />
    </section>

    <PersonListDrawer v-model="attendeeVisible" :title="attendeeDrawerTitle" :people="visibleAttendees" />
    <PersonSelectDialog
      v-model="selectorVisible"
      :title="selectorTitle"
      :departments="visibleDepartments"
      :users="candidateUsers"
      :selected-ids="selectedIds"
      :default-department="defaultDepartment"
      :loading="submitting"
      @confirm="submitSelection"
    />
    <el-dialog v-model="conclusionVisible" title="议题结论信息" width="560px">
      <div v-if="activeConclusion" class="conclusion-dialog">
        <h3>{{ activeConclusion.title }}</h3>
        <p class="muted">汇报部门：{{ activeConclusion.reportDepartmentName || '-' }}</p>
        <div>{{ activeConclusion.conclusion || '暂未录入会议结论' }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { Edit, Link as LinkIcon, Refresh, View } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import PersonListDrawer from '@/components/meeting/PersonListDrawer.vue'
import PersonSelectDialog from '@/components/meeting/PersonSelectDialog.vue'
import { api, DepartmentItem, MeetingItem, TopicItem, UserItem } from '@/api/meeting'

const route = useRoute()
const store = useStore()
const meetingId = computed(() => Number(route.params.id))
const meeting = ref<MeetingItem | null>(null)
const users = ref<UserItem[]>([])
const departments = ref<DepartmentItem[]>([])
const loading = ref(false)
const attendeeVisible = ref(false)
const selectorVisible = ref(false)
const conclusionVisible = ref(false)
const submitting = ref(false)
const confirming = ref(false)
const activeTopic = ref<TopicItem | null>(null)
const activeAction = ref<'REPORT' | 'PARTAKE'>('PARTAKE')
const selectedIds = ref<string[]>([])
const defaultDepartment = ref('')
const activeConclusion = ref<TopicItem | null>(null)

const currentUser = computed(() => store.state.user)
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
const topics = computed<TopicItem[]>(() => meeting.value?.topics || [])
const attendees = computed<UserItem[]>(() => meeting.value?.attendees || [])
const visibleAttendees = computed<UserItem[]>(() => {
  const normalized = attendees.value.map(normalizeUser)
  if (isAdmin.value || !currentDepartmentId()) return normalized
  return normalized.filter((person) => person.departmentId === currentDepartmentId())
})
const attendeeDrawerTitle = computed(() => isAdmin.value ? '本次会议参会人员' : '本部门参会人员')
const detailTitle = computed(() => {
  if (!meeting.value) return '会议详情'
  return `${meeting.value.meetingDate || ''} ${meeting.value.periodNo || ''} 三重一大会议`.trim()
})
const subtitle = computed(() => meeting.value ? `${meeting.value.meetingTime || ''} · ${meeting.value.location || ''}` : '')
const selectorTitle = computed(() => activeAction.value === 'REPORT' ? '选择汇报人' : '选择参会人')
const submitAttendeeType = computed<'LEADER' | 'PARTAKE'>(() => currentUser.value?.role === 'LEADER' ? 'PARTAKE' : 'LEADER')
const candidateRole = computed(() => submitAttendeeType.value === 'LEADER' ? 'LEADER' : 'PARTICIPANT')
const canConfirmAttendees = computed(() => ['SECRETARY', 'LEADER'].includes(currentUser.value?.role || ''))
const candidateUsers = computed(() => users.value.filter((user) => user.role === candidateRole.value && user.departmentId === currentDepartmentId()))
const visibleDepartments = computed(() => departments.value.filter((department) => String(department.id || department.deptId || '') === currentDepartmentId()))

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
  return currentUser.value?.role === 'SECRETARY' && currentDepartmentId() === topic.reportDepartmentId
}

function canSelectParticipant(topic: TopicItem) {
  if (currentUser.value?.role === 'SECRETARY') {
    return (topic.participantDepartmentIds || []).includes(currentDepartmentId())
  }
  if (currentUser.value?.role === 'LEADER') {
    return topicPeople(topic, 'LEADER').some((person) => String(person.userId || person.id) === currentUserId())
  }
  return false
}

function openSelector(topic: TopicItem, action: 'REPORT' | 'PARTAKE') {
  activeTopic.value = topic
  activeAction.value = action
  defaultDepartment.value = currentDepartmentId()
  const type = submitAttendeeType.value
  selectedIds.value = topicPeople(topic, type).map((item: any) => String(item.userId || item.id))
  selectorVisible.value = true
}

async function confirmAttendees() {
  confirming.value = true
  try {
    const result = await api.confirmAttendees(meetingId.value) as any
    ElMessage.success(`已确认 ${result.confirmedCount || 0} 名参会人员`)
    await load()
  } catch (error: any) {
    ElMessage.error(error.message || '确认失败')
  } finally {
    confirming.value = false
  }
}

async function submitSelection(ids: string[]) {
  if (!activeTopic.value) return
  submitting.value = true
  try {
    await api.submitAttendees(activeTopic.value.id as number, { userIds: ids, attendeeType: submitAttendeeType.value })
    ElMessage.success('人员已保存')
    selectorVisible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

function topicPeople(topic: TopicItem, type: 'LEADER' | 'PARTAKE') {
  const attendees = ((topic as any).attendees || []) as Array<UserItem & { attendeeType?: string; attendee_type?: string }>
  return attendees.filter((item) => {
    const itemType = item.attendeeType || item.attendee_type
    return type === 'PARTAKE' ? itemType === 'PARTAKE' || itemType === 'PARTICIPANT' : itemType === type
  })
}

function currentDepartmentId() {
  return String(currentUser.value?.departmentId || currentUser.value?.department_id || currentUser.value?.deptId || '')
}

function currentUserId() {
  return String(currentUser.value?.userId || currentUser.value?.user_id || currentUser.value?.id || currentUser.value?.employeeNo || '')
}

function showConclusion(topic: TopicItem) {
  activeConclusion.value = topic
  conclusionVisible.value = true
}

async function copyMobileLink() {
  await copyText(`${window.location.origin}/mobile/meetings/${meetingId.value}`)
  ElMessage.success('移动详情链接已复制')
}

async function copyText(text: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
    return
  }
  const input = document.createElement('textarea')
  input.value = text
  document.body.appendChild(input)
  input.select()
  document.execCommand('copy')
  document.body.removeChild(input)
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
.topic-panel,
.conclusion-panel {
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
  margin: 6px 0 8px;
  color: #667085;
  font-size: 12px;
}

.topic-people {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
}

.topic-people span {
  color: #475467;
  font-weight: 600;
}

.topic-people em {
  color: #98a2b3;
  font-style: normal;
}

.row-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
}

.conclusion-item {
  margin-top: 12px;
  padding: 14px;
  background: #f8fafc;
  border: 1px dashed #d0d5dd;
  border-radius: 12px;
}

.conclusion-item p,
.conclusion-dialog div {
  margin: 8px 0 0;
  color: #475467;
  line-height: 1.7;
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
}
</style>
