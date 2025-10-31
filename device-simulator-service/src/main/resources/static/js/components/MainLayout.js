
import { changePassword } from '../api/auth.js';

export default {
    data() {

        const validatePass = (rule, value, callback) => {

            if (value === '') {

                callback(new Error('请输入新密码'));

            } else if (value.length < 6) {

                callback(new Error('新密码长度不能少于6位'));

            } else {

                if (this.changePasswordForm.confirmPassword !== '') {

                    this.$refs.changePasswordFormRef.validateField('confirmPassword');

                }

                callback();

            }

        };

        const validatePass2 = (rule, value, callback) => {

            if (value === '') {

                callback(new Error('请再次输入新密码'));

            } else if (value !== this.changePasswordForm.newPassword) {

                callback(new Error('两次输入密码不一致!'));

            } else {

                callback();

            }

        };

        return {

            username: '',

            activeMenu: 'instances',

            changePasswordDialogVisible: false,

            changePasswordForm: {

                oldPassword: '',

                newPassword: '',

                confirmPassword: '',

            },

            changePasswordRules: {

                oldPassword: [

                    { required: true, message: '请输入当前密码', trigger: 'blur' }

                ],

                newPassword: [

                    { required: true, message: '请输入新密码', trigger: 'blur' },

                    { validator: validatePass, trigger: 'blur' }

                ],

                confirmPassword: [

                    { required: true, message: '请确认新密码', trigger: 'blur' },

                    { validator: validatePass2, trigger: 'blur' }

                ]

            },

        };

    },

    computed: {

        headerTitle() {

            switch (this.activeMenu) {

                case 'instances': return '实例监控';

                case 'profiles': return '画像管理';

                case 'payloads': return '报文管理';

                case 'rules': return '规则管理';

                default: return '设备模拟器';

            }

        }

    },

    created() {

        const token = localStorage.getItem('jwt');

        if (!token) {

            this.$router.push('/login');

            return;

        }

        this.username = this.parseJwt(token).sub;

        this.activeMenu = this.$route.path.split('/')[2] || 'instances';

        this.$router.push(`/main/${this.activeMenu}`);

    },

    methods: {

        parseJwt(token) {

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

        },

        handleMenuSelect(index) {

            this.activeMenu = index;

            this.$router.push(`/main/${index}`);

        },

        handleUserCommand(command) {

            if (command === 'changePassword') {

                this.openChangePasswordDialog();

            } else if (command === 'logout') {

                this.handleLogout();

            }

        },

        handleLogout() {

            localStorage.removeItem('jwt');

            this.$router.push('/login');

            this.$message.success('您已成功登出。');

        },

        openChangePasswordDialog() {

            this.changePasswordDialogVisible = true;

            this.$nextTick(() => {

                if (this.$refs.changePasswordFormRef) {

                    this.$refs.changePasswordFormRef.resetFields();

                }

            });

        },

        async handleChangePassword() {

            this.$refs.changePasswordFormRef.validate(async (valid) => {

                if (valid) {

                    try {

                        const response = await changePassword(this.changePasswordForm);

                        if (response.ok) {

                            const message = await response.text();

                            this.$message.success(message || '密码修改成功！');

                            this.changePasswordDialogVisible = false;

                            this.handleLogout();

                        } else {

                            const errorText = await response.text();

                            this.$message.error(`修改密码失败: ${errorText}`);

                        }

                    } catch (error) {

                        if (error.message === 'AUTH_FAILURE') {

                            this.$message.error('认证失败，请重新登录。');

                            this.handleLogout();

                        } else {

                            console.error('修改密码请求失败:', error);

                            this.$message.error(`修改密码请求失败: ${error.message}`);

                        }

                    }

                }

            });

        },

    },

    template: `

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

    `

};
