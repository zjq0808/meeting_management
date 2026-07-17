<template>
  <div class="page">
    <div class="toolbar">
      <h2>会议看板</h2>
      <div>
        <el-button @click="$router.push('/meetings')">返回</el-button>
      </div>
    </div>

    <el-descriptions v-if="meeting" :column="4" border>
      <el-descriptions-item label="期数">{{ meeting.periodNo }}</el-descriptions-item>
      <el-descriptions-item label="状态">{{ meeting.status }}</el-descriptions-item>
      <el-descriptions-item label="签到">{{ meeting.signedCount || 0 }} / {{ meeting.attendeeCount || 0 }}</el-descriptions-item>
      <el-descriptions-item label="当前议题">{{ meeting.currentTopic?.sortNo || '未开始' }}</el-descriptions-item>
    </el-descriptions>

    <el-table v-if="meeting" :data="meeting.topics" border class="topic-table">
      <el-table-column prop="sortNo" label="序号" width="70" />
      <el-table-column prop="title" label="议题" min-width="220" />
      <el-table-column prop="reportDepartmentName" label="汇报部门" width="140" />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column prop="reportDepartmentSigned" label="汇报签到" width="110" />
      <el-table-column prop="actualMinutes" label="时长(分钟)" width="120" />
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-button size="small" type="primary" :disabled="row.status !== 'WAITING'" @click="start(row.id)">开始</el-button>
          <el-button size="small" type="warning" :disabled="row.status !== 'RUNNING'" @click="end(row.id)">结束</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script lang="ts" setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { api } from '@/api/meeting'

const route = useRoute()
const meetingId = Number(route.params.id)
const meeting = ref<any>(null)
let timer = 0

async function load() {
  meeting.value = await api.dashboard(meetingId)
}

async function start(topicId: number) {
  await api.startTopic(topicId)
  await load()
}

async function end(topicId: number) {
  await api.endTopic(topicId)
  await load()
}

onMounted(() => {
  load().catch((error: any) => ElMessage.error(error.message || '加载失败'))
  timer = window.setInterval(() => load().catch(() => undefined), 5000)
})

onBeforeUnmount(() => window.clearInterval(timer))
</script>

<style scoped>
.topic-table {
  margin-top: 16px;
}
</style>
