<template>
  <div class="mobile-page">
    <van-nav-bar title="会议控制台" left-text="返回" left-arrow @click-left="$router.back()" />

    <van-cell-group inset>
      <van-cell title="会议期数" :value="dashboard?.periodNo" />
      <van-cell title="当前状态" :value="statusLabel(dashboard?.status)" />
      <van-cell title="签到议题" :value="`${dashboard?.signedTopicCount || 0}/${dashboard?.topicCount || 0}`" />
    </van-cell-group>

    <van-cell-group inset title="议题控制">
      <van-cell v-for="topic in topics" :key="topic.id" :title="`第 ${topic.sortNo} 项：${topic.title}`" :label="topicStatus(topic.status)">
        <template #value>
          <van-button size="mini" type="primary" :disabled="topic.status === 'RUNNING' || hasRunning(topic)" @click="start(topic.id)">开始</van-button>
          <van-button size="mini" type="warning" :disabled="topic.status !== 'RUNNING'" @click="end(topic.id)">结束</van-button>
        </template>
      </van-cell>
    </van-cell-group>

    <van-cell-group inset title="会议结论">
      <div v-for="topic in topics" :key="`conclusion-${topic.id}`" class="conclusion-item">
        <div class="conclusion-title">第 {{ topic.sortNo }} 项</div>
        <van-field v-model="conclusions[topic.id]" autosize type="textarea" rows="2" placeholder="录入会议结论" />
        <van-button block size="small" type="primary" @click="saveConclusion(topic.id)">保存结论</van-button>
      </div>
    </van-cell-group>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { api } from '@/api/meeting'

const route = useRoute()
const meetingId = computed(() => Number(route.params.id))
const dashboard = ref<any>(null)
const conclusions = ref<Record<number, string>>({})
const topics = computed(() => dashboard.value?.topics || [])

async function load() {
  dashboard.value = await api.dashboard(meetingId.value)
  const next: Record<number, string> = {}
  topics.value.forEach((topic: any) => { next[topic.id] = topic.conclusion || '' })
  conclusions.value = next
}

function hasRunning(row: any) {
  return !!dashboard.value?.currentTopic && dashboard.value.currentTopic.id !== row.id
}

async function start(topicId: number) {
  try {
    await api.startTopic(topicId)
    showSuccessToast('已开始')
    await load()
  } catch (error: any) {
    showFailToast(error.message || '开始失败')
  }
}

async function end(topicId: number) {
  try {
    await api.endTopic(topicId)
    showSuccessToast('已结束')
    await load()
  } catch (error: any) {
    showFailToast(error.message || '结束失败')
  }
}

async function saveConclusion(topicId: number) {
  try {
    await api.saveConclusion(topicId, { conclusion: conclusions.value[topicId] || '' })
    showSuccessToast('已保存')
    await load()
  } catch (error: any) {
    showFailToast(error.message || '保存失败')
  }
}

function statusLabel(status?: string) {
  const map: Record<string, string> = { DRAFT: '待发布', PUBLISHED: '待开始', IN_PROGRESS: '进行中', FINISHED: '已结束' }
  return status ? map[status] || status : ''
}

function topicStatus(status: string) {
  const map: Record<string, string> = { WAITING: '待讨论', RUNNING: '讨论中', FINISHED: '已结束' }
  return map[status] || status
}

onMounted(() => load().catch((error: any) => showFailToast(error.message || '加载失败')))
</script>

<style scoped>
.van-cell-group {
  margin-top: 12px;
}

.conclusion-item {
  padding: 12px 16px 16px;
  border-top: 1px solid #f1f5f9;
}

.conclusion-title {
  margin-bottom: 8px;
  font-weight: 600;
}
</style>
