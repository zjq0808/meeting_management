<template>
  <main class="login-page">
    <section class="login-card">
      <div class="login-brand">
        <span>会</span>
        <div>
          <h1>三重一大会议管理</h1>
          <p>企业微信用户本地调试登录</p>
        </div>
      </div>
      <el-form :model="form" label-position="top" @submit.prevent>
        <el-form-item label="企业微信用户 ID">
          <el-input v-model="form.userId" autocomplete="username" size="large" placeholder="例如 U0001" @keyup.enter="login" />
        </el-form-item>
        <el-button type="primary" size="large" :loading="loading" @click="login">登录系统</el-button>
      </el-form>
      <div class="login-demo">
        <strong>演示账号</strong>
        <span>U0001 管理员</span>
        <span>U0002 秘书</span>
        <span>U0004 科组长</span>
        <span>U0006 参会人员</span>
      </div>
    </section>
  </main>
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
    await store.dispatch('login', { userId: form.userId })
    await router.replace(route.query.redirect ? String(route.query.redirect) : '/meetings')
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
  background:
    radial-gradient(circle at 18% 18%, rgba(26, 115, 232, 0.16), transparent 32%),
    linear-gradient(135deg, #eef5ff 0%, #f8fbff 48%, #f4f7fb 100%);
}

.login-card {
  width: min(440px, 100%);
  padding: 34px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid #e6eaf0;
  border-radius: 18px;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.14);
}

.login-brand {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 28px;
}

.login-brand > span {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  color: #fff;
  font-size: 22px;
  font-weight: 900;
  background: #1a73e8;
  border-radius: 14px;
  box-shadow: 0 12px 26px rgba(26, 115, 232, 0.26);
}

h1 {
  margin: 0;
  color: #111827;
  font-size: 24px;
}

p {
  margin: 6px 0 0;
  color: #667085;
}

.el-button {
  width: 100%;
}

.login-demo {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 20px;
  color: #667085;
  font-size: 13px;
}

.login-demo strong {
  width: 100%;
  color: #344054;
}
</style>
