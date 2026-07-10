<template>
  <div class="page">
    <PageHeader
      title="技术局三重一大会议"
      :subtitle="isAdmin ? '会议管理员可查看全部会议' : '仅展示与本人相关的会议'"
    >
      <template #actions>
        <el-button v-if="isAdmin" type="primary" :icon="Plus" @click="$router.push('/meetings/new')">创建会议</el-button>
      </template>
    </PageHeader>

    <section class="filters surface">
      <el-select v-model="filters.status" clearable placeholder="全部状态">
        <el-option label="待发布" value="DRAFT" />
        <el-option label="待开始" value="PUBLISHED" />
        <el-option label="进行中" value="IN_PROGRESS" />
        <el-option label="已结束" value="FINISHED" />
      </el-select>
      <el-date-picker v-model="filters.date" value-format="YYYY-MM-DD" type="date" placeholder="会议日期" />
      <el-input v-model="filters.keyword" clearable placeholder="输入期数、地点或内容" />
      <el-button :icon="Search" @click="load">查询</el-button>
    </section>
    <el-alert
      v-if="backendError"
      class="backend-alert"
      type="warning"
      :closable="false"
      show-icon
      :title="backendError"
    />

    <el-skeleton v-if="loading" :rows="8" animated />
    <el-empty v-else-if="filteredMeetings.length === 0" description="暂无会议" />
    <section v-else class="meeting-grid">
      <MeetingCard
        v-for="meeting in filteredMeetings"
        :key="meeting.id"
        :meeting="meeting"
        :editable="canEdit(meeting)"
        :console-visible="isAdmin"
        @detail="openDetail"
        @edit="openEdit"
        @console="openConsole"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import MeetingCard from '@/components/meeting/MeetingCard.vue'
import { api, MeetingItem } from '@/api/meeting'

const router = useRouter()
const store = useStore()
const loading = ref(false)
const backendError = ref('')
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
    backendError.value = ''
    meetings.value = await api.meetings() as MeetingItem[]
  } catch (error: any) {
    backendError.value = error.message || '加载会议失败'
    ElMessage.error(backendError.value)
  } finally {
    loading.value = false
  }
}

function canEdit(meeting: MeetingItem) {
  return isAdmin.value && ['DRAFT', 'PUBLISHED'].includes(meeting.status)
}

function openDetail(meeting: MeetingItem) {
  router.push(`/meetings/${meeting.id}/detail`)
}

function openEdit(meeting: MeetingItem) {
  router.push(`/meetings/${meeting.id}/edit`)
}

function openConsole(meeting: MeetingItem) {
  router.push(`/meetings/${meeting.id}/console`)
}

onMounted(load)
</script>

<style scoped>
.filters {
  display: grid;
  grid-template-columns: 180px 190px minmax(220px, 1fr) auto;
  gap: 12px;
  align-items: center;
  margin-bottom: 22px;
  padding: 16px;
}

.backend-alert {
  margin-bottom: 16px;
}

.meeting-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

@media (max-width: 1180px) {
  .meeting-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .filters,
  .meeting-grid {
    grid-template-columns: 1fr;
  }
}
</style>
