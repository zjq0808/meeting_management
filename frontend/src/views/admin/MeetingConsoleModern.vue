<template>
  <div class="page page-wide">
    <PageHeader :title="consoleTitle" :subtitle="subtitle" back @back="$router.push('/meetings')">
      <template #actions>
        <el-button @click="$router.push(`/meetings/${meetingId}/detail`)">会议详情</el-button>
        <el-button @click="$router.push(`/meetings/${meetingId}/signin-dashboard`)">签到看板</el-button>
        <el-button :icon="Refresh" @click="load({ preserveDraft: true })">刷新</el-button>
      </template>
    </PageHeader>

    <section class="stats-grid">
      <StatCard label="总议题数" :value="dashboard?.topicCount || 0" :icon="Tickets" />
      <StatCard label="已签到议题" :value="dashboard?.signedTopicCount || 0" :icon="CircleCheck" />
      <StatCard label="签到人数" :value="dashboard?.signedCount || 0" :icon="User" />
      <StatCard label="参会名单人数" :value="dashboard?.attendeeCount || 0" :icon="UserFilled" />
    </section>

    <main class="console-layout">
      <aside class="surface topic-side">
        <div class="side-head">
          <h2>议题列表</h2>
          <span>{{ runningIndex }} / {{ topics.length }}</span>
        </div>

        <article
          v-for="topic in topics"
          :key="topic.id || topic.sortNo"
          :class="['topic-row', { active: currentTopic?.id === topic.id }]"
          @click="selectTopic(topic)"
        >
          <div class="topic-row__head">
            <StatusBadge :status="topic.status || 'WAITING'" type="topic" />
            <span v-if="topic.actualMinutes">{{ topic.actualMinutes }} 分钟</span>
          </div>
          <h3>第 {{ topic.sortNo }} 项：{{ topic.title }}</h3>
          <div class="topic-row__meta">
            <span>汇报部门：{{ topic.reportDepartmentName || '-' }}</span>
            <span>开始：{{ formatDateTime(topic.startTime) }}</span>
            <span>结束：{{ formatDateTime(topic.endTime) }}</span>
          </div>
        </article>
      </aside>

      <section class="surface control-panel">
        <div v-if="currentTopic" class="current-topic">
          <StatusBadge :status="currentTopic.status" type="topic" />
          <h2>{{ currentTopic.title }}</h2>
          <div class="topic-meta">
            <span>汇报部门：{{ currentTopic.reportDepartmentName || '-' }}</span>
            <span>开始时间：{{ formatDateTime(currentTopic.startTime) }}</span>
            <span>结束时间：{{ formatDateTime(currentTopic.endTime) }}</span>
            <span>实际时长：{{ currentTopic.actualMinutes || 0 }} 分钟</span>
          </div>
        </div>
        <el-empty v-else description="请选择或开始一个议题" />

        <div class="quick-actions">
          <el-button @click="appendConclusion('同意')">同意</el-button>
          <el-button @click="appendConclusion('原则同意')">原则同意</el-button>
          <el-button @click="appendConclusion('不同意')">不同意</el-button>
          <el-button @click="appendConclusion('进一步论证')">进一步论证</el-button>
        </div>
        <el-input
          v-model="currentConclusion"
          type="textarea"
          :rows="8"
          resize="none"
          placeholder="录入当前议题会议结论"
          @input="conclusionDirty = true"
        />

        <div class="control-actions">
          <el-button
            type="primary"
            size="large"
            :disabled="!currentTopic || currentTopic.status === 'RUNNING' || hasOtherRunning(currentTopic)"
            @click="start(currentTopic?.id)"
          >
            开始议题
          </el-button>
          <el-button
            type="warning"
            size="large"
            :disabled="!currentTopic || currentTopic.status !== 'RUNNING'"
            @click="end(currentTopic?.id)"
          >
            结束议题
          </el-button>
          <el-button size="large" :disabled="!currentTopic" @click="saveConclusion(currentTopic?.id)">
            保存结论
          </el-button>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck, Refresh, Tickets, User, UserFilled } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
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
const consoleTitle = computed(() => {
  if (!dashboard.value) return '会议控制台'
  return `${dashboard.value.meetingDate || ''} ${dashboard.value.periodNo || ''} 三重一大会议`.trim()
})
const subtitle = computed(() => dashboard.value ? `${dashboard.value.meetingTime || ''} · ${dashboard.value.location || ''}` : '')
const runningIndex = computed(() => currentTopic.value?.sortNo || 0)

watch(
  () => currentTopic.value?.id,
  () => syncConclusionFromTopic(true)
)

async function load(options: { preserveDraft?: boolean } = {}) {
  dashboard.value = await api.dashboard(meetingId.value)
  if (!selectedTopicId.value && currentTopic.value?.id) {
    selectedTopicId.value = currentTopic.value.id
  }
  if (!options.preserveDraft || !conclusionDirty.value) {
    syncConclusionFromTopic(false)
  }
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

function hasOtherRunning(row: any) {
  return !!dashboard.value?.currentTopic && dashboard.value.currentTopic.id !== row.id
}

function appendConclusion(text: string) {
  currentConclusion.value = currentConclusion.value ? `${currentConclusion.value}\n${text}` : text
  conclusionDirty.value = true
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
    ElMessage.success('议题已开始')
    await load({ preserveDraft: true })
  } catch (error: any) {
    ElMessage.error(error.message || '开始失败')
  }
}

async function end(topicId?: number) {
  if (!topicId) return
  try {
    await api.endTopic(topicId)
    selectedTopicId.value = topicId
    ElMessage.success('议题已结束')
    await load({ preserveDraft: true })
  } catch (error: any) {
    ElMessage.error(error.message || '结束失败')
  }
}

async function saveConclusion(topicId?: number) {
  if (!topicId) return
  try {
    await api.saveConclusion(topicId, { conclusion: currentConclusion.value, actualMinutes: currentTopic.value?.actualMinutes })
    conclusionDirty.value = false
    ElMessage.success('结论已保存')
    await load()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

onMounted(() => {
  load().catch((error: any) => ElMessage.error(error.message || '加载失败'))
  timer = window.setInterval(() => load({ preserveDraft: true }).catch(() => undefined), 5000)
})

onBeforeUnmount(() => window.clearInterval(timer))
</script>

<style scoped>
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.console-layout {
  display: grid;
  grid-template-columns: 420px minmax(0, 1fr);
  gap: 18px;
}

.topic-side,
.control-panel {
  padding: 20px;
}

.side-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 18px;
}

h2,
h3 {
  margin: 0;
}

.topic-row {
  padding: 14px 14px 16px;
  border: 1px solid #e8edf5;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.topic-row + .topic-row {
  margin-top: 12px;
}

.topic-row:hover,
.topic-row.active {
  border-color: #3b82f6;
  box-shadow: 0 10px 24px rgba(30, 64, 175, 0.1);
  transform: translateY(-1px);
}

.topic-row__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
  color: #64748b;
  font-size: 13px;
}

.topic-row h3 {
  font-size: 15px;
  line-height: 1.5;
  color: #111827;
}

.topic-row__meta {
  display: grid;
  grid-template-columns: 1fr;
  gap: 5px;
  margin-top: 10px;
  color: #64748b;
  font-size: 13px;
}

.current-topic h2 {
  margin: 18px 0 14px;
  font-size: 28px;
  line-height: 1.35;
}

.topic-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
  margin-bottom: 22px;
  color: #667085;
}

.quick-actions,
.control-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 16px 0;
}

.control-actions {
  justify-content: center;
  margin-top: 22px;
}

@media (max-width: 1100px) {
  .stats-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .console-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 620px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
