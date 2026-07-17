<template>
  <div class="mobile-page signin-status-page">
    <van-nav-bar title="签到详情" left-arrow @click-left="$router.back()">
      <template #right>
        <van-icon name="replay" size="20" @click="reload" />
      </template>
    </van-nav-bar>

    <main class="mobile-content">
      <section class="signin-plate">
        <div>
          <span>签到进度</span>
          <strong>{{ signedTopicCount }}/{{ topicCount }}</strong>
        </div>
        <p>{{ dashboard?.periodNo || '会议' }} · {{ dashboard?.meetingDate || '-' }} {{ dashboard?.meetingTime || '' }}</p>
      </section>

      <section class="status-rail">
        <article
          v-for="topic in topicStats"
          :key="topic.id"
          class="status-row"
          :class="{ signed: topic.signed }"
        >
          <div class="status-mark">
            <span>{{ pad(topic.sortNo) }}</span>
          </div>
          <div class="status-main">
            <h3>{{ topic.title || '未命名议题' }}</h3>
            <p>{{ topic.reportDepartmentName || '未填写汇报部门' }}</p>
          </div>
          <van-tag :type="topic.signed ? 'success' : 'warning'" plain>
            {{ topic.signed ? '已签到' : '未签到' }}
          </van-tag>
        </article>
        <van-empty v-if="!loading && topicStats.length === 0" description="暂无议题签到记录" />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showFailToast } from 'vant'
import { api } from '@/api/meeting'

interface TopicSignStat {
  id?: number
  sortNo?: number
  title?: string
  reportDepartmentName?: string
  signed?: boolean
}

const route = useRoute()
const meetingId = computed(() => Number(route.params.id))
const dashboard = ref<any>(null)
const loading = ref(false)

const topicStats = computed<TopicSignStat[]>(() => dashboard.value?.topicSignStats || [])
const topicCount = computed(() => dashboard.value?.topicCount || topicStats.value.length || 0)
const signedTopicCount = computed(() => dashboard.value?.signedTopicCount || topicStats.value.filter((item) => item.signed).length)

async function reload() {
  loading.value = true
  try {
    dashboard.value = await api.signinDashboard(meetingId.value)
  } catch (error: any) {
    showFailToast(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function pad(value?: number) {
  return String(value || 0).padStart(2, '0')
}

onMounted(reload)
</script>

<style scoped>
.signin-status-page {
  background:
    linear-gradient(180deg, rgba(47, 125, 246, 0.14), rgba(247, 249, 252, 0) 220px),
    #f7f9fc;
}

.signin-plate {
  padding: 16px 16px 14px;
  color: #fff;
  background: #123b70;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  box-shadow: 0 10px 22px rgba(18, 59, 112, 0.16);
}

.signin-plate div {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
}

.signin-plate span {
  color: #bcd7ff;
  font-size: 12px;
  font-weight: 700;
}

.signin-plate strong {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 32px;
  line-height: 1;
}

.signin-plate p {
  margin: 10px 0 0;
  overflow: hidden;
  color: #d9e8ff;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-rail {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.status-row {
  display: grid;
  grid-template-columns: 36px minmax(0, 1fr) 62px;
  align-items: center;
  gap: 9px;
  padding: 11px 10px;
  background: #fff;
  border: 1px solid #e7ecf3;
  border-left: 3px solid #f59e0b;
  border-radius: 8px;
}

.status-row.signed {
  border-left-color: #13a66b;
}

.status-mark {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  background: #f4f7fb;
  border-radius: 7px;
}

.status-mark span {
  color: #123b70;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 12px;
  font-weight: 800;
}

.status-main {
  min-width: 0;
}

.status-main h3 {
  margin: 0;
  overflow: hidden;
  color: #172033;
  font-size: 14px;
  font-weight: 800;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-main p {
  margin: 4px 0 0;
  overflow: hidden;
  color: #667085;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
