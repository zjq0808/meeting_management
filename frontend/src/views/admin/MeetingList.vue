<template>
  <div class="page meeting-list-page">
    <div class="toolbar list-toolbar">
      <div>
        <h2>技术局三重一大会议</h2>
        <p class="muted">{{ isAdmin ? '会议管理员可查看全部会议' : '仅显示本人作为参会人、汇报人或职责相关的会议' }}</p>
      </div>
      <el-button v-if="isAdmin" type="primary" class="action-button" :icon="Plus" @click="$router.push('/meetings/new')">创建会议</el-button>
    </div>

    <section class="filter-panel">
      <el-select v-model="filters.status" clearable placeholder="全部状态" class="filter-control">
        <el-option label="待发布" value="DRAFT" />
        <el-option label="待开始" value="PUBLISHED" />
        <el-option label="进行中" value="IN_PROGRESS" />
        <el-option label="已结束" value="FINISHED" />
      </el-select>
      <el-date-picker v-model="filters.date" value-format="YYYY-MM-DD" type="date" placeholder="会议日期" class="filter-control" />
      <el-input v-model="filters.keyword" placeholder="输入期数或地点" clearable class="filter-control" />
      <el-button :icon="Search" class="action-button" @click="load">查询</el-button>
    </section>

    <el-skeleton v-if="loading" :rows="6" animated />
    <el-empty v-else-if="filteredMeetings.length === 0" description="暂无会议" />

    <div v-else class="meeting-grid">
      <article v-for="meeting in filteredMeetings" :key="meeting.id" class="meeting-card">
        <header class="meeting-card-head">
          <span :class="['status-pill', statusClass(meeting.status)]">{{ statusLabel(meeting.status) }}</span>
          <span class="meeting-no">#{{ meeting.periodNo || meeting.id }}</span>
        </header>
        <div class="meeting-card-body">
          <button class="meeting-title" @click="$router.push(`/meetings/${meeting.id}/detail`)">
            {{ displayTitle(meeting) }}
          </button>
          <div class="meeting-meta"><el-icon><Clock /></el-icon><span>{{ meeting.meetingDate }} {{ meeting.meetingTime }}</span></div>
          <div class="meeting-meta"><el-icon><Location /></el-icon><span>{{ meeting.location || '-' }}</span></div>
          <div class="meeting-content">{{ meeting.content || '暂无会议内容' }}</div>
        </div>
        <footer class="meeting-card-foot">
          <span>{{ topicCount(meeting) }} 个议题</span>
          <div class="card-actions">
            <el-button link type="primary" @click="$router.push(`/meetings/${meeting.id}/detail`) ">详情</el-button>
            <el-button v-if="canEdit(meeting)" link type="primary" @click="$router.push(`/meetings/${meeting.id}/edit`) ">编辑</el-button>
            <el-button v-if="isAdmin" link type="primary" @click="$router.push(`/meetings/${meeting.id}/console`) ">控制台</el-button>
            <el-button v-if="isAdmin" link @click="$router.push(`/meetings/${meeting.id}/signin-dashboard`) ">签到</el-button>
          </div>
        </footer>
      </article>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { Clock, Location, Plus, Search } from '@element-plus/icons-vue'
import { api, MeetingItem } from '@/api/meeting'

const store = useStore()
const loading = ref(false)
const meetings = ref<MeetingItem[]>([])
const filters = reactive({ status: '', date: '', keyword: '' })
const isAdmin = computed(() => store.state.user?.role === 'ADMIN')

const filteredMeetings = computed(() => {
  const keyword = filters.keyword.trim()
  return meetings.value.filter((meeting) => {
    if (filters.status && meeting.status !== filters.status) return false
    if (filters.date && meeting.meetingDate !== filters.date) return false
    if (keyword && !`${meeting.periodNo || ''}${meeting.location || ''}${meeting.content || ''}`.includes(keyword)) return false
    return true
  })
})

async function load() {
  loading.value = true
  try {
    meetings.value = await api.meetings() as MeetingItem[]
  } catch (error: any) {
    ElMessage.error(error.message || '加载会议失败')
  } finally {
    loading.value = false
  }
}

function canEdit(row: MeetingItem) {
  return isAdmin.value && ['DRAFT', 'PUBLISHED'].includes(row.status)
}

function displayTitle(meeting: MeetingItem) {
  return `${meeting.meetingDate || ''} ${meeting.periodNo || ''} 三重一大会议`.trim()
}

function topicCount(meeting: MeetingItem) {
  return meeting.topics?.length || 0
}

function statusLabel(status: string) {
  const map: Record<string, string> = { DRAFT: '待发布', PUBLISHED: '待开始', IN_PROGRESS: '进行中', FINISHED: '已结束' }
  return map[status] || status
}

function statusClass(status: string) {
  const map: Record<string, string> = { DRAFT: 'status-draft', PUBLISHED: 'status-published', IN_PROGRESS: 'status-progress', FINISHED: 'status-finished' }
  return map[status] || 'status-draft'
}

onMounted(load)
</script>

<style scoped>
.meeting-list-page {
  max-width: 1200px;
}

.filter-panel {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  margin-bottom: 22px;
}

.filter-control {
  width: 190px;
}

.meeting-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.meeting-card {
  display: flex;
  min-height: 245px;
  flex-direction: column;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.meeting-card:hover {
  transform: translateY(-2px);
  border-color: #bfdbfe;
  box-shadow: 0 14px 32px rgba(15, 23, 42, 0.1);
}

.meeting-card-head,
.meeting-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 16px 18px;
}

.meeting-card-head {
  border-bottom: 1px solid #f0f2f5;
}

.meeting-no {
  color: #9ca3af;
  font-size: 12px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
}

.meeting-card-body {
  flex: 1;
  padding: 18px;
}

.meeting-title {
  display: block;
  width: 100%;
  margin: 0 0 14px;
  padding: 0;
  color: #1a73e8;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.45;
  text-align: left;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.meeting-title:hover {
  text-decoration: underline;
}

.meeting-meta {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 8px;
  color: #6b7280;
  font-size: 14px;
}

.meeting-content {
  margin-top: 12px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.meeting-card-foot {
  border-top: 1px solid #f0f2f5;
  color: #6b7280;
  font-size: 14px;
}

.card-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 1180px) {
  .meeting-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .meeting-grid {
    grid-template-columns: 1fr;
  }

  .filter-control {
    width: 100%;
  }
}
</style>
