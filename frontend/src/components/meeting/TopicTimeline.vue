<template>
  <div class="topic-timeline">
    <article v-for="topic in topics" :key="topic.id || topic.sortNo" :class="['topic-timeline__item', `is-${topic.status || 'WAITING'}`]">
      <span class="topic-timeline__dot" />
      <div>
        <div class="topic-timeline__head">
          <StatusBadge :status="topic.status || 'WAITING'" type="topic" />
          <span v-if="topic.actualMinutes">{{ topic.actualMinutes }} 分钟</span>
        </div>
        <h4>第 {{ topic.sortNo }} 项：{{ topic.title }}</h4>
      </div>
    </article>
  </div>
</template>

<script setup lang="ts">
import StatusBadge from '@/components/common/StatusBadge.vue'
import { TopicItem } from '@/api/meeting'

defineProps<{ topics: TopicItem[] }>()
</script>
