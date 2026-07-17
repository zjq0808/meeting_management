<template>
  <div class="mobile-page">
    <van-nav-bar title="三重一大会议" />

    <div class="mobile-content">
      <section class="mobile-card mobile-filters">
        <van-dropdown-menu>
          <van-dropdown-item v-model="filters.status" :options="statusOptions" />
        </van-dropdown-menu>
        <van-field v-model="filters.keyword" clearable placeholder="搜索期数、地点" />
      </section>

      <van-pull-refresh v-model="refreshing" @refresh="load">
        <article v-for="meeting in filteredMeetings" :key="meeting.id" class="mobile-meeting-card" @click="$router.push(`/mobile/meetings/${meeting.id}`)">
          <div class="meeting-card-head">
            <h3>{{ meeting.periodNo || '未填写期数' }} 三重一大会议</h3>
            <van-tag :type="tagType(meeting.status)">{{ statusLabel(meeting.status) }}</van-tag>
          </div>
          <div class="meeting-meta">
            <span>{{ meeting.meetingDate || '-' }}</span>
            <span>{{ meeting.meetingTime || '-' }}</span>
            <span>{{ meeting.location || '-' }}</span>
          </div>
          <footer>
            <span>{{ topicTotal(meeting) }} 个议题</span>
            <span>{{ meeting.content || '暂无会议内容' }}</span>
            <van-button v-if="canEdit(meeting)" size="mini" type="primary" @click.stop="$router.push(`/meetings/${meeting.id}/edit`)">编辑</van-button>
            <van-button v-if="canOpenConsole" size="mini" plain type="primary" @click.stop="$router.push(`/mobile/meetings/${meeting.id}/console`)">控制台</van-button>
          </footer>
        </article>
        <van-empty v-if="!loading && filteredMeetings.length === 0" description="暂无会议" />
      </van-pull-refresh>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useStore } from 'vuex'
import { showFailToast } from 'vant'
import { api, MeetingItem } from '@/api/meeting'

const store = useStore()
const meetings = ref<MeetingItem[]>([])
const loading = ref(false)
const refreshing = ref(false)
const filters = reactive({ status: '', keyword: '' })
const statusOptions = [
  { text: '全部状态', value: '' },
  { text: '待发布', value: 'DRAFT' },
  { text: '待开始', value: 'PUBLISHED' },
  { text: '进行中', value: 'IN_PROGRESS' },
  { text: '已结束', value: 'FINISHED' }
]
const canOpenConsole = computed(() => store.state.user?.role === 'ADMIN')

const filteredMeetings = computed(() => {
  const keyword = filters.keyword.trim()
  return meetings.value.filter((meeting) => {
    if (filters.status && meeting.status !== filters.status) return false
    if (keyword && !`${meeting.periodNo || ''}${meeting.location || ''}${meeting.content || ''}`.includes(keyword)) return false
    return true
  })
})

async function load() {
  loading.value = true
  try {
    meetings.value = await api.meetings() as MeetingItem[]
  } catch (error: any) {
    showFailToast(error.message || '加载失败')
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function canEdit(meeting: MeetingItem) {
  return false
}

function topicTotal(meeting: MeetingItem) {
  return meeting.topicCount ?? meeting.topics?.length ?? 0
}

function statusLabel(status: string) {
  const map: Record<string, string> = { DRAFT: '待发布', PUBLISHED: '待开始', IN_PROGRESS: '进行中', FINISHED: '已结束' }
  return map[status] || status
}

function tagType(status: string) {
  const map: Record<string, string> = { DRAFT: 'default', PUBLISHED: 'primary', IN_PROGRESS: 'success', FINISHED: 'default' }
  return map[status] || 'default'
}

onMounted(load)
</script>

<style scoped>
.mobile-filters {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  gap: 6px;
  padding: 8px;
}

.mobile-filters :deep(.van-dropdown-menu__bar),
.mobile-filters :deep(.van-field) {
  height: 34px;
  background: #f7f9fc;
  box-shadow: none;
}

.mobile-filters :deep(.van-dropdown-menu__title),
.mobile-filters :deep(.van-field__control) {
  color: #475467;
  font-size: 12px;
}

.mobile-meeting-card {
  position: relative;
  margin-bottom: 8px;
  padding: 11px 12px 10px 14px;
  background: #fff;
  border: 1px solid #e7ecf3;
  border-left: 3px solid #2f7df6;
  border-radius: 10px;
  box-shadow: none;
}

.mobile-meeting-card:active {
  background: #f8fbff;
}

.meeting-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

h3 {
  margin: 0;
  color: #111827;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.35;
}

.meeting-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 5px 10px;
  margin-top: 8px;
  color: #667085;
  font-size: 12px;
}

.meeting-meta span {
  position: relative;
  max-width: 100%;
}

.meeting-meta span + span::before {
  position: absolute;
  left: -6px;
  color: #d0d5dd;
  content: "·";
}

footer {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  margin-top: 9px;
  padding-top: 8px;
  color: #667085;
  border-top: 1px solid #eef2f6;
  font-size: 12px;
}

footer span:first-child {
  color: #175cd3;
  font-weight: 700;
}

footer span:nth-child(2) {
  overflow: hidden;
  color: #98a2b3;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
