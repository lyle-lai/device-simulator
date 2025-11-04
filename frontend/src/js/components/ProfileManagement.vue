<template>
  <div>
    <el-button type="primary" @click="openProfileDialog()">新建画像</el-button>
    <el-table :data="profiles" style="width: 100%">
      <el-table-column prop="profileName" label="画像名称"></el-table-column>
      <el-table-column prop="enabled" label="是否启用">
        <template #default="scope">
          <el-switch v-model="scope.row.enabled" disabled></el-switch>
        </template>
      </el-table-column>
      <el-table-column prop="device.id" label="设备ID"></el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="small" @click="openProfileDialog(scope.row)">编辑</el-button>
          <el-popconfirm
              title="确定要删除这个画像吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="deleteProfileHandler(scope.row.profileName)"
          >
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- Profile编辑/新建对话框 -->
    <el-dialog :title="isEditing ? '编辑画像' : '新建画像'" v-model="profileDialogVisible" width="60%">
      <el-form :model="currentProfile" :rules="profileRules" ref="profileFormRef" label-width="120px">
        <el-form-item label="画像名称" prop="profileName">
          <el-input v-model="currentProfile.profileName" :disabled="isEditing"></el-input>
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="currentProfile.enabled"></el-switch>
        </el-form-item>
        <el-form-item label="设备ID" prop="device.id">
          <el-input v-model="currentProfile.device.id"></el-input>
        </el-form-item>

        <!-- 协议配置 -->
        <el-card class="box-card">
          <template #header><div class="card-header"><span>协议配置</span></div></template>
          <el-form-item label="协议类型" prop="protocol.type">
            <el-select v-model="currentProfile.protocol.type" placeholder="请选择协议类型">
              <el-option label="TCP客户端" value="tcp-client"></el-option>
              <el-option label="TCP服务端" value="tcp-server"></el-option>
              <el-option label="直通协议" value="passthrough"></el-option>
            </el-select>
          </el-form-item>
          <template v-if="currentProfile.protocol.type === 'tcp-client'">
            <el-form-item label="主机" prop="protocol.properties.host">
              <el-input v-model="currentProfile.protocol.properties.host"></el-input>
            </el-form-item>
            <el-form-item label="端口" prop="protocol.properties.port">
              <el-input-number v-model="currentProfile.protocol.properties.port" :min="1" :max="65535"></el-input-number>
            </el-form-item>
            <el-form-item label="本地IP">
              <el-tooltip content="可选，指定客户端连接时绑定的本地IP地址" placement="top">
                <el-input v-model="currentProfile.protocol.properties.localAddress" placeholder="留空则使用默认IP"></el-input>
              </el-tooltip>
            </el-form-item>
            <el-form-item label="重连间隔(ms)">
              <el-tooltip content="客户端断线后，尝试重新连接的延迟时间" placement="top">
                <el-input-number v-model="currentProfile.protocol.properties.reconnectDelay" :min="1000" placeholder="默认5000"></el-input-number>
              </el-tooltip>
            </el-form-item>
          </template>
          <template v-if="currentProfile.protocol.type === 'tcp-server'">
            <el-form-item label="监听端口" prop="protocol.properties.port">
              <el-input-number v-model="currentProfile.protocol.properties.port" :min="1" :max="65535"></el-input-number>
            </el-form-item>
          </template>
        </el-card>

        <!-- 编解码器配置 -->
        <el-card class="box-card">
          <template #header><div class="card-header"><span>编解码器配置</span></div></template>
          <el-form-item label="编解码类型" prop="codec.type">
            <el-select v-model="currentProfile.codec.type" placeholder="请选择编解码类型">
              <el-option label="JSON" value="json"></el-option>
              <el-option label="HEX" value="hex"></el-option>
              <el-option label="直通编解码" value="passthrough"></el-option>
            </el-select>
          </el-form-item>
        </el-card>

        <!-- 策略配置 -->
        <el-card class="box-card">
          <template #header><div class="card-header"><span>策略配置</span></div></template>
          <el-form-item label="策略类型" prop="strategy.type">
            <el-select v-model="currentProfile.strategy.type" placeholder="请选择策略类型">
              <el-option label="周期性推送" value="periodic-push"></el-option>
              <el-option label="请求-响应" value="request-response"></el-option>
              <el-option label="原始数据回放 (已废弃)" value="raw-replay"></el-option>
            </el-select>
          </el-form-item>

          <!-- 周期性推送策略配置 -->
          <template v-if="currentProfile.strategy.type === 'periodic-push'">
            <el-form-item label="推送间隔(ms)" prop="strategy.properties.intervalMillis">
              <el-input-number v-model="currentProfile.strategy.properties.intervalMillis" :min="100"></el-input-number>
            </el-form-item>
            <el-form-item label="数据生成器" prop="dataGenerator.type">
              <el-select v-model="currentProfile.dataGenerator.type" placeholder="请选择数据生成器类型">
                <el-option label="随机生成器" value="random-generator"></el-option>
                <el-option label="数据库生成器" value="database-generator"></el-option>
                <el-option label="直通生成器" value="passthrough-generator"></el-option>
              </el-select>
            </el-form-item>
            <template v-if="currentProfile.dataGenerator.type === 'random-generator'">
              <el-form-item label="随机属性(JSON)">
                <el-input type="textarea" :rows="5" v-model="currentProfile.dataGenerator.propertiesJson" placeholder="请输入随机属性的JSON数组"></el-input>
              </el-form-item>
            </template>
            <template v-if="currentProfile.dataGenerator.type === 'database-generator'">
              <el-form-item label="报文分组键" prop="dataGenerator.properties.groupKey">
                <el-tooltip content="选择一个已存在的报文分组键作为数据源" placement="top">
                  <el-select v-model="currentProfile.dataGenerator.properties.groupKey" placeholder="请选择报文分组键">
                    <el-option v-for="key in availablePayloadGroupKeys" :key="key" :label="key" :value="key"></el-option>
                  </el-select>
                </el-tooltip>
              </el-form-item>
            </template>
          </template>

          <!-- 请求-响应策略配置 -->
          <template v-if="currentProfile.strategy.type === 'request-response'">
            <el-form-item label="数据生成器" prop="dataGenerator.type">
              <el-select v-model="currentProfile.dataGenerator.type" placeholder="请选择数据生成器类型">
                <el-option label="数据库规则生成器" value="db-rule-based"></el-option>
                <el-option label="直通生成器" value="passthrough"></el-option>
              </el-select>
            </el-form-item>
            <template v-if="currentProfile.dataGenerator.type === 'db-rule-based'">
              <el-form-item label="规则分组键" prop="dataGenerator.properties.ruleGroup">
                <el-tooltip content="选择一个已存在的规则分组作为请求-响应规则集" placement="top">
                  <el-select v-model="currentProfile.dataGenerator.properties.ruleGroup" placeholder="请选择规则分组">
                    <el-option v-for="group in availableRuleGroups" :key="group" :label="group" :value="group"></el-option>
                  </el-select>
                </el-tooltip>
              </el-form-item>
            </template>
          </template>

          <!-- 原始数据回放策略配置 (已废弃) -->
          <template v-if="currentProfile.strategy.type === 'raw-replay'">
            <el-alert title="此策略已废弃" type="warning" show-icon :closable="false"></el-alert>
            <el-form-item label="回放文件路径"><el-input v-model="currentProfile.strategy.properties.filePath"></el-input></el-form-item>
            <el-form-item label="回放速度(倍)"><el-input-number v-model="currentProfile.strategy.properties.speedFactor" :min="0.1"></el-input-number></el-form-item>
          </template>
        </el-card>

        <!-- 原始JSON配置 -->
        <el-card class="box-card">
          <template #header><div class="card-header"><span>原始JSON配置 (高级)</span></div></template>
          <el-form-item label="原始JSON">
            <el-input type="textarea" :rows="10" v-model="currentProfileJson" placeholder="可在此处直接粘贴或编辑完整的JSON配置"></el-input>
          </el-form-item>
        </el-card>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="profileDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveProfile">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getProfiles, createProfile, updateProfile, deleteProfile } from '../api/profiles.js';
import { getAvailablePayloadGroupKeys } from '../api/payloads.js';
import { getAvailableRuleGroups } from '../api/rules.js';

const router = useRouter();
const profileFormRef = ref(null);

const profiles = ref([]);
const profileDialogVisible = ref(false);
const isEditing = ref(false);

const defaultProfile = () => ({
  profileName: '',
  enabled: true,
  device: { id: '' },
  protocol: { type: 'passthrough', properties: { host: '', port: null, localAddress: '', reconnectDelay: 5000 } },
  codec: { type: 'passthrough' },
  strategy: { type: 'passthrough', properties: { intervalMillis: null, filePath: '', speedFactor: null } },
  dataGenerator: { type: 'passthrough', properties: { payloadName: '', ruleGroup: '' }, propertiesJson: '' }
});

const currentProfile = ref(defaultProfile());
const currentProfileJson = ref('');

const profileRules = ref({
  profileName: [{ required: true, message: '请输入画像名称', trigger: 'blur' }],
  'device.id': [{ required: true, message: '请输入设备ID', trigger: 'blur' }],
  'protocol.properties.host': [{ required: true, message: '请输入主机地址', trigger: 'blur' }],
  'protocol.properties.port': [{ required: true, message: '请输入端口号', trigger: ['blur', 'change'] }],
  'strategy.properties.intervalMillis': [{ required: true, message: '请输入推送间隔', trigger: ['blur', 'change'] }],
  'dataGenerator.properties.groupKey': [{ required: true, message: '请选择报文分组键', trigger: 'change' }],
  'dataGenerator.properties.ruleGroup': [{ required: true, message: '请选择规则分组', trigger: 'change' }]
});

const availablePayloadGroupKeys = ref([]);
const availableRuleGroups = ref([]);

watch(currentProfile, (newValue) => {
  if (!profileDialogVisible.value) return;
  const tempProfile = JSON.parse(JSON.stringify(newValue));
  if (tempProfile.dataGenerator) delete tempProfile.dataGenerator.propertiesJson;
  if (tempProfile.protocol && !tempProfile.protocol.properties) tempProfile.protocol.properties = {};
  if (tempProfile.strategy && !tempProfile.strategy.properties) tempProfile.strategy.properties = {};
  if (tempProfile.dataGenerator && !tempProfile.dataGenerator.properties) tempProfile.dataGenerator.properties = {};
  currentProfileJson.value = JSON.stringify(tempProfile, null, 2);
}, { deep: true });

watch(currentProfileJson, (newValue) => {
  if (profileDialogVisible.value && newValue.trim() !== '') {
    try {
      JSON.parse(newValue);
    } catch (e) { /* no-op */ }
  }
});

async function fetchProfiles() {
  try {
    profiles.value = await getProfiles();
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') router.push('/login');
    else ElMessage.error('获取画像列表失败!');
  }
}

async function fetchAvailablePayloadGroupKeys() {
  try {
    availablePayloadGroupKeys.value = await getAvailablePayloadGroupKeys();
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') router.push('/login');
    else ElMessage.error('获取可用报文分组键失败!');
  }
}

async function fetchAvailableRuleGroups() {
  try {
    availableRuleGroups.value = await getAvailableRuleGroups();
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') router.push('/login');
    else ElMessage.error('获取可用规则分组失败!');
  }
}

onMounted(fetchProfiles);

function openProfileDialog(profile) {
  isEditing.value = !!profile;
  fetchAvailablePayloadGroupKeys();
  fetchAvailableRuleGroups();

  if (profile) {
    const deepCopiedProfile = JSON.parse(JSON.stringify(profile));
    currentProfile.value = {
      ...defaultProfile(),
      ...deepCopiedProfile,
      device: deepCopiedProfile.device || { id: '' },
      protocol: deepCopiedProfile.protocol || { type: 'passthrough', properties: {} },
      codec: deepCopiedProfile.codec || { type: 'passthrough' },
      strategy: deepCopiedProfile.strategy || { type: 'passthrough', properties: {} },
      dataGenerator: deepCopiedProfile.dataGenerator || { type: 'passthrough', properties: {} },
    };
    if (currentProfile.value.dataGenerator.type === 'random' && currentProfile.value.dataGenerator.properties) {
      currentProfile.value.dataGenerator.propertiesJson = JSON.stringify(currentProfile.value.dataGenerator.properties, null, 2);
    } else {
      currentProfile.value.dataGenerator.propertiesJson = '';
    }
  } else {
    currentProfile.value = defaultProfile();
    currentProfileJson.value = '';
  }
  profileDialogVisible.value = true;
  nextTick(() => profileFormRef.value?.clearValidate());
}

async function saveProfile() {
  if (!profileFormRef.value) return;
  await profileFormRef.value.validate(async (valid) => {
    if (valid) {
      try {
        let profileDataToSend = currentProfileJson.value.trim() !== ''
            ? JSON.parse(currentProfileJson.value)
            : JSON.parse(JSON.stringify(currentProfile.value));

        if (profileDataToSend.dataGenerator) delete profileDataToSend.dataGenerator.propertiesJson;

        await (isEditing.value
            ? updateProfile(profileDataToSend.profileName, profileDataToSend)
            : createProfile(profileDataToSend));

        ElMessage.success(`画像 ${profileDataToSend.profileName} ${isEditing.value ? '更新' : '创建'}成功!`);
        profileDialogVisible.value = false;
        fetchProfiles();
      } catch (error) {
        if (error.message === 'AUTH_FAILURE') router.push('/login');
        else ElMessage.error(`保存画像失败: ${error.message}`);
      }
    } else {
      ElMessage.error('请检查输入项是否都已正确填写！');
      return false;
    }
  });
}

async function deleteProfileHandler(profileName) {
  try {
    await deleteProfile(profileName);
    ElMessage.success(`画像 ${profileName} 删除成功!`);
    fetchProfiles();
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') router.push('/login');
    else ElMessage.error(`删除失败: ${error.message}`);
  }
}
</script>

<style scoped>
.box-card {
  margin-bottom: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
