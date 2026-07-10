<template>
  <div class="page signin-page">
    <PageHeader :title="title" :subtitle="subtitle" back @back="$router.push(`/meetings/${meetingId}/console`)">
      <template #actions>
        <el-button @click="$router.push(`/meetings/${meetingId}/detail`)">{{ texts.meetingDetail }}</el-button>
        <el-button :icon="Refresh" @click="reload">{{ texts.refresh }}</el-button>
      </template>
    </PageHeader>

    <section class="stats-grid">
      <StatCard :label="texts.totalTopics" :value="dashboard?.topicCount || 0" :icon="Tickets" />
      <StatCard :label="texts.signedTopics" :value="dashboard?.signedTopicCount || 0" :icon="CircleCheck" />
      <StatCard :label="texts.signedPeople" :value="dashboard?.signedCount || 0" :icon="User" />
    </section>

    <section class="signin-layout">
      <div class="surface qr-panel">
        <div class="section-head compact">
          <div>
            <h2>{{ texts.qrTitle }}</h2>
            <p class="muted">{{ texts.qrHint }}</p>
          </div>
          <el-button type="primary" :loading="qrLoading" @click="generateQr">{{ texts.refreshQr }}</el-button>
        </div>

        <div class="qr-box">
          <el-image v-if="qrImageUrl" :src="qrImageUrl" fit="contain" class="qr-image" />
          <el-empty v-else :description="texts.noQr" />
        </div>

        <div class="qr-meta">
          <span>{{ texts.expiresAt }}{{ formatDateTime(qr?.expiresAt) }}</span>
          <el-input v-model="signUrl" readonly>
            <template #append>
              <el-button :icon="Link" @click="copyUrl">{{ texts.copy }}</el-button>
            </template>
          </el-input>
        </div>
      </div>

      <div class="surface sign-table">
        <div class="section-head">
          <div>
            <h2>{{ texts.topicStatus }}</h2>
            <p class="muted">{{ texts.topicStatusHint }}</p>
          </div>
        </div>
        <el-table :data="dashboard?.topicSignStats || []" border>
          <el-table-column prop="sortNo" :label="texts.sequence" width="80" />
          <el-table-column prop="title" :label="texts.topic" min-width="260" />
          <el-table-column prop="reportDepartmentName" :label="texts.reportDepartment" width="180" />
          <el-table-column :label="texts.signStatus" width="140">
            <template #default="{ row }">
              <el-tag :type="row.signed ? 'success' : 'warning'">{{ row.signed ? texts.signed : texts.unsigned }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { CircleCheck, Link, Refresh, Tickets, User } from '@element-plus/icons-vue'
import PageHeader from '@/components/common/PageHeader.vue'
import StatCard from '@/components/common/StatCard.vue'
import { api, SignQrCodeResult } from '@/api/meeting'
import { createQrDataUrl } from '@/utils/qrCode'

const texts = {
  meetingDetail: '\u4f1a\u8bae\u8be6\u60c5',
  refresh: '\u5237\u65b0',
  totalTopics: '\u603b\u8bae\u9898\u6570',
  signedTopics: '\u5df2\u7b7e\u5230\u8bae\u9898\u6570',
  signedPeople: '\u5df2\u7b7e\u5230\u4eba\u5458\u8bb0\u5f55',
  qrTitle: '\u4f1a\u8bae\u7b7e\u5230\u4e8c\u7ef4\u7801',
  qrHint: '\u6c47\u62a5\u90e8\u95e8\u548c\u53c2\u4f1a\u90e8\u95e8\u4eba\u5458\u626b\u7801\u540e\uff0c\u53ea\u5c55\u793a\u672c\u4eba\u76f8\u5173\u8bae\u9898\u3002',
  refreshQr: '\u5237\u65b0\u4e8c\u7ef4\u7801',
  noQr: '\u6682\u65e0\u4e8c\u7ef4\u7801',
  expiresAt: '\u6709\u6548\u671f\u81f3\uff1a',
  copy: '\u590d\u5236',
  topicStatus: '\u8bae\u9898\u7b7e\u5230\u72b6\u6001',
  topicStatusHint: '\u6309\u8bae\u9898\u6c47\u62a5\u90e8\u95e8\u7b7e\u5230\u72b6\u6001\u7edf\u8ba1\u3002',
  sequence: '\u5e8f\u53f7',
  topic: '\u8bae\u9898',
  reportDepartment: '\u6c47\u62a5\u90e8\u95e8',
  signStatus: '\u7b7e\u5230\u72b6\u6001',
  signed: '\u5df2\u7b7e\u5230',
  unsigned: '\u672a\u7b7e\u5230',
  qrFail: '\u4e8c\u7ef4\u7801\u751f\u6210\u5931\u8d25',
  loadFail: '\u52a0\u8f7d\u5931\u8d25',
  copyOk: '\u5df2\u590d\u5236\u7b7e\u5230\u94fe\u63a5',
  copyFail: '\u5f53\u524d\u6d4f\u89c8\u5668\u4e0d\u652f\u6301\u81ea\u52a8\u590d\u5236\uff0c\u8bf7\u624b\u52a8\u590d\u5236\u94fe\u63a5',
  signinTitle: '\u4f1a\u8bae\u7b7e\u5230'
}

const route = useRoute()
const meetingId = computed(() => Number(route.params.id))
const dashboard = ref<any>(null)
const qr = ref<SignQrCodeResult | null>(null)
const signUrl = ref('')
const qrImageUrl = ref('')
const qrLoading = ref(false)

const title = computed(() => `${dashboard.value?.periodNo || ''} ${texts.signinTitle}`.trim())
const subtitle = computed(() => `${dashboard.value?.meetingDate || ''} ${dashboard.value?.meetingTime || ''}`)

async function load() {
  dashboard.value = await api.signinDashboard(meetingId.value)
}

async function generateQr() {
  qrLoading.value = true
  try {
    qr.value = await api.createSignQrCode(meetingId.value)
    signUrl.value = toAbsoluteUrl(qr.value.url)
    qrImageUrl.value = createQrDataUrl(signUrl.value)
  } catch (error: any) {
    ElMessage.error(error.message || texts.qrFail)
  } finally {
    qrLoading.value = false
  }
}

async function reload() {
  try {
    await load()
    await generateQr()
  } catch (error: any) {
    ElMessage.error(error.message || texts.loadFail)
  }
}

function toAbsoluteUrl(url: string) {
  if (/^https?:\/\//i.test(url)) return url
  return `${window.location.origin}${url.startsWith('/') ? url : `/${url}`}`
}

function formatDateTime(value?: string | Date | null) {
  if (!value) return '-'
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(0, 16)
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function copyUrl() {
  if (!signUrl.value) return
  try {
    await navigator.clipboard.writeText(signUrl.value)
    ElMessage.success(texts.copyOk)
  } catch (error) {
    ElMessage.warning(texts.copyFail)
  }
}

onMounted(reload)
</script>

<style scoped>
.signin-page {
  padding-bottom: 32px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 18px;
}

.signin-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.qr-panel,
.sign-table {
  padding: 20px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.section-head.compact {
  align-items: flex-start;
}

h2 {
  margin: 0 0 6px;
  font-size: 17px;
}

.qr-box {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 292px;
  margin: 16px 0;
  background: #f8fafc;
  border: 1px dashed #dbe4f0;
  border-radius: 16px;
}

.qr-image {
  width: 260px;
  height: 260px;
  padding: 12px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.08);
}

.qr-meta {
  display: grid;
  gap: 10px;
  color: #667085;
  font-size: 13px;
}

@media (max-width: 980px) {
  .signin-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>