<template>
  <div class="mobile-page">
    <van-nav-bar title="三重一大会议">
      <template #right><van-button v-if="isAdmin" size="small" type="primary" @click="$router.push('/meetings/new')">创建</van-button></template>
    </van-nav-bar>

    <van-pull-refresh v-model="refreshing" @refresh="load">
      <van-list>
        <van-cell v-for="meeting in meetings" :key="meeting.id" is-link @click="$router.push(`/mobile/meetings/${meeting.id}`)">
          <template #title><strong>{{ meeting.periodNo }}</strong></template>
          <template #label>
            <div>{{ meeting.meetingDate }} {{ meeting.meetingTime }} {{ meeting.location }}</div>
            <van-tag plain>{{ statusLabel(meeting.status) }}</van-tag>
          </template>
          <template #value>
            <van-button v-if="canEdit(meeting)" size="mini" type="primary" @click.stop="$router.push(`/meetings/${meeting.id}/edit`)">编辑</van-button>
          </template>
        </van-cell>
      </van-list>
      <van-empty v-if="!loading && meetings.length === 0" description="暂无会议" />
    </van-pull-refresh>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useStore } from 'vuex'
import { showFailToast } from 'vant'
import { api } from '@/api/meeting'

const store = useStore()
const meetings = ref<any[]>([])
const loading = ref(false)
const refreshing = ref(false)
const isAdmin = computed(() => store.state.user?.role === 'ADMIN')

async function load() {
  loading.value = true
  try {
    meetings.value = await api.meetings() as any[]
  } catch (error: any) {
    showFailToast(error.message || '加载失败')
  } finally {
    loading.value = false
    refreshing.value = false
  }
}

function canEdit(meeting: any) {
  return isAdmin.value && ['DRAFT', 'PUBLISHED'].includes(meeting.status)
}

function statusLabel(status: string) {
  const map: Record<string, string> = { DRAFT: '待发布', PUBLISHED: '待开始', IN_PROGRESS: '进行中', FINISHED: '已结束' }
  return map[status] || status
}

onMounted(load)
</script>
