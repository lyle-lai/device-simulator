
import { getProfileStatuses, startInstance, stopInstance } from '../api/instances.js';
import { webSocketService } from '../services/websocket.js';

export default {
    data() {
        return {
            profileStatuses: [],
            logDialogVisible: false,
            currentLogProfileName: '',
            logs: [],
            logSubscription: null,
        };
    },
    created() {
        this.fetchProfileStatuses();
    },
    methods: {
        async fetchProfileStatuses() {
            try {
                this.profileStatuses = await getProfileStatuses();
            } catch (error) {
                if (error.message === 'AUTH_FAILURE') {
                    this.$router.push('/login');
                } else {
                    console.error('获取画像状态失败:', error);
                    this.$message.error('获取画像状态失败!');
                }
            }
        },
        async startInstance(profileName) {
            try {
                const updatedStatus = await startInstance(profileName);
                const index = this.profileStatuses.findIndex(s => s.profile.profileName === profileName);
                if (index !== -1) {
                    this.profileStatuses.splice(index, 1, updatedStatus);
                }
                this.$message.success(`实例 ${profileName} 启动成功!`);
            } catch (error) {
                if (error.message === 'AUTH_FAILURE') {
                    this.$router.push('/login');
                } else {
                    console.error(`启动实例 ${profileName} 失败:`, error);
                    this.$message.error(`启动实例 ${profileName} 失败: ${error.message}`);
                }
            }
        },
        async stopInstance(profileName) {
            try {
                const updatedStatus = await stopInstance(profileName);
                const index = this.profileStatuses.findIndex(s => s.profile.profileName === profileName);
                if (index !== -1) {
                    this.profileStatuses.splice(index, 1, updatedStatus);
                }
                this.$message.success(`实例 ${profileName} 停止成功!`);
            } catch (error) {
                if (error.message === 'AUTH_FAILURE') {
                    this.$router.push('/login');
                } else {
                    console.error(`停止实例 ${profileName} 失败:`, error);
                    this.$message.error(`停止实例 ${profileName} 失败: ${error.message}`);
                }
            }
        },
        openLogDialog(profileName) {
            this.currentLogProfileName = profileName;
            this.logs = [];
            this.logDialogVisible = true;
            this.connectWebSocket(profileName);
        },
        closeLogDialog() {
            this.disconnectWebSocket();
            this.logs = [];
            this.currentLogProfileName = '';
        },
        connectWebSocket(profileName) {
            this.disconnectWebSocket();
            webSocketService.connect(() => {
                this.logSubscription = webSocketService.subscribe(`/topic/logs/${profileName}`, log => {
                    this.logs.push(log);
                    this.$nextTick(() => {
                        const logContainer = this.$refs.logContainer;
                        if (logContainer) {
                            logContainer.scrollTop = logContainer.scrollHeight;
                        }
                    });
                });
            }, (error) => {
                this.$message.error('WebSocket连接失败，无法获取实时日志!');
            });
        },
        disconnectWebSocket() {
            if (this.logSubscription) {
                webSocketService.unsubscribe(`/topic/logs/${this.currentLogProfileName}`);
                this.logSubscription = null;
            }
            webSocketService.disconnect();
        }
    },
    beforeUnmount() {
        this.disconnectWebSocket();
    },
    template: `
        <div>
            <el-table :data="profileStatuses" style="width: 100%">
                <el-table-column prop="profile.profileName" label="画像名称"></el-table-column>
                <el-table-column prop="status" label="运行状态">
                    <template #default="scope">
                        <el-tag :type="scope.row.status === 'RUNNING' ? 'success' : (scope.row.status === 'STOPPED' ? 'info' : 'danger')">{{ scope.row.status }}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column label="启用状态">
                    <template #default="scope">
                        <el-switch v-model="scope.row.profile.enabled" disabled></el-switch>
                    </template>
                </el-table-column>
                <el-table-column label="操作">
                    <template #default="scope">
                        <el-button size="small" @click="startInstance(scope.row.profile.profileName)" :disabled="scope.row.status === 'RUNNING' || !scope.row.profile.enabled">启动</el-button>
                        <el-button size="small" type="danger" @click="stopInstance(scope.row.profile.profileName)" :disabled="scope.row.status !== 'RUNNING'">停止</el-button>
                        <el-button size="small" @click="openLogDialog(scope.row.profile.profileName)" :disabled="scope.row.status !== 'RUNNING'">查看日志</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <!-- Log Dialog -->
            <el-dialog :title="'实例日志: ' + currentLogProfileName" v-model="logDialogVisible" width="60%" @close="closeLogDialog">
                <div class="log-container" ref="logContainer">
                    <div v-for="(log, index) in logs" :key="index" :class="['log-entry', 'log-' + log.level.toLowerCase()]">
                        <span class="log-timestamp">{{ log.timestamp }}</span>
                        <span class="log-level">{{ log.level }}</span>
                        <span class="log-message">{{ log.message }}</span>
                    </div>
                </div>
                <template #footer>
                    <span class="dialog-footer">
                        <el-button @click="logDialogVisible = false">关闭</el-button>
                    </span>
                </template>
            </el-dialog>
        </div>
    `
};
