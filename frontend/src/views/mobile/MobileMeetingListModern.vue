<template>
  <div class="mobile-page">
    <van-nav-bar title="三重一大会议">
      <template #right>
        <van-button v-if="isAdmin" size="small" type="primary" @click="$router.push('/meetings/new')">创建</van-button>
      </template>
    </van-nav-bar>

    <div class="mobile-content">
      <section class="mobile-card mobile-filters">
        <van-dropdown-menu>
          <van-dropdown-item v-model="filters.status" :options="statusOptions" />
        </van-dropdown-menu>
        <van-field v-model="filters.keyword" clearable placeholder="搜索期数、地点" />
      </section>

      <van-pull-refresh v-model="refreshing" @refresh="load">
        <article v-for="meeting in filteredMeetings" :key="meeting.id" class="mobile-meeting-card" @click="$router.push(`/mobile/meetings/${meeting.id}`)">
          <div>
            <h3>{{ meeting.periodNo || '未填写期数' }} 三重一大会议</h3>
            <van-tag :type="tagType(meeting.status)">{{ statusLabel(meeting.status) }}</van-tag>
          </div>
          <p>{{ meeting.meetingDate }} {{ meeting.meetingTime }}</p>
          <p>{{ meeting.location || '-' }}</p>
          <footer>
            <span>{{ meeting.topics?.length || 0 }} 个议题</span>
            <van-button v-if="canEdit(meeting)" size="mini" type="primary" @click.stop="$router.push(`/meetings/${meeting.id}/edit`)">编辑</van-button>
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
const isAdmin = computed(() => store.state.user?.role === 'ADMIN')
const statusOptions = [
  { text: '全部状态', value: '' },
  { text: '待发布', value: 'DRAFT' },
  { text: '待开始', value: 'PUBLISHED' },
  { text: '进行中', value: 'IN_PROGRESS' },
  { text: '已结束', value: 'FINISHED' }
]

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
  return isAdmin.value && ['DRAFT', 'PUBLISHED'].includes(meeting.status)
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
  grid-template-columns: 120px minmax(0, 1fr);
  gap: 8px;
  padding: 10px;
}

.mobile-meeting-card {
  margin-bottom: 12px;
  padding: 16px;
  background: #fff;
  border-left: 4px solid #1a73e8;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.mobile-meeting-card > div {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 10px;
}

h3 {
  margin: 0 0 10px;
  font-size: 16px;
  line-height: 1.4;
}

p {
  margin: 4px 0;
  color: #667085;
  font-size: 13px;
}

footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 12px;
  padding-top: 12px;
  color: #98a2b3;
  border-top: 1px solid #edf1f7;
}
</style>
