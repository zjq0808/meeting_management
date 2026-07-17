<template>
  <div class="mobile-page signin-mobile">
    <header class="signin-header">
      <button class="icon-button" type="button" @click="$router.back()">
        <van-icon name="arrow-left" />
      </button>
      <h1>{{ texts.title }}</h1>
      <span class="header-space" />
    </header>

    <main class="signin-scroll">
      <van-empty v-if="!token" :description="texts.missingToken" />
      <template v-else>
        <section class="meeting-card">
          <p class="eyebrow">{{ texts.currentMeeting }}</p>
          <h2>{{ meetingTitle }}</h2>
          <div class="meeting-meta">
            <span><van-icon name="underway-o" />{{ meetingDateTime }}</span>
            <span><van-icon name="location-o" />{{ preview?.meeting?.location || '-' }}</span>
          </div>
          <div class="summary-row">
            <div>
              <strong>{{ topics.length }}</strong>
              <span>{{ texts.relatedTopics }}</span>
            </div>
            <div>
              <strong>{{ signedCount }}</strong>
              <span>{{ texts.signedTopics }}</span>
            </div>
          </div>
        </section>

        <section class="topic-section">
          <div class="section-title-mobile">
            <h3>{{ texts.myTopics }}</h3>
            <van-tag v-if="allSigned" type="success">{{ texts.allSigned }}</van-tag>
            <van-tag v-else type="primary">{{ texts.waitingSign }}</van-tag>
          </div>

          <van-skeleton v-if="loadingPreview" title :row="4" />
          <van-empty v-else-if="!topics.length" :description="preview?.message || texts.noTopics" />
          <div v-else class="topic-timeline">
            <article v-for="topic in topics" :key="topic.attendeeId || topic.id" class="topic-card" :class="{ signed: isSigned(topic) }">
              <div class="topic-dot">
                <van-icon :name="isSigned(topic) ? 'success' : 'clock-o'" />
              </div>
              <div class="topic-body">
                <div class="topic-topline">
                  <span>{{ texts.topicPrefix }} {{ topic.sortNo || '-' }} {{ texts.topicSuffix }}</span>
                  <van-tag :type="isSigned(topic) ? 'success' : 'warning'">{{ isSigned(topic) ? texts.signed : texts.unsigned }}</van-tag>
                </div>
                <h4>{{ topic.title }}</h4>
                <p>{{ topic.summary || texts.noSummary }}</p>
                <div class="topic-info">
                  <span>{{ texts.reportDepartment }}{{ topic.reportDepartmentName || '-' }}</span>
                  <span>{{ texts.attendeeType }}{{ attendeeTypeText(topic.attendeeType) }}</span>
                  <span v-if="topic.signedAt">{{ texts.signedAt }}{{ formatDateTime(topic.signedAt) }}</span>
                </div>
              </div>
            </article>
          </div>
        </section>
      </template>
    </main>

    <footer v-if="token" class="signin-footer">
      <div class="footer-status">
        <span>{{ texts.signStatus }}</span>
        <strong>{{ signedCount }} / {{ topics.length }}</strong>
      </div>
      <van-button block type="primary" size="large" :disabled="!canSign" :loading="submitting" @click="submit">
        {{ allSigned ? texts.signedButton : texts.signButton }}
      </van-button>
    </footer>
  </div>
</template>

<script lang="ts" setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { showFailToast, showSuccessToast } from 'vant'
import { api, SignInPreview } from '@/api/meeting'

const texts = {
  title: '\u4f1a\u8bae\u7b7e\u5230',
  missingToken: '\u7f3a\u5c11\u7b7e\u5230\u4e8c\u7ef4\u7801\u53c2\u6570',
  currentMeeting: '当前会议',
  relatedTopics: '\u76f8\u5173\u8bae\u9898',
  signedTopics: '\u5df2\u7b7e\u8bae\u9898',
  myTopics: '\u6211\u7684\u76f8\u5173\u8bae\u9898',
  allSigned: '\u5df2\u5168\u90e8\u7b7e\u5230',
  waitingSign: '\u5f85\u7b7e\u5230',
  noTopics: '\u5f53\u524d\u767b\u5f55\u4eba\u6682\u65e0\u76f8\u5173\u8bae\u9898',
  topicPrefix: '\u7b2c',
  topicSuffix: '\u9879',
  signed: '\u5df2\u7b7e\u5230',
  unsigned: '\u672a\u7b7e\u5230',
  noSummary: '\u6682\u65e0\u7b80\u8ff0',
  reportDepartment: '\u6c47\u62a5\u90e8\u95e8\uff1a',
  attendeeType: '\u53c2\u4f1a\u7c7b\u578b\uff1a',
  signedAt: '\u7b7e\u5230\u65f6\u95f4\uff1a',
  signStatus: '\u7b7e\u5230\u72b6\u6001',
  signButton: '\u786e\u8ba4\u7b7e\u5230',
  signedButton: '\u5df2\u5b8c\u6210\u7b7e\u5230',
  signSuccess: '\u7b7e\u5230\u6210\u529f',
  signFail: '\u7b7e\u5230\u5931\u8d25',
  loadFail: '\u52a0\u8f7d\u7b7e\u5230\u4fe1\u606f\u5931\u8d25',
  report: '\u6c47\u62a5\u4eba',
  partake: '\u53c2\u4f1a\u4eba'
}

const route = useRoute()
const token = computed(() => String(route.query.token || ''))
const preview = ref<SignInPreview | null>(null)
const loadingPreview = ref(false)
const submitting = ref(false)

const topics = computed(() => preview.value?.topics || [])
const signedCount = computed(() => topics.value.filter(isSigned).length)
const allSigned = computed(() => topics.value.length > 0 && signedCount.value === topics.value.length)
const canSign = computed(() => !!preview.value?.canSign && !allSigned.value && !loadingPreview.value)
const meetingTitle = computed(() => preview.value?.meeting?.periodNo ? `${preview.value.meeting.meetingDate || ''} ${preview.value.meeting.periodNo}` : texts.title)
const meetingDateTime = computed(() => `${preview.value?.meeting?.meetingDate || '-'} ${preview.value?.meeting?.meetingTime || ''}`.trim())

function isSigned(topic: any) {
  return topic?.signed === true || topic?.signStatus === 'SIGNED'
}

function attendeeTypeText(type?: string) {
  const value = String(type || '').toUpperCase()
  if (value === 'REPORT') return texts.report
  if (value === 'PARTAKE') return texts.partake
  return value || '-'
}

function formatDateTime(value?: string | Date | null) {
  if (!value) return '-'
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) return String(value).replace('T', ' ').slice(0, 16)
  const pad = (num: number) => String(num).padStart(2, '0')
  return `${date.getFullYear()}/${pad(date.getMonth() + 1)}/${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

async function loadPreview() {
  if (!token.value) return
  loadingPreview.value = true
  try {
    preview.value = await api.signInPreview(token.value)
  } catch (error: any) {
    showFailToast(error.message || texts.loadFail)
  } finally {
    loadingPreview.value = false
  }
}

async function submit() {
  if (!canSign.value) return
  submitting.value = true
  try {
    await api.signIn({ token: token.value })
    showSuccessToast(texts.signSuccess)
    await loadPreview()
  } catch (error: any) {
    showFailToast(error.message || texts.signFail)
  } finally {
    submitting.value = false
  }
}

onMounted(loadPreview)
</script>

<style scoped>
.signin-mobile {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background: #f5f7fa;
}

.signin-header {
  display: flex;
  flex: none;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 14px;
  color: #fff;
  background: #1a73e8;
  box-shadow: 0 6px 18px rgba(26, 115, 232, 0.18);
}

.signin-header h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.icon-button,
.header-space {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  color: inherit;
  background: transparent;
  border: 0;
}

.signin-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 14px 14px 104px;
  -webkit-overflow-scrolling: touch;
}

.meeting-card,
.topic-section {
  margin-bottom: 14px;
  padding: 18px;
  background: #fff;
  border: 1px solid #edf1f7;
  border-radius: 18px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
}

.eyebrow {
  margin: 0 0 6px;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.meeting-card h2 {
  margin: 0 0 12px;
  color: #111827;
  font-size: 17px;
  line-height: 1.35;
}

.meeting-meta {
  display: grid;
  gap: 6px;
  color: #64748b;
  font-size: 13px;
}

.meeting-meta span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-top: 16px;
}

.summary-row div {
  padding: 12px;
  text-align: center;
  background: #f8fafc;
  border-radius: 14px;
}

.summary-row strong {
  display: block;
  color: #1a73e8;
  font-size: 24px;
  line-height: 1;
}

.summary-row span {
  display: block;
  margin-top: 5px;
  color: #64748b;
  font-size: 12px;
}

.section-title-mobile {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.section-title-mobile h3 {
  margin: 0;
  color: #111827;
  font-size: 16px;
}

.topic-timeline {
  position: relative;
  display: grid;
  gap: 12px;
}

.topic-card {
  position: relative;
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 10px;
}

.topic-card:not(:last-child)::before {
  position: absolute;
  top: 28px;
  bottom: -14px;
  left: 13px;
  width: 2px;
  background: #e5e7eb;
  content: '';
}

.topic-dot {
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: #fff;
  background: #1a73e8;
  border: 4px solid #fff;
  border-radius: 50%;
  box-shadow: 0 6px 14px rgba(26, 115, 232, 0.18);
}

.topic-card.signed .topic-dot {
  background: #22c55e;
}

.topic-body {
  min-width: 0;
  padding: 13px;
  background: #f8fafc;
  border: 1px solid #edf1f7;
  border-left: 4px solid #1a73e8;
  border-radius: 14px;
}

.topic-card.signed .topic-body {
  border-left-color: #22c55e;
}

.topic-topline {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 12px;
}

.topic-body h4 {
  margin: 0 0 6px;
  color: #111827;
  font-size: 14px;
  line-height: 1.45;
}

.topic-body p {
  margin: 0 0 10px;
  color: #667085;
  font-size: 12px;
  line-height: 1.5;
}

.topic-info {
  display: grid;
  gap: 4px;
  color: #64748b;
  font-size: 12px;
}

.signin-footer {
  position: fixed;
  right: 0;
  bottom: 0;
  left: 0;
  padding: 10px 14px calc(14px + env(safe-area-inset-bottom));
  background: rgba(255, 255, 255, 0.96);
  border-top: 1px solid #edf1f7;
  box-shadow: 0 -10px 28px rgba(15, 23, 42, 0.08);
}

.footer-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 12px;
}

.footer-status strong {
  color: #111827;
}
</style>
