<template>
  <div class="mobile-page">
    <van-nav-bar title="会议详情" left-arrow @click-left="$router.back()">
      <template #right>
        <van-icon name="share-o" @click="copyLink" />
      </template>
    </van-nav-bar>
    <main class="mobile-content">
      <section v-if="meeting" class="mobile-card">
        <div class="detail-title">
          <h2>{{ meeting.periodNo || '未填写期数' }} 三重一大会议</h2>
          <van-tag :type="tagType(meeting.status)">{{ statusLabel(meeting.status) }}</van-tag>
        </div>
        <p>{{ meeting.meetingDate }} {{ meeting.meetingTime }}</p>
        <p>{{ meeting.location || '-' }}</p>
        <p class="content">{{ meeting.content || '暂无会议内容' }}</p>
        <van-button block plain type="primary" @click="peopleVisible = true">查看参会人员（{{ attendees.length }} 人）</van-button>
      </section>

      <section class="topic-list">
        <h3>参会议题信息</h3>
        <article v-for="topic in topics" :key="topic.id" class="mobile-card topic-card">
          <div class="topic-head">
            <span>{{ pad(topic.sortNo) }}</span>
            <h4>{{ topic.title }}</h4>
          </div>
          <p>汇报部门：{{ topic.reportDepartmentName || '-' }}</p>
          <p>参会部门：{{ topic.participantDepartments || topic.participantDeptName || '-' }}</p>
          <div class="topic-actions">
            <van-button v-if="canSelectReporter(topic)" size="small" type="primary" @click="openSelector(topic, 'REPORT')">选择汇报人</van-button>
            <van-button v-if="canSelectParticipant(topic)" size="small" plain type="primary" @click="openSelector(topic, 'PARTAKE')">选择参会人</van-button>
            <van-button v-if="canShare(topic)" size="small" plain @click="copyTopicLink(topic)">分享</van-button>
            <van-button size="small" plain @click="showConclusion(topic)">查看结论</van-button>
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
        <van-search v-model="keyword" placeholder="搜索姓名或工号" />
        <van-checkbox-group v-model="selectedIds">
          <van-cell-group inset>
            <van-cell v-for="user in filteredUsers" :key="user.id" clickable :title="user.realName || user.username" :label="`${user.departmentName || '-'} / ${user.employeeNo || user.id}`" @click="toggleUser(user.id)">
              <template #right-icon><van-checkbox :name="user.id" /></template>
            </van-cell>
          </van-cell-group>
        </van-checkbox-group>
        <van-button block type="primary" :loading="submitting" @click="submitSelection">保存</van-button>
      </div>
    </van-popup>

    <van-dialog v-model:show="conclusionVisible" title="议题结论" confirm-button-text="关闭">
      <div class="dialog-content">{{ activeConclusion?.conclusion || '暂未录入会议结论' }}</div>
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
const selectorTitle = computed(() => activeType.value === 'REPORT' ? '选择汇报人' : '选择参会人')
const filteredUsers = computed(() => {
  const key = keyword.value.trim()
  return users.value.filter((user) => {
    if (activeType.value === 'REPORT' && activeTopic.value?.reportDepartmentId && user.departmentId !== activeTopic.value.reportDepartmentId) return false
    if (activeType.value === 'PARTAKE' && currentUser.value?.departmentId && user.departmentId !== currentUser.value.departmentId) return false
    if (key && !`${user.username || ''}${user.realName || ''}${user.employeeNo || ''}${user.id}`.includes(key)) return false
    return true
  })
})

async function load() {
  try {
    const [detail, userData] = await Promise.all([api.meetingDetail(meetingId.value), api.users()])
    meeting.value = normalizeMeeting(detail as MeetingItem)
    users.value = userData as UserItem[]
  } catch (error: any) {
    showFailToast(error.message || '加载失败')
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
  return ['SECRETARY', 'LEADER'].includes(currentUser.value?.role || '')
}

function canShare(topic: TopicItem) {
  return currentUser.value?.role === 'SECRETARY' && (canSelectReporter(topic) || canSelectParticipant(topic))
}

function openSelector(topic: TopicItem, type: 'REPORT' | 'PARTAKE') {
  activeTopic.value = topic
  activeType.value = type
  selectedIds.value = (topic.attendees || []).filter((item: any) => item.attendeeType === type || (type === 'PARTAKE' && item.attendeeType === 'PARTICIPANT')).map((item: any) => String(item.id || item.userId))
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

function showConclusion(topic: TopicItem) {
  activeConclusion.value = topic
  conclusionVisible.value = true
}

async function copyLink() {
  await copyText(`${window.location.origin}/mobile/meetings/${meetingId.value}`)
  showSuccessToast('链接已复制')
}

async function copyTopicLink(topic: TopicItem) {
  await copyText(`${window.location.origin}/mobile/meetings/${meetingId.value}?topicId=${topic.id}`)
  showSuccessToast('链接已复制')
}

async function copyText(text: string) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text)
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
  gap: 10px;
}

h2,
h3,
h4 {
  margin: 0;
}

.content {
  padding-top: 10px;
  border-top: 1px solid #edf1f7;
}

.topic-list h3 {
  margin: 14px 4px 10px;
  font-size: 15px;
}

.topic-head {
  display: flex;
  gap: 10px;
}

.topic-head > span {
  display: grid;
  flex: none;
  place-items: center;
  width: 28px;
  height: 28px;
  color: #175cd3;
  font-weight: 800;
  background: #eff6ff;
  border-radius: 8px;
}

.topic-card p,
.mobile-card p {
  color: #667085;
  font-size: 13px;
}

.topic-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.popup-panel {
  max-height: 72vh;
  padding: 18px 12px 20px;
  overflow: auto;
}

.popup-panel h3 {
  margin: 0 0 12px 4px;
}

.dialog-content {
  padding: 18px 24px;
  color: #475467;
  line-height: 1.7;
}
</style>
