<template>
  <div class="mobile-page mobile-detail-page">
    <van-nav-bar title="会议详情" left-arrow fixed placeholder @click-left="$router.back()">
      <template #right><van-icon name="share-o" size="20" @click="share" /></template>
    </van-nav-bar>

    <main class="mobile-content">
      <section class="mobile-card meeting-card">
        <div class="meeting-head">
          <h2>{{ title }}</h2>
          <span :class="['mobile-status', statusClass(meeting?.status)]">{{ statusLabel(meeting?.status) }}</span>
        </div>
        <div class="meta-list">
          <div><van-icon name="clock-o" /><span>{{ meeting?.meetingDate || '-' }} {{ meeting?.meetingTime || '' }}</span></div>
          <div><van-icon name="location-o" /><span>{{ meeting?.location || '-' }}</span></div>
          <div><van-icon name="label-o" /><span>{{ meeting?.periodNo || '-' }}</span></div>
        </div>
        <div class="meeting-stats">
          <button @click="peopleVisible = true">
            <small>参会人员</small>
            <strong>{{ attendees.length }} 人</strong>
          </button>
        </div>
      </section>

      <section class="topic-block">
        <h3><span></span>参会议题信息</h3>
        <article v-for="topic in topics" :key="topic.id" class="mobile-card topic-card">
          <div class="topic-main">
            <span class="topic-index">{{ pad(topic.sortNo || 1) }}</span>
            <div>
              <h4>{{ topic.title }}</h4>
              <p>汇报部门：{{ topic.reportDepartmentName || '-' }}</p>
              <p>参会部门：{{ topic.participantDepartments || '-' }}</p>
            </div>
          </div>
          <div class="topic-foot">
            <span>已选择 <strong>{{ topic.attendees?.length || 0 }}</strong> 人</span>
            <div class="topic-actions">
              <van-button v-if="canSelectReporter(topic)" size="small" type="primary" plain round @click="openPicker(topic, 'REPORT')">选汇报人</van-button>
              <van-button v-if="canSelectParticipant(topic)" size="small" type="primary" round @click="openPicker(topic, 'PARTAKE')">选参会人</van-button>
              <van-button size="small" plain round @click="openConclusion(topic)">结论</van-button>
            </div>
          </div>
        </article>
        <van-empty v-if="!loading && topics.length === 0" description="暂无议题" />
      </section>
    </main>

    <van-popup v-model:show="peopleVisible" round position="bottom" :style="{ maxHeight: '70vh' }">
      <section class="popup-panel">
        <h3>参会人员名单</h3>
        <van-cell v-for="user in attendees" :key="user.id" :title="user.realName || user.username" :label="`${user.departmentName || '-'} / ${user.employeeNo || user.id}`" />
        <van-empty v-if="attendees.length === 0" description="尚未选择参会人员" />
      </section>
    </van-popup>

    <van-popup v-model:show="pickerVisible" round position="bottom" :style="{ maxHeight: '82vh' }">
      <section class="popup-panel picker-panel">
        <h3>{{ pickerTitle }}</h3>
        <van-search v-model="keyword" placeholder="搜索姓名或工号" />
        <van-checkbox-group v-model="selectedIds">
          <van-cell-group inset>
            <van-cell v-for="user in filteredCandidates" :key="user.id" clickable :title="user.realName || user.username" :label="`${user.departmentName || '-'} / ${user.employeeNo || user.id}`" @click="toggle(user.id)">
              <template #right-icon><van-checkbox :name="user.id" /></template>
            </van-cell>
          </van-cell-group>
        </van-checkbox-group>
        <van-empty v-if="filteredCandidates.length === 0" description="暂无可选人员" />
        <div class="popup-actions">
          <van-button block @click="pickerVisible = false">取消</van-button>
          <van-button block type="primary" :loading="submitting" @click="submit">保存</van-button>
        </div>
      </section>
    </van-popup>

    <van-dialog v-model:show="singleConclusionVisible" title="议题结论" confirm-button-text="关闭">
      <div v-if="activeConclusion" class="single-conclusion">
        <strong>{{ activeConclusion.title }}</strong>
        <p>{{ activeConclusion.conclusion || '暂未录入会议结论' }}</p>
      </div>
    </van-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { showFailToast, showSuccessToast } from 'vant'
import { api, MeetingItem, TopicItem, UserItem } from '@/api/meeting'

const route = useRoute()
const store = useStore()
const meetingId = computed(() => Number(route.params.id))
const meeting = ref<MeetingItem | null>(null)
const users = ref<UserItem[]>([])
const loading = ref(false)
const peopleVisible = ref(false)
const pickerVisible = ref(false)
const singleConclusionVisible = ref(false)
const submitting = ref(false)
const activeTopic = ref<TopicItem | null>(null)
const activeType = ref<'REPORT' | 'PARTAKE'>('PARTAKE')
const activeConclusion = ref<TopicItem | null>(null)
const selectedIds = ref<string[]>([])
const keyword = ref('')
const user = computed(() => store.state.user)
const topics = computed<TopicItem[]>(() => meeting.value?.topics || [])
const attendees = computed<UserItem[]>(() => meeting.value?.attendees || [])
const title = computed(() => meeting.value ? `${meeting.value.meetingDate || ''} ${meeting.value.periodNo || ''} 三重一大会议` : '会议详情')
const pickerTitle = computed(() => activeType.value === 'REPORT' ? '选择汇报人' : '选择参会人')
const filteredCandidates = computed(() => {
  const deptId = activeType.value === 'REPORT' ? activeTopic.value?.reportDepartmentId : user.value?.departmentId
  const text = keyword.value.trim()
  return users.value.filter((item) => {
    if (deptId && item.departmentId !== deptId) return false
    if (text && !`${item.username || ''}${item.realName || ''}${item.employeeNo || ''}${item.id}`.includes(text)) return false
    return true
  })
})

async function load() {
  loading.value = true
  try {
    const [detail, userList] = await Promise.all([api.meetingDetail(meetingId.value), api.users()])
    meeting.value = normalizeMeeting(detail as MeetingItem)
    users.value = userList as UserItem[]
  } catch (error: any) {
    showFailToast(error.message || '加载失败')
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
  return canSelectRole() && user.value?.departmentId === topic.reportDepartmentId
}

function canSelectParticipant(topic: TopicItem) {
  return canSelectRole() && (topic.participantDepartmentIds || []).includes(user.value?.departmentId || '')
}

function canSelectRole() {
  return ['ADMIN', 'SECRETARY'].includes(user.value?.role || '')
}

function openPicker(topic: TopicItem, type: 'REPORT' | 'PARTAKE') {
  activeTopic.value = topic
  activeType.value = type
  selectedIds.value = (topic.attendees || []).filter((item: any) => item.attendeeType === type).map((item: any) => String(item.id || item.userId))
  keyword.value = ''
  pickerVisible.value = true
}

function toggle(id: string) {
  const index = selectedIds.value.indexOf(id)
  if (index >= 0) selectedIds.value.splice(index, 1)
  else selectedIds.value.push(id)
}

async function submit() {
  if (!activeTopic.value) return
  submitting.value = true
  try {
    await api.submitAttendees(activeTopic.value.id as number, { userIds: selectedIds.value, attendeeType: activeType.value })
    showSuccessToast('保存成功')
    pickerVisible.value = false
    await load()
  } catch (error: any) {
    showFailToast(error.message || '保存失败')
  } finally {
    submitting.value = false
  }
}

function openConclusion(topic: TopicItem) {
  activeConclusion.value = topic
  singleConclusionVisible.value = true
}

async function share() {
  await copyText(window.location.href)
  showSuccessToast('链接已复制')
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

function pad(value: number) {
  return String(value).padStart(2, '0')
}

function statusLabel(status?: string) {
  const map: Record<string, string> = { DRAFT: '待发布', PUBLISHED: '待开始', IN_PROGRESS: '进行中', FINISHED: '已结束' }
  return status ? map[status] || status : ''
}

function statusClass(status?: string) {
  const map: Record<string, string> = { DRAFT: 'status-draft', PUBLISHED: 'status-published', IN_PROGRESS: 'status-progress', FINISHED: 'status-finished' }
  return status ? map[status] || 'status-draft' : 'status-draft'
}

onMounted(load)
</script>

<style scoped>
:deep(.van-nav-bar) {
  background: #1a73e8;
}

:deep(.van-nav-bar__title),
:deep(.van-nav-bar .van-icon) {
  color: #fff;
}

.mobile-content {
  padding: 14px;
}

.mobile-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.meeting-card {
  padding: 16px;
}

.meeting-head {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  justify-content: space-between;
}

.meeting-head h2 {
  margin: 0;
  font-size: 18px;
  line-height: 1.4;
}

.mobile-status {
  flex: none;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 700;
}

.meta-list {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  color: #5f6b7a;
  font-size: 14px;
}

.meta-list div {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meeting-stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-top: 14px;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
}

.meeting-stats button {
  padding: 0;
  text-align: left;
  background: transparent;
  border: 0;
}

.meeting-stats small,
.meeting-stats strong {
  display: block;
}

.meeting-stats small {
  color: #9ca3af;
  margin-bottom: 4px;
}

.meeting-stats strong {
  color: #1a73e8;
}

.topic-block {
  margin-top: 18px;
}

.topic-block h3 {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 12px;
  font-size: 16px;
}

.topic-block h3 span {
  width: 4px;
  height: 16px;
  background: #1a73e8;
  border-radius: 999px;
}

.topic-card {
  padding: 14px;
  margin-bottom: 12px;
}

.topic-main {
  display: flex;
  gap: 12px;
}

.topic-index {
  display: inline-grid;
  place-items: center;
  flex: none;
  width: 26px;
  height: 26px;
  color: #6b7280;
  font-size: 12px;
  font-weight: 700;
  background: #f3f4f6;
  border-radius: 5px;
}

.topic-main h4 {
  margin: 0 0 8px;
  font-size: 15px;
  line-height: 1.45;
}

.topic-main p {
  margin: 2px 0;
  color: #6b7280;
  font-size: 12px;
}

.topic-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed #e5e7eb;
}

.topic-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.popup-panel {
  padding: 16px 12px 24px;
}

.popup-panel h3 {
  margin: 0 4px 12px;
  font-size: 17px;
}

.popup-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 14px;
  padding: 0 4px;
}

.single-conclusion {
  padding: 18px;
}

.single-conclusion p {
  color: #4b5563;
  line-height: 1.7;
}
</style>
