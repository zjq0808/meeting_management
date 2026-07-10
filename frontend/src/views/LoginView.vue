<template>
  <div class="login-page">
    <section class="login-panel">
      <h1>技术局三重一大会议</h1>
      <p class="muted">本地调试使用企业微信用户 ID 登录，后端根据权限表判断角色。</p>
      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="企业微信用户 ID">
          <el-input v-model="form.userId" autocomplete="username" placeholder="如 U0001" />
        </el-form-item>
        <el-button type="primary" :loading="loading" @click="login">登录</el-button>
      </el-form>
      <p class="demo">演示：U0001 管理员，U0002 秘书，U0004 科组长，U0006 参会人</p>
    </section>
  </div>
</template>

<script lang="ts" setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'

const store = useStore()
const router = useRouter()
const route = useRoute()
const loading = ref(false)
const form = reactive({ userId: 'U0001' })

async function login() {
  loading.value = true
  try {
    await store.dispatch('login', form)
    const redirect = route.query.redirect ? String(route.query.redirect) : '/meetings'
    await router.replace(redirect)
  } catch (error: any) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #eef4ff 0%, #f7fbff 52%, #f5f7fa 100%);
}

.login-panel {
  width: min(420px, 100%);
  padding: 30px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 18px 50px rgba(31, 41, 55, 0.12);
}

h1 {
  margin: 0 0 8px;
  font-size: 24px;
}

.el-button {
  width: 100%;
}

.demo {
  margin: 18px 0 0;
  color: #6b7280;
  line-height: 1.6;
}
</style>
