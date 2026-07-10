<template>
  <article class="meeting-card" :class="`meeting-card--${statusTone}`">
    <header class="meeting-card__header">
      <StatusBadge :status="meeting.status" />
      <span>#{{ meeting.periodNo || meeting.id }}</span>
    </header>
    <button class="meeting-card__title" @click="$emit('detail', meeting)">
      {{ title }}
    </button>
    <div class="meeting-card__meta">
      <el-icon><Clock /></el-icon>
      <span>{{ meeting.meetingDate || '-' }} {{ meeting.meetingTime || '' }}</span>
    </div>
    <div class="meeting-card__meta">
      <el-icon><Location /></el-icon>
      <span>{{ meeting.location || '-' }}</span>
    </div>
    <p class="meeting-card__content">{{ meeting.content || '暂无会议内容' }}</p>
    <footer class="meeting-card__footer">
      <span>{{ topicCount }} 个议题</span>
      <div>
        <el-button link type="primary" @click="$emit('detail', meeting)">详情</el-button>
        <el-button v-if="editable" link type="primary" @click="$emit('edit', meeting)">编辑</el-button>
        <el-button v-if="consoleVisible" link type="primary" @click="$emit('console', meeting)">控制台</el-button>
      </div>
    </footer>
  </article>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Clock, Location } from '@element-plus/icons-vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import { MeetingItem } from '@/api/meeting'

const props = defineProps<{ meeting: MeetingItem; editable?: boolean; consoleVisible?: boolean }>()
defineEmits<{
  (e: 'detail', meeting: MeetingItem): void
  (e: 'edit', meeting: MeetingItem): void
  (e: 'console', meeting: MeetingItem): void
}>()

const title = computed(() => `${props.meeting.meetingDate || ''} ${props.meeting.periodNo || ''} 三重一大会议`.trim())
const topicCount = computed(() => props.meeting.topics?.length || Number((props.meeting as any).topicCount || 0))
const statusTone = computed(() => {
  const map: Record<string, string> = { DRAFT: 'gray', PUBLISHED: 'blue', IN_PROGRESS: 'green', FINISHED: 'slate' }
  return map[props.meeting.status] || 'gray'
})
</script>
