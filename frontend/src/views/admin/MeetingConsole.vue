<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>会议控制台</h2>
        <p class="muted">{{ dashboard?.periodNo }} {{ dashboard?.meetingDate }} {{ dashboard?.meetingTime }}</p>
      </div>
      <div>
        <el-button @click="$router.push(`/meetings/${meetingId}/detail`)">会议详情</el-button>
        <el-button @click="load">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats">
      <el-col :span="6"><div class="stat"><el-statistic title="总议题数" :value="dashboard?.topicCount || 0" /></div></el-col>
      <el-col :span="6"><div class="stat"><el-statistic title="已签到议题" :value="dashboard?.signedTopicCount || 0" /></div></el-col>
      <el-col :span="6"><div class="stat"><el-statistic title="签到人数" :value="dashboard?.signedCount || 0" /></div></el-col>
      <el-col :span="6"><div class="stat"><el-statistic title="参会名单人数" :value="dashboard?.attendeeCount || 0" /></div></el-col>
    </el-row>

    <el-table :data="topics" border>
      <el-table-column prop="sortNo" label="序号" width="70" />
      <el-table-column prop="title" label="议题" min-width="240" />
      <el-table-column prop="reportDepartmentName" label="汇报部门" width="160" />
      <el-table-column label="状态" width="110"><template #default="{ row }"><el-tag>{{ topicStatus(row.status) }}</el-tag></template></el-table-column>
      <el-table-column prop="actualMinutes" label="实际时长" width="110" />
      <el-table-column label="会议结论" min-width="260"><template #default="{ row }"><el-input v-model="conclusions[row.id]" type="textarea" :rows="2" placeholder="录入会议结论" /></template></el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" :disabled="row.status === 'RUNNING' || hasOtherRunning(row)" @click="start(row.id)">开始</el-button>
          <el-button size="small" type="warning" :disabled="row.status !== 'RUNNING'" @click="end(row.id)">结束</el-button>
          <el-button size="small" @click="saveConclusion(row.id)">保存结论</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
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

function hasOtherRunning(row: any) {
  return !!dashboard.value?.currentTopic && dashboard.value.currentTopic.id !== row.id
}

async function start(topicId: number) {
  try {
    await api.startTopic(topicId)
    ElMessage.success('议题已开始')
    await load()
  } catch (error: any) {
    ElMessage.error(error.message || '开始失败')
  }
}

async function end(topicId: number) {
  try {
    await api.endTopic(topicId)
    ElMessage.success('议题已结束')
    await load()
  } catch (error: any) {
    ElMessage.error(error.message || '结束失败')
  }
}

async function saveConclusion(topicId: number) {
  try {
    await api.saveConclusion(topicId, { conclusion: conclusions.value[topicId] || '' })
    ElMessage.success('结论已保存')
    await load()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

function topicStatus(status: string) {
  const map: Record<string, string> = { WAITING: '待讨论', RUNNING: '讨论中', FINISHED: '已结束' }
  return map[status] || status
}

onMounted(() => load().catch((error: any) => ElMessage.error(error.message || '加载失败')))
</script>

<style scoped>
.stats {
  margin-bottom: 18px;
}

.stat {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
}
</style>
