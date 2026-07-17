<template>
  <div class="mobile-page console-mobile-page">
    <van-nav-bar title="会议控制台" left-arrow @click-left="$router.back()" />
    <main class="mobile-content">
      <section class="mobile-card console-summary">
        <h2>{{ dashboard?.periodNo || '会议' }}</h2>
        <p>{{ dashboard?.meetingDate }} {{ dashboard?.meetingTime }} · {{ dashboard?.location }}</p>
        <div class="mini-stats">
          <span>议题 {{ dashboard?.topicCount || 0 }}</span>
          <span>签到 {{ dashboard?.signedTopicCount || 0 }}/{{ dashboard?.topicCount || 0 }}</span>
        </div>
        <van-button block plain type="primary" class="signin-detail-link" @click="$router.push(`/mobile/meetings/${meetingId}/signin-dashboard`)">
          查看签到详情
        </van-button>
      </section>

      <section class="mobile-card console-topics">
        <h3>议题控制</h3>
        <article
          v-for="topic in topics"
          :key="topic.id"
          class="console-topic"
          :class="{ active: topic.id === currentTopic?.id }"
          @click="selectTopic(topic)"
        >
          <div class="topic-index">第 {{ topic.sortNo }} 项</div>
          <div class="topic-main">
            <strong>{{ topic.title }}</strong>
            <span>汇报：{{ topic.reportDepartmentName || '-' }}</span>
            <span>参会：{{ topic.participantDepartments || topic.participantDeptName || '-' }}</span>
          </div>
          <div class="topic-controls">
            <em>{{ topicStatusLabel(topic.status) }}</em>
            <van-button size="mini" type="primary" :disabled="topic.status === 'RUNNING' || hasRunning(topic)" @click.stop="start(topic.id)">开始</van-button>
            <van-button size="mini" type="warning" :disabled="topic.status !== 'RUNNING'" @click.stop="end(topic.id)">结束</van-button>
          </div>
        </article>
      </section>

      <section class="mobile-card topic-detail-card">
        <h3>当前议题</h3>
        <div v-if="currentTopic" class="current-topic">
          <strong>第 {{ currentTopic.sortNo }} 项：{{ currentTopic.title }}</strong>
          <div class="topic-time">
            <span>汇报部门<strong>{{ currentTopic.reportDepartmentName || '-' }}</strong></span>
            <span>参会部门<strong>{{ currentTopic.participantDepartments || currentTopic.participantDeptName || '-' }}</strong></span>
            <span>开始时间<strong>{{ formatDateTime(currentTopic.startTime) }}</strong></span>
            <span>结束时间<strong>{{ formatDateTime(currentTopic.endTime) }}</strong></span>
            <span>实际时长<strong>{{ currentTopic.actualMinutes || 0 }} 分钟</strong></span>
          </div>
          <div class="conclusion-mobile">
            <div class="conclusion-head">
              <label>会议结论</label>
              <van-button v-if="isAdmin" size="mini" type="primary" plain @click="saveConclusion">保存结论</van-button>
            </div>
            <van-field
              v-if="isAdmin"
              v-model="conclusionDraft"
              autosize
              type="textarea"
              rows="3"
              placeholder="请输入会议结论"
              @update:model-value="conclusionDirty = true"
            />
            <p v-else>{{ currentTopic.conclusion || '暂未录入会议结论' }}</p>
          </div>
        </div>
        <van-empty v-else description="暂无议题" />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { showFailToast, showSuccessToast } from 'vant'
import { api, TopicItem } from '@/api/meeting'

const route = useRoute()
const store = useStore()
const meetingId = computed(() => Number(route.params.id))
const currentUser = computed(() => store.state.user)
const dashboard = ref<any>(null)
const selectedTopicId = ref<number | null>(null)
const conclusionDraft = ref('')
const conclusionDirty = ref(false)
let timer = 0

const topics = computed<TopicItem[]>(() => dashboard.value?.topics || [])
const currentTopic = computed<TopicItem | null>(() => {
  const selected = topics.value.find((item) => item.id === selectedTopicId.value)
  return selected || dashboard.value?.currentTopic || topics.value.find((item) => item.status === 'WAITING') || topics.value[0] || null
})
const isAdmin = computed(() => currentUser.value?.role === 'ADMIN')

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
  syncConclusionFromTopic(true)
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

function topicStatusLabel(status?: string) {
  const map: Record<string, string> = { WAITING: '待讨论', RUNNING: '讨论中', FINISHED: '已结束' }
  return map[status || ''] || '待讨论'
}

function syncConclusionFromTopic(force: boolean) {
  if (!force && conclusionDirty.value) return
  conclusionDraft.value = currentTopic.value?.conclusion || ''
  conclusionDirty.value = false
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

async function saveConclusion() {
  if (!currentTopic.value?.id) return
  try {
    await api.updateTopic(currentTopic.value.id, {
      title: currentTopic.value.title || '',
      topicType: currentTopic.value.topicType || '',
      summary: currentTopic.value.summary || '',
      conclusion: conclusionDraft.value
    })
    conclusionDirty.value = false
    showSuccessToast('会议结论已保存')
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
  margin: 0 0 8px;
}

p {
  color: #667085;
}

.console-mobile-page {
  background:
    linear-gradient(180deg, rgba(18, 59, 112, 0.12), rgba(247, 249, 252, 0) 210px),
    #f7f9fc;
}

.console-summary {
  color: #fff;
  background: #123b70;
  border: 1px solid rgba(255, 255, 255, 0.14);
  box-shadow: 0 10px 22px rgba(18, 59, 112, 0.16);
}

.console-summary h2 {
  color: #fff;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.35;
}

.console-summary p {
  margin: 3px 0 9px;
  color: #d9e8ff;
  font-size: 12px;
}

.mini-stats {
  display: flex;
  gap: 7px;
}

.signin-detail-link {
  height: 34px;
  margin-top: 11px;
  border-radius: 7px;
  font-weight: 700;
}

.mini-stats span {
  padding: 3px 8px;
  color: #d9e8ff;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 5px;
  font-size: 12px;
  font-weight: 700;
}

.console-topics h3,
.topic-detail-card h3 {
  color: #111827;
  font-size: 15px;
  font-weight: 800;
}

.console-topic {
  display: grid;
  grid-template-columns: 58px minmax(0, 1fr) auto;
  gap: 8px;
  padding: 10px 0;
  border-top: 1px solid #eef2f6;
}

.console-topic:first-of-type {
  border-top: 0;
}

.console-topic.active {
  margin: 0 -6px;
  padding-right: 6px;
  padding-left: 6px;
  background: #f3f8ff;
  border-left: 3px solid #2f7df6;
  border-radius: 8px;
}

.topic-index {
  color: #175cd3;
  font-size: 12px;
  font-weight: 800;
}

.topic-main {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.topic-main strong {
  overflow: hidden;
  color: #172033;
  font-size: 13px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-main span {
  overflow: hidden;
  color: #667085;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-controls {
  display: grid;
  justify-items: end;
  gap: 5px;
}

.topic-controls em {
  color: #98a2b3;
  font-size: 11px;
  font-style: normal;
}

.current-topic > strong {
  display: block;
  color: #172033;
  font-size: 14px;
  line-height: 1.4;
}

.topic-time {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 7px;
  margin-top: 9px;
}

.topic-time span {
  display: grid;
  gap: 3px;
  padding: 8px;
  color: #8a94a6;
  background: #f7f9fc;
  border: 1px solid #eef2f6;
  border-radius: 7px;
  font-size: 11px;
}

.topic-time span:last-child {
  grid-column: 1 / -1;
}

.topic-time strong {
  overflow: hidden;
  color: #172033;
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.conclusion-mobile {
  margin-top: 10px;
  padding-top: 9px;
  border-top: 1px solid #eef2f6;
}

.conclusion-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.conclusion-mobile label {
  color: #8a94a6;
  font-size: 11px;
}

.conclusion-mobile :deep(.van-cell) {
  padding: 8px 9px;
  background: #f7f9fc;
  border: 1px solid #eef2f6;
  border-radius: 7px;
}

.conclusion-mobile p {
  margin: 5px 0 0;
  color: #475467;
  font-size: 12px;
  line-height: 1.55;
}
</style>
