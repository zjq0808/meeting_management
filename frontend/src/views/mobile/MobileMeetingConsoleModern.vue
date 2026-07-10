<template>
  <div class="mobile-page">
    <van-nav-bar title="会议控制台" left-arrow @click-left="$router.back()" />
    <main class="mobile-content">
      <section class="mobile-card">
        <h2>{{ dashboard?.periodNo || '会议' }}</h2>
        <p>{{ dashboard?.meetingDate }} {{ dashboard?.meetingTime }} · {{ dashboard?.location }}</p>
        <div class="mini-stats">
          <span>议题 {{ dashboard?.topicCount || 0 }}</span>
          <span>签到 {{ dashboard?.signedTopicCount || 0 }}/{{ dashboard?.topicCount || 0 }}</span>
        </div>
      </section>

      <section class="mobile-card">
        <h3>议题控制</h3>
        <van-cell
          v-for="topic in topics"
          :key="topic.id"
          :title="`第 ${topic.sortNo} 项`"
          :label="topic.title"
          @click="selectTopic(topic)"
        >
          <template #value>
            <van-button size="mini" type="primary" :disabled="topic.status === 'RUNNING' || hasRunning(topic)" @click.stop="start(topic.id)">开始</van-button>
            <van-button size="mini" type="warning" :disabled="topic.status !== 'RUNNING'" @click.stop="end(topic.id)">结束</van-button>
          </template>
        </van-cell>
        <div v-for="topic in topics" :key="`time-${topic.id}`" class="topic-time">
          <strong>第 {{ topic.sortNo }} 项</strong>
          <span>汇报部门：{{ topic.reportDepartmentName || '-' }}</span>
          <span>开始：{{ formatDateTime(topic.startTime) }}</span>
          <span>结束：{{ formatDateTime(topic.endTime) }}</span>
          <span>实际时长：{{ topic.actualMinutes || 0 }} 分钟</span>
        </div>
      </section>

      <section class="mobile-card">
        <h3>会议结论</h3>
        <div v-if="currentTopic" class="conclusion-mobile">
          <strong>第 {{ currentTopic.sortNo }} 项：{{ currentTopic.title }}</strong>
          <van-field v-model="currentConclusion" autosize type="textarea" rows="3" placeholder="录入会议结论" @update:model-value="conclusionDirty = true" />
          <van-button block size="small" type="primary" @click="saveConclusion(currentTopic.id)">保存结论</van-button>
        </div>
        <van-empty v-else description="暂无议题" />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { api, TopicItem } from '@/api/meeting'

const route = useRoute()
const meetingId = computed(() => Number(route.params.id))
const dashboard = ref<any>(null)
const selectedTopicId = ref<number | null>(null)
const currentConclusion = ref('')
const conclusionDirty = ref(false)
let timer = 0

const topics = computed<TopicItem[]>(() => dashboard.value?.topics || [])
const currentTopic = computed<TopicItem | null>(() => {
  const selected = topics.value.find((item) => item.id === selectedTopicId.value)
  return selected || dashboard.value?.currentTopic || topics.value.find((item) => item.status === 'WAITING') || topics.value[0] || null
})

watch(
  () => currentTopic.value?.id,
  () => syncConclusionFromTopic(true)
)

async function load(options: { preserveDraft?: boolean } = {}) {
  dashboard.value = await api.dashboard(meetingId.value)
  if (!selectedTopicId.value && currentTopic.value?.id) selectedTopicId.value = currentTopic.value.id
  if (!options.preserveDraft || !conclusionDirty.value) syncConclusionFromTopic(false)
}

function selectTopic(topic: TopicItem) {
  if (!topic.id) return
  selectedTopicId.value = topic.id
}

function syncConclusionFromTopic(force: boolean) {
  if (!force && conclusionDirty.value) return
  currentConclusion.value = currentTopic.value?.conclusion || ''
  conclusionDirty.value = false
}

function hasRunning(row: any) {
  return !!dashboard.value?.currentTopic && dashboard.value.currentTopic.id !== row.id
}

function formatDateTime(value?: string | Date | null) {
  if (!value) return '-'
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(0, 16).replace(/-/g, '/')
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function start(topicId?: number) {
  if (!topicId) return
  try {
    await api.startTopic(topicId)
    selectedTopicId.value = topicId
    showSuccessToast('已开始')
    await load({ preserveDraft: true })
  } catch (error: any) {
    showFailToast(error.message || '开始失败')
  }
}

async function end(topicId?: number) {
  if (!topicId) return
  try {
    await api.endTopic(topicId)
    selectedTopicId.value = topicId
    showSuccessToast('已结束')
    await load({ preserveDraft: true })
  } catch (error: any) {
    showFailToast(error.message || '结束失败')
  }
}

async function saveConclusion(topicId?: number) {
  if (!topicId) return
  try {
    await api.saveConclusion(topicId, { conclusion: currentConclusion.value, actualMinutes: currentTopic.value?.actualMinutes })
    conclusionDirty.value = false
    showSuccessToast('已保存')
    await load()
  } catch (error: any) {
    showFailToast(error.message || '保存失败')
  }
}

onMounted(() => {
  load().catch((error: any) => showFailToast(error.message || '加载失败'))
  timer = window.setInterval(() => load({ preserveDraft: true }).catch(() => undefined), 5000)
})

onBeforeUnmount(() => window.clearInterval(timer))
</script>

<style scoped>
h2,
h3 {
  margin: 0 0 10px;
}

p {
  color: #667085;
}

.mini-stats {
  display: flex;
  gap: 10px;
}

.mini-stats span {
  padding: 4px 9px;
  color: #175cd3;
  background: #eff6ff;
  border-radius: 999px;
  font-size: 12px;
}

.topic-time {
  display: grid;
  gap: 4px;
  padding: 10px 16px;
  color: #64748b;
  border-top: 1px solid #edf1f7;
  font-size: 12px;
}

.topic-time strong {
  color: #111827;
  font-size: 13px;
}

.conclusion-mobile {
  padding: 12px 0;
  border-top: 1px solid #edf1f7;
}
</style>
