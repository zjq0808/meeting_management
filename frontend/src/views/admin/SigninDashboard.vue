<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>会议签到</h2>
        <p class="muted">{{ dashboard?.periodNo }} {{ dashboard?.meetingDate }} {{ dashboard?.meetingTime }}</p>
      </div>
      <div>
        <el-button @click="$router.push(`/meetings/${meetingId}/detail`)">会议详情</el-button>
        <el-button type="primary" @click="load">刷新</el-button>
      </div>
    </div>

    <div class="signin-layout">
      <section class="sign-stats">
        <el-statistic title="总议题数" :value="dashboard?.topicCount || 0" />
        <el-statistic title="已签到议题数" :value="dashboard?.signedTopicCount || 0" />
        <el-statistic title="已签到人员记录" :value="dashboard?.signedCount || 0" />
      </section>
    </div>

    <el-table :data="dashboard?.topicSignStats || []" border>
      <el-table-column prop="sortNo" label="序号" width="80" />
      <el-table-column prop="title" label="议题" min-width="260" />
      <el-table-column prop="reportDepartmentName" label="汇报部门" width="180" />
      <el-table-column label="签到状态" width="140">
        <template #default="{ row }"><el-tag :type="row.signed ? 'success' : 'warning'">{{ row.signed ? '已签到' : '未签到' }}</el-tag></template>
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

async function load() {
  dashboard.value = await api.signinDashboard(meetingId.value)
}

onMounted(async () => {
  try {
    await load()
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  }
})
</script>

<style scoped>
.signin-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
  margin-bottom: 18px;
}

.sign-stats {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
}

.sign-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  align-items: center;
}
</style>
