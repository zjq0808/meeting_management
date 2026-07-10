<template>
  <div class="mobile-page">
    <van-nav-bar title="会议进程" left-arrow @click-left="$router.back()" />
    <main class="mobile-content">
      <section class="mobile-card progress-hero">
        <span :class="['pulse', { active: currentTopic }]"></span>
        <h2>{{ title }}</h2>
        <p>{{ dashboard?.location || '-' }}</p>
        <div class="progress-strip">
          <strong>第 {{ currentTopic?.sortNo || 0 }} 项</strong>
          <span>/ 共 {{ topics.length }} 项</span>
        </div>
      </section>
      <section class="mobile-card">
        <h3>会议议程</h3>
        <TopicTimeline :topics="topics" />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showFailToast } from 'vant'
import TopicTimeline from '@/components/meeting/TopicTimeline.vue'
import { api, TopicItem } from '@/api/meeting'

const route = useRoute()
const meetingId = Number(route.params.meetingId)
const dashboard = ref<any>(null)
let timer = 0
const topics = computed<TopicItem[]>(() => dashboard.value?.topics || [])
const currentTopic = computed<TopicItem | null>(() => dashboard.value?.currentTopic || null)
const title = computed(() => dashboard.value ? `${dashboard.value.periodNo || ''} 三重一大会议`.trim() : '会议即将开始')

async function load() {
  dashboard.value = await api.dashboard(meetingId)
}

onMounted(() => {
  load().catch((error: any) => showFailToast(error.message || '加载失败'))
  timer = window.setInterval(() => load().catch(() => undefined), 5000)
})

onBeforeUnmount(() => window.clearInterval(timer))
</script>

<style scoped>
.progress-hero {
  color: #fff;
  background: #1a73e8;
  border: 0;
}

.pulse {
  display: block;
  width: 12px;
  height: 12px;
  margin-bottom: 10px;
  background: rgba(255, 255, 255, 0.55);
  border-radius: 50%;
}

.pulse.active {
  background: #fff;
  box-shadow: 0 0 0 8px rgba(255, 255, 255, 0.18);
}

h2,
h3 {
  margin: 0;
}

.progress-hero p {
  color: rgba(255, 255, 255, 0.8);
}

.progress-strip {
  margin-top: 18px;
  font-size: 20px;
}

.progress-strip span {
  margin-left: 4px;
  font-size: 13px;
  opacity: 0.8;
}
</style>
