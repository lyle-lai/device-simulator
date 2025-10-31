
import { login } from '../api/auth.js';

export default {
    data() {
        return {
            loginForm: {
                username: 'admin',
                password: 'password'
            },
            loginError: '',
        };
    },
    methods: {
        async handleLogin() {
            this.loginError = '';
            try {
                const data = await login(this.loginForm.username, this.loginForm.password);
                localStorage.setItem('jwt', data.jwt);
                this.$router.push('/main/instances');
            } catch (error) {
                console.error('登录请求失败:', error);
                this.loginError = `登录失败: ${error.message}`;
                this.$message.error(this.loginError);
            }
        },
    },
    template: `
        <div class="login-container">
            <el-card class="login-card">
                <div class="login-title-container">
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
    `
};
