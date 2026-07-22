<template>
  <div class="page detail-page">
    <div class="toolbar detail-topbar">
      <div>
        <h2>{{ detailTitle }}</h2>
        <p class="muted">{{ meeting?.meetingDate }} {{ meeting?.meetingTime }} · {{ meeting?.location }}</p>
      </div>
      <div class="top-actions">
        <el-button :icon="Back" @click="$router.push('/meetings')">返回</el-button>
        <el-button v-if="isAdmin" type="primary" :icon="Edit" @click="$router.push(`/meetings/${meetingId}/edit`) ">编辑</el-button>
      </div>
    </div>

    <section v-if="meeting" class="panel info-panel">
      <div class="panel-header">
        <h3><el-icon><Document /></el-icon>会议基本信息</h3>
        <span :class="['status-pill', statusClass(meeting.status)]">{{ statusLabel(meeting.status) }}</span>
      </div>
      <div class="info-grid">
        <div><label>会议日期</label><strong>{{ meeting.meetingDate || '-' }}</strong></div>
        <div><label>会议时间</label><strong>{{ meeting.meetingTime || '-' }}</strong></div>
        <div><label>会议期数</label><strong>{{ meeting.periodNo || '-' }}</strong></div>
        <div><label>会议地点</label><strong>{{ meeting.location || '-' }}</strong></div>
      </div>
      <div class="content-block">
        <label>会议内容</label>
        <p>{{ meeting.content || '暂无会议内容' }}</p>
      </div>
      <div class="leader-block">
        <label>本次会议参会人员</label>
        <el-button link type="primary" :icon="View" @click="attendeeVisible = true">查看 {{ attendees.length }} 人</el-button>
      </div>
    </section>

    <section class="panel topic-section">
      <div class="panel-header">
        <h3><el-icon><List /></el-icon>参会议题信息</h3>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
      <el-table v-loading="loading" :data="topics" border empty-text="暂无议题">
        <el-table-column label="序号" width="76" align="center"><template #default="{ $index }">{{ pad($index + 1) }}</template></el-table-column>
        <el-table-column label="议题名称" min-width="260"><template #default="{ row }"><strong>{{ row.title }}</strong></template></el-table-column>
        <el-table-column label="汇报部门" width="160"><template #default="{ row }">{{ row.reportDepartmentName || '-' }}</template></el-table-column>
        <el-table-column label="参会部门" min-width="180"><template #default="{ row }">{{ row.participantDepartments || '-' }}</template></el-table-column>
        <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag>{{ topicStatusLabel(row.status) }}</el-tag></template></el-table-column>
        <el-table-column label="操作" width="320" fixed="right" align="center">
          <template #default="{ row }">
            <div class="row-actions">
              <el-button v-if="canSelectReporter(row)" size="small" type="primary" @click="openSelector(row, 'REPORT')">选择汇报人</el-button>
              <el-button v-if="canSelectParticipant(row)" size="small" type="primary" plain @click="openSelector(row, 'PARTAKE')">选择参会人</el-button>
              <el-button v-if="canShare(row)" size="small" :icon="Share" @click="shareTopic(row)">分享</el-button>
              <el-button v-if="canViewTopic(row)" size="small" link type="primary" @click="showConclusion(row)">查看</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-drawer v-model="attendeeVisible" title="本次会议参会人员" size="560px">
      <el-table :data="attendees" border>
        <el-table-column prop="departmentName" label="部门" />
        <el-table-column prop="employeeNo" label="工号" width="130" />
        <el-table-column label="姓名" width="140"><template #default="{ row }">{{ row.realName || row.username }}</template></el-table-column>
      </el-table>
      <el-empty v-if="attendees.length === 0" description="尚未选择参会人员" />
    </el-drawer>

    <el-dialog v-model="selectorVisible" :title="selectorTitle" width="760px" destroy-on-close>
      <div class="selector-layout">
        <aside class="dept-list">
          <button v-for="dept in departments" :key="dept.id" :class="{ active: selectedDepartment === dept.id }" @click="selectedDepartment = dept.id">
            {{ dept.name }}
          </button>
        </aside>
        <section class="user-list">
          <el-input v-model="userKeyword" clearable placeholder="搜索姓名或工号" />
          <el-checkbox-group v-model="selectedIds" class="user-checks">
            <label v-for="candidate in filteredCandidates" :key="candidate.id" class="user-check">
              <el-checkbox :label="candidate.id" />
              <span class="avatar">{{ firstChar(candidate.realName || candidate.username) }}</span>
              <span><strong>{{ candidate.realName || candidate.username }}</strong><em>{{ candidate.departmentName || '-' }} / {{ candidate.employeeNo || candidate.id }}</em></span>
            </label>
          </el-checkbox-group>
          <el-empty v-if="filteredCandidates.length === 0" description="当前部门暂无人员" />
        </section>
      </div>
      <template #footer>
        <el-button @click="selectorVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitSelection">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="conclusionVisible" title="议题结论信息" width="680px">
      <div v-if="activeConclusion" class="conclusion-dialog">
        <h4>{{ activeConclusion.title }}</h4>
        <p class="muted">汇报部门：{{ activeConclusion.reportDepartmentName || '-' }}</p>
        <el-input
          :model-value="activeConclusion.summary || '暂无会议纪要'"
          type="textarea"
          :rows="7"
          readonly
          resize="vertical"
          class="topic-summary-input"
        />
        <div class="conclusion-box">{{ activeConclusion.conclusion || '暂未录入会议结论' }}</div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { Back, Document, Edit, List, Refresh, Share, Tickets, View } from '@element-plus/icons-vue'
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
const activeTopic = ref<TopicItem | null>(null)
const activeType = ref<'REPORT' | 'PARTAKE'>('PARTAKE')
const selectedIds = ref<string[]>([])
const selectedDepartment = ref('')
const userKeyword = ref('')
const activeConclusion = ref<TopicItem | null>(null)

const currentUser = computed(() => store.state.user)
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')
const topics = computed<TopicItem[]>(() => meeting.value?.topics || [])
const attendees = computed<UserItem[]>(() => meeting.value?.attendees || [])
const detailTitle = computed(() => meeting.value ? `${meeting.value.meetingDate || ''} ${meeting.value.periodNo || ''} 三重一大会议` : '会议详情')
const selectorTitle = computed(() => activeType.value === 'REPORT' ? '选择汇报人' : '选择参会人')
const filteredCandidates = computed(() => {
  const keyword = userKeyword.value.trim()
  return users.value.filter((user) => {
    if (selectedDepartment.value && user.departmentId !== selectedDepartment.value) return false
    if (keyword && !`${user.username || ''}${user.realName || ''}${user.employeeNo || ''}${user.id}`.includes(keyword)) return false
    return true
  })
})

async function load() {
  loading.value = true
  try {
    const [meetingData, userData, departmentData] = await Promise.all([api.meetingDetail(meetingId.value), api.users(), api.departments()])
    meeting.value = normalizeMeeting(meetingData as MeetingItem)
    users.value = userData as UserItem[]
    departments.value = departmentData as DepartmentItem[]
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
      participantDepartmentIds: normalizeIds(topic.participantDepartmentIds || topic.participantDeptId)
    }))
  }
}

function canSelectReporter(topic: TopicItem) {
  return canSelectRole() && currentUser.value?.departmentId === topic.reportDepartmentId
}

function canSelectParticipant(topic: TopicItem) {
  return canSelectRole() && (topic.participantDepartmentIds || []).includes(currentUser.value?.departmentId || '')
}

function canSelectRole() {
  return ['ADMIN', 'SECRETARY'].includes(currentUser.value?.role || '')
}

function canViewTopic(topic: TopicItem) {
  if (currentUser.value?.role === 'ADMIN') return true
  const departmentId = String(currentUser.value?.departmentId || currentUser.value?.department_id || currentUser.value?.deptId || '')
  if (!departmentId) return false
  const reportIds = normalizeIds(topic.reportDepartmentIds || topic.reportDepartmentId)
  const participantIds = normalizeIds(topic.participantDepartmentIds || topic.participantDeptId)
  return reportIds.includes(departmentId) || participantIds.includes(departmentId)
}

function canShare(topic: TopicItem) {
  return currentUser.value?.role === 'SECRETARY' && (canSelectReporter(topic) || canSelectParticipant(topic))
}

function openSelector(topic: TopicItem, type: 'REPORT' | 'PARTAKE') {
  activeTopic.value = topic
  activeType.value = type
  selectedDepartment.value = type === 'REPORT' ? (topic.reportDepartmentId || '') : (currentUser.value?.departmentId || '')
  userKeyword.value = ''
  selectedIds.value = (topic.attendees || []).filter((item: any) => item.attendeeType === type).map((item: any) => String(item.id || item.userId))
  selectorVisible.value = true
}

async function submitSelection() {
  if (!activeTopic.value) return
  submitting.value = true
  try {
    await api.submitAttendees(activeTopic.value.id as number, { userIds: selectedIds.value, attendeeType: activeType.value })
    ElMessage.success('人员已保存')
    selectorVisible.value = false
    await load()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

function showConclusion(topic: TopicItem) {
  activeConclusion.value = topic
  conclusionVisible.value = true
}

async function shareTopic(topic: TopicItem) {
  const url = `${window.location.origin}/mobile/meetings/${meetingId.value}?topicId=${topic.id}`
  await copyText(url)
  ElMessage.success('移动端链接已复制，可发给科组长处理')
}

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

function firstChar(value?: string) {
  return value ? value.slice(0, 1) : '-'
}

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function statusLabel(status: string) {
  const map: Record<string, string> = { DRAFT: '待发布', PUBLISHED: '待开始', IN_PROGRESS: '进行中', FINISHED: '已结束' }
  return map[status] || status
}

function statusClass(status: string) {
  const map: Record<string, string> = { DRAFT: 'status-draft', PUBLISHED: 'status-published', IN_PROGRESS: 'status-progress', FINISHED: 'status-finished' }
  return map[status] || 'status-draft'
}

function topicStatusLabel(status?: string) {
  const map: Record<string, string> = { WAITING: '待讨论', RUNNING: '讨论中', FINISHED: '已结束' }
  return status ? map[status] || status : '-'
}

onMounted(load)
</script>

<style scoped>
.detail-page {
  max-width: 1200px;
}

.detail-topbar,
.top-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.info-panel,
.topic-section,
.conclusion-section {
  margin-bottom: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 28px;
}

.info-grid label,
.content-block label,
.leader-block label {
  display: block;
  margin-bottom: 7px;
  color: #9ca3af;
  font-size: 12px;
}

.info-grid strong {
  font-size: 14px;
}

.content-block,
.leader-block {
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid #f3f4f6;
}

.content-block p {
  margin: 0;
  color: #4b5563;
  line-height: 1.7;
}

.topic-summary {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 12px;
  line-height: 1.5;
}

.topic-summary-input {
  margin-top: 14px;
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

.conclusion-list {
  display: grid;
  gap: 12px;
}

.conclusion-item {
  padding: 14px;
  background: #f8fafc;
  border: 1px dashed #dbe3ef;
  border-radius: 8px;
}

.conclusion-item p {
  margin: 8px 0 0;
  color: #4b5563;
  line-height: 1.7;
}

.selector-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  min-height: 430px;
  border: 1px solid #eef2f7;
  border-radius: 8px;
  overflow: hidden;
}

.dept-list {
  padding: 12px;
  overflow: auto;
  background: #f8fafc;
  border-right: 1px solid #eef2f7;
}

.dept-list button {
  display: block;
  width: 100%;
  margin-bottom: 6px;
  padding: 8px 10px;
  text-align: left;
  background: transparent;
  border: 0;
  border-radius: 6px;
  cursor: pointer;
}

.dept-list button.active,
.dept-list button:hover {
  color: #1a73e8;
  background: #eff6ff;
}

.user-list {
  padding: 14px;
}

.user-checks {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  max-height: 360px;
  margin-top: 12px;
  overflow: auto;
}

.user-check {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.user-check:hover {
  background: #eff6ff;
  border-color: #93c5fd;
}

.avatar {
  display: inline-grid;
  place-items: center;
  width: 32px;
  height: 32px;
  color: #1a73e8;
  font-weight: 700;
  background: #dbeafe;
  border-radius: 50%;
}

.user-check strong,
.user-check em {
  display: block;
  font-style: normal;
}

.user-check em {
  margin-top: 2px;
  color: #9ca3af;
  font-size: 12px;
}

.conclusion-dialog h4 {
  margin: 0 0 8px;
}

.conclusion-box {
  margin-top: 14px;
  padding: 14px;
  color: #4b5563;
  line-height: 1.7;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

@media (max-width: 900px) {
  .info-grid,
  .selector-layout {
    grid-template-columns: 1fr;
  }

  .dept-list {
    border-right: 0;
    border-bottom: 1px solid #eef2f7;
  }

  .user-checks {
    grid-template-columns: 1fr;
  }
}
</style>
