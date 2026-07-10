<template>
  <section class="stat-card">
    <div>
      <span>{{ label }}</span>
      <strong>{{ value }}</strong>
    </div>
    <el-icon v-if="icon" class="stat-card__icon"><component :is="icon" /></el-icon>
  </section>
</template>

<script setup lang="ts">
defineProps<{ label: string; value: string | number; icon?: unknown }>()
</script>
