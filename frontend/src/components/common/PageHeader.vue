<template>
  <header class="page-header">
    <div class="page-header__main">
      <el-button v-if="back" text :icon="ArrowLeft" class="page-header__back" @click="$emit('back')" />
      <div>
        <h1>{{ title }}</h1>
        <p v-if="subtitle">{{ subtitle }}</p>
      </div>
    </div>
    <div v-if="$slots.actions" class="page-header__actions">
      <slot name="actions" />
    </div>
  </header>
</template>

<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'

defineProps<{ title: string; subtitle?: string; back?: boolean }>()
defineEmits<{ (e: 'back'): void }>()
</script>
