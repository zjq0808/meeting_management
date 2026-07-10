<template>
  <div class="mobile-page">
    <van-nav-bar title="会议进度" />
    <div class="progress-panel">
      <van-circle :current-rate="rate" :rate="rate" :speed="80" :text="circleText" size="150px" />
      <h2>{{ message }}</h2>
      <p v-if="progress?.currentTopic">当前第 {{ progress.currentTopic.sortNo }} 项议题</p>
      <p v-else>请等待管理员开始议题。</p>
    </div>
    <van-cell-group inset title="已结束议题">
      <van-cell v-for="item in finishedTopics" :key="item.sortNo" :title="`第 ${item.sortNo} 项`" :value="`${item.actualMinutes || 0} 分钟`" />
    </van-cell-group>
  </div>
</template>

<script lang="ts" setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showFailToast } from 'vant'
import { api } from '@/api/meeting'

const route = useRoute()
const meetingId = Number(route.params.meetingId)
const progress = ref<any>(null)
let timer = 0
const finishedTopics = computed(() => (progress.value?.topics || [])
  .filter((item: any) => item.status === 'FINISHED')
  .map((item: any, index: number) => ({
    sortNo: item.sortNo || index + 1,
    actualMinutes: item.actualMinutes || 0
  })))
const rate = computed(() => progress.value?.currentTopic ? 70 : 10)
const circleText = computed(() => progress.value?.currentTopic ? `第${progress.value.currentTopic.sortNo}项` : '待开始')
const message = computed(() => {
  if (progress.value?.status === 'FINISHED') return '会议已结束'
  return progress.value?.currentTopic ? '会议进行中' : '会议即将开始'
})

async function load() {
  progress.value = await api.dashboard(meetingId)
}

onMounted(() => {
  load().catch((error: any) => showFailToast(error.message || '加载失败'))
  timer = window.setInterval(() => load().catch(() => undefined), 5000)
})

onBeforeUnmount(() => window.clearInterval(timer))
</script>

<style scoped>
.progress-panel {
  padding: 36px 24px 20px;
  text-align: center;
}

h2 {
  margin: 20px 0 8px;
  font-size: 20px;
}

p {
  color: #6b7280;
}
</style>
