<template>
  <el-container style="height: 100vh;">
    <el-aside width="200px" style="background-color: #545c64;">
      <el-menu
          :default-active="activeMenu"
          class="el-menu-vertical-demo"
          @select="handleMenuSelect"
          background-color="#545c64"
          text-color="#fff"
          active-text-color="#ffd04b">
        <div class="logo">
          <h4>设备模拟器</h4>
        </div>
        <el-menu-item index="instances">
          <el-icon><Monitor /></el-icon>
          <span>实例监控</span>
        </el-menu-item>
        <el-menu-item index="profiles">
          <el-icon><Document /></el-icon>
          <span>画像管理</span>
        </el-menu-item>
        <el-menu-item index="payloads">
          <el-icon><MessageBox /></el-icon>
          <span>报文管理</span>
        </el-menu-item>
        <el-menu-item index="rules">
          <el-icon><Setting /></el-icon>
          <span>规则管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="main-header">
        <div class="header-title">{{ headerTitle }}</div>
        <div class="header-user-info">
          <el-dropdown @command="handleUserCommand">
            <span class="el-dropdown-link">
              <el-icon class="el-icon--left"><UserFilled /></el-icon>
              欢迎您, {{ username }}
              <el-icon class="el-icon--right"><arrow-down /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
                <el-dropdown-item command="logout" divided>登出</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main>
        <router-view></router-view>
      </el-main>
    </el-container>
  </el-container>

  <!-- 修改密码对话框 -->
  <el-dialog title="修改密码" v-model="changePasswordDialogVisible" width="30%">
    <el-form :model="changePasswordForm" :rules="changePasswordRules" ref="changePasswordFormRef" label-width="100px">
      <el-form-item label="当前密码" prop="oldPassword">
        <el-input type="password" v-model="changePasswordForm.oldPassword" show-password></el-input>
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input type="password" v-model="changePasswordForm.newPassword" show-password></el-input>
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirmPassword">
        <el-input type="password" v-model="changePasswordForm.confirmPassword" show-password></el-input>
      </el-form-item>
    </el-form>
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="changePasswordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleChangePassword">确认修改</el-button>
      </span>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Monitor, Document, MessageBox, Setting, UserFilled, ArrowDown } from '@element-plus/icons-vue';
import { changePassword } from '../api/auth.js';

const router = useRouter();
const route = useRoute();

const username = ref('');
const activeMenu = ref('instances');
const changePasswordDialogVisible = ref(false);
const changePasswordFormRef = ref(null); // 用于引用表单

const changePasswordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
});

const validatePass = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入新密码'));
  } else if (value.length < 6) {
    callback(new Error('新密码长度不能少于6位'));
  } else {
    if (changePasswordForm.value.confirmPassword !== '') {
      changePasswordFormRef.value.validateField('confirmPassword');
    }
    callback();
  }
};

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'));
  } else if (value !== changePasswordForm.value.newPassword) {
    callback(new Error('两次输入密码不一致!'));
  } else {
    callback();
  }
};

const changePasswordRules = ref({
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }, { validator: validatePass, trigger: 'blur' }],
  confirmPassword: [{ required: true, message: '请确认新密码', trigger: 'blur' }, { validator: validatePass2, trigger: 'blur' }],
});

const headerTitle = computed(() => {
  switch (activeMenu.value) {
    case 'instances': return '实例监控';
    case 'profiles': return '画像管理';
    case 'payloads': return '报文管理';
    case 'rules': return '规则管理';
    default: return '设备模拟器';
  }
});

onMounted(() => {
  const token = localStorage.getItem('jwt');
  if (!token) {
    router.push('/login');
    return;
  }
  username.value = parseJwt(token).sub;
  activeMenu.value = route.path.split('/')[2] || 'instances';
  router.push(`/main/${activeMenu.value}`);
});

function parseJwt(token) {
  try {
    const base64Url = token.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
      return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
    }).join(''));
    return JSON.parse(jsonPayload);
  } catch (e) {
    console.error('解析JWT失败:', e);
    return null;
  }
}

function handleMenuSelect(index) {
  activeMenu.value = index;
  router.push(`/main/${index}`);
}

function handleUserCommand(command) {
  if (command === 'changePassword') {
    openChangePasswordDialog();
  } else if (command === 'logout') {
    handleLogout();
  }
}

function handleLogout() {
  localStorage.removeItem('jwt');
  router.push('/login');
  ElMessage.success('您已成功登出。');
}

function openChangePasswordDialog() {
  changePasswordDialogVisible.value = true;
  nextTick(() => {
    if (changePasswordFormRef.value) {
      changePasswordFormRef.value.resetFields();
    }
  });
}

async function handleChangePassword() {
  if (!changePasswordFormRef.value) return;
  await changePasswordFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        const response = await changePassword(changePasswordForm.value);
        if (response.ok) {
          const message = await response.text();
          ElMessage.success(message || '密码修改成功！');
          changePasswordDialogVisible.value = false;
          handleLogout(); // 修改成功后登出
        } else {
          const errorText = await response.text();
          ElMessage.error(`修改密码失败: ${errorText}`);
        }
      } catch (error) {
        if (error.message === 'AUTH_FAILURE') {
          ElMessage.error('认证失败，请重新登录。');
          handleLogout();
        } else {
          console.error('修改密码请求失败:', error);
          ElMessage.error(`修改密码请求失败: ${error.message}`);
        }
      }
    }
  });
}
</script>

<style scoped>
.logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
}
.logo h4 {
    margin: 0;
    font-size: 18px;
}
.main-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e6e6e6;
}
.header-title {
    font-size: 18px;
}
.header-user-info .el-dropdown-link {
    cursor: pointer;
    display: flex;
    align-items: center;
}
</style>
