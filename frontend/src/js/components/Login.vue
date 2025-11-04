<template>
  <div class="login-container">
    <el-card class="login-card">
      <div class="login-title-container">
        <!-- 注意：在SFC模板中，全局注册的组件可以直接使用 -->
        <el-icon class="login-title-icon"><Platform /></el-icon>
        <div class="login-title-text">设备模拟器</div>
      </div>
      <el-form :model="loginForm" @keyup.enter="handleLogin" label-position="top">
        <el-alert v-if="loginError" :title="loginError" type="error" show-icon class="login-error-alert" :closable="false"></el-alert>
        <el-form-item label="用户名">
          <el-input v-model="loginForm.username" placeholder="请输入用户名" :prefix-icon="User" size="large"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password size="large"></el-input>
        </el-form-item>
        <el-form-item style="margin-top: 30px;">
          <el-button type="primary" @click="handleLogin" style="width: 100%;" size="large">登 录</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { User, Lock, Platform } from '@element-plus/icons-vue'; // 显式导入图标更可靠
import { login } from '../api/auth.js';

// 使用组合式API
const router = useRouter();

const loginForm = ref({
  username: 'admin',
  password: ''
});
const loginError = ref('');

const handleLogin = async () => {
  loginError.value = '';
  try {
    const data = await login(loginForm.value.username, loginForm.value.password);
    localStorage.setItem('jwt', data.jwt);
    router.push('/main/instances');
  } catch (error) {
    console.error('登录请求失败:', error);
    loginError.value = `登录失败: ${error.message}`;
    ElMessage.error(loginError.value);
  }
};
</script>

<style>
/* 之前的全局样式依然生效，如果需要特定样式可在此处添加 */
.login-container {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100vh;
    background-color: #f0f2f5;
}
.login-card {
    width: 400px;
}
.login-title-container {
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 20px;
}
.login-title-icon {
    font-size: 24px;
    margin-right: 10px;
}
.login-title-text {
    font-size: 22px;
    font-weight: bold;
}
.login-error-alert {
    margin-bottom: 20px;
}
</style>
