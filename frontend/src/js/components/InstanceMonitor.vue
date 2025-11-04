<template>
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
          <el-button size="small" @click="startInstanceHandler(scope.row.profile.profileName)" :disabled="scope.row.status === 'RUNNING' || !scope.row.profile.enabled">启动</el-button>
          <el-button size="small" type="danger" @click="stopInstanceHandler(scope.row.profile.profileName)" :disabled="scope.row.status !== 'RUNNING'">停止</el-button>
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
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getProfileStatuses, startInstance, stopInstance } from '../api/instances.js';
import { webSocketService } from '../services/websocket.js';

const router = useRouter();

const profileStatuses = ref([]);
const logDialogVisible = ref(false);
const currentLogProfileName = ref('');
const logs = ref([]);
let logSubscription = null;
const logContainer = ref(null); // Ref for the log container div

async function fetchProfileStatuses() {
  try {
    profileStatuses.value = await getProfileStatuses();
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') {
      router.push('/login');
    } else {
      console.error('获取画像状态失败:', error);
      ElMessage.error('获取画像状态失败!');
    }
  }
}

onMounted(() => {
  fetchProfileStatuses();
});

async function startInstanceHandler(profileName) {
  try {
    const updatedStatus = await startInstance(profileName);
    const index = profileStatuses.value.findIndex(s => s.profile.profileName === profileName);
    if (index !== -1) {
      profileStatuses.value.splice(index, 1, updatedStatus);
    }
    ElMessage.success(`实例 ${profileName} 启动成功!`);
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') {
      router.push('/login');
    } else {
      console.error(`启动实例 ${profileName} 失败:`, error);
      ElMessage.error(`启动实例 ${profileName} 失败: ${error.message}`);
    }
  }
}

async function stopInstanceHandler(profileName) {
  try {
    const updatedStatus = await stopInstance(profileName);
    const index = profileStatuses.value.findIndex(s => s.profile.profileName === profileName);
    if (index !== -1) {
      profileStatuses.value.splice(index, 1, updatedStatus);
    }
    ElMessage.success(`实例 ${profileName} 停止成功!`);
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') {
      router.push('/login');
    } else {
      console.error(`停止实例 ${profileName} 失败:`, error);
      ElMessage.error(`停止实例 ${profileName} 失败: ${error.message}`);
    }
  }
}

function openLogDialog(profileName) {
  currentLogProfileName.value = profileName;
  logs.value = [];
  logDialogVisible.value = true;
  connectWebSocket(profileName);
}

function closeLogDialog() {
  disconnectWebSocket();
  logs.value = [];
  currentLogProfileName.value = '';
}

function connectWebSocket(profileName) {
  disconnectWebSocket();
  webSocketService.connect(() => {
    logSubscription = webSocketService.subscribe(`/topic/logs/${profileName}`, log => {
      logs.value.push(log);
      nextTick(() => {
        if (logContainer.value) {
          logContainer.value.scrollTop = logContainer.value.scrollHeight;
        }
      });
    });
  }, (error) => {
    ElMessage.error('WebSocket连接失败，无法获取实时日志!');
  });
}

function disconnectWebSocket() {
  if (logSubscription && typeof logSubscription.unsubscribe === 'function') {
    logSubscription.unsubscribe();
    logSubscription = null;
    console.log(`已取消订阅: /topic/logs/${currentLogProfileName.value}`);
  }
  // 注意：这里我们不关闭整个WebSocket连接(webSocketService.disconnect()),
  // 因为它可能正在被其他组件或服务使用。连接的生命周期由websocketService自身管理。
}

onBeforeUnmount(() => {
  disconnectWebSocket();
});
</script>

<style scoped>
.log-container {
  height: 400px;
  overflow-y: auto;
  background-color: #282c34;
  color: #abb2bf;
  padding: 10px;
  font-family: 'Courier New', Courier, monospace;
  border-radius: 4px;
}
.log-entry {
  margin-bottom: 5px;
  white-space: pre-wrap;
}
.log-timestamp {
  color: #61afef;
  margin-right: 15px;
}
.log-level {
  font-weight: bold;
  margin-right: 15px;
}
.log-info .log-level { color: #98c379; }
.log-warn .log-level { color: #e5c07b; }
.log-error .log-level { color: #e06c75; }
</style>
