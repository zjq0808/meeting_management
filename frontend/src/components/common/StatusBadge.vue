<template>
  <span :class="['status-badge', `status-badge--${tone}`]">{{ text }}</span>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ status?: string; type?: 'meeting' | 'topic' }>()

const text = computed(() => {
  const meeting: Record<string, string> = {
    DRAFT: '待发布',
    PUBLISHED: '待开始',
    IN_PROGRESS: '进行中',
    FINISHED: '已结束'
  }
  const topic: Record<string, string> = {
    WAITING: '待讨论',
    RUNNING: '讨论中',
    FINISHED: '已结束'
  }
  return (props.type === 'topic' ? topic : meeting)[props.status || ''] || props.status || '-'
})

const tone = computed(() => {
  const map: Record<string, string> = {
    DRAFT: 'gray',
    PUBLISHED: 'blue',
    IN_PROGRESS: 'green',
    FINISHED: 'slate',
    WAITING: 'gray',
    RUNNING: 'blue'
  }
  return map[props.status || ''] || 'gray'
})
</script>
