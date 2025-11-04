<template>
  <div>
    <el-button type="primary" @click="openPayloadDialog()">新建报文</el-button>
    <el-table :data="payloads" style="width: 100%">
      <el-table-column prop="payloadKey" label="报文键"></el-table-column>
      <el-table-column prop="groupKey" label="分组键"></el-table-column>
      <el-table-column prop="payloadType" label="报文类型"></el-table-column>
      <el-table-column prop="content" label="内容" show-overflow-tooltip></el-table-column>
      <el-table-column prop="description" label="描述"></el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="small" @click="openPayloadDialog(scope.row)">编辑</el-button>
          <el-popconfirm
              title="确定要删除这个报文吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="deletePayloadHandler(scope.row.payloadKey)"
          >
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 报文编辑/新建对话框 -->
    <el-dialog :title="isEditingPayload ? '编辑报文' : '新建报文'" v-model="payloadDialogVisible" width="50%">
      <el-form :model="currentPayload" label-width="100px">
        <el-form-item label="报文键">
          <el-tooltip content="报文的唯一标识符" placement="top">
            <el-input v-model="currentPayload.payloadKey" :disabled="isEditingPayload"></el-input>
          </el-tooltip>
        </el-form-item>
        <el-form-item label="分组键">
          <el-tooltip content="选择一个已有分组或输入新分组名后按回车创建" placement="top">
            <el-select v-model="currentPayload.groupKey" placeholder="请选择或输入分组键" filterable allow-create default-first-option>
              <el-option v-for="key in availablePayloadGroupKeys" :key="key" :label="key" :value="key"></el-option>
            </el-select>
          </el-tooltip>
        </el-form-item>
        <el-form-item label="报文类型">
          <el-tooltip content="报文内容的类型" placement="top">
            <el-select v-model="currentPayload.payloadType" placeholder="请选择报文类型">
              <el-option label="JSON" value="JSON"></el-option>
              <el-option label="HEX" value="HEX"></el-option>
              <el-option label="STRING" value="STRING"></el-option>
            </el-select>
          </el-tooltip>
        </el-form-item>
        <el-form-item label="内容">
          <el-tooltip content="报文的实际内容" placement="top">
            <el-input type="textarea" :rows="8" v-model="currentPayload.content"></el-input>
          </el-tooltip>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="currentPayload.description"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="payloadDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="savePayload">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getPayloads, createPayload, updatePayload, deletePayload, getAvailablePayloadGroupKeys } from '../api/payloads.js';

const router = useRouter();

const payloads = ref([]);
const payloadDialogVisible = ref(false);
const isEditingPayload = ref(false);
const currentPayload = ref({
  id: null,
  payloadKey: '',
  groupKey: '',
  payloadType: 'JSON',
  content: '',
  description: ''
});
const availablePayloadGroupKeys = ref([]);

async function fetchPayloads() {
  try {
    payloads.value = await getPayloads();
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') router.push('/login');
    else ElMessage.error('获取报文失败!');
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

onMounted(fetchPayloads);

function openPayloadDialog(payload) {
  isEditingPayload.value = !!payload;
  fetchAvailablePayloadGroupKeys();
  if (payload) {
    currentPayload.value = JSON.parse(JSON.stringify(payload));
  } else {
    currentPayload.value = { id: null, payloadKey: '', groupKey: '', payloadType: 'JSON', content: '', description: '' };
  }
  payloadDialogVisible.value = true;
}

async function savePayload() {
  try {
    await (isEditingPayload.value
        ? updatePayload(currentPayload.value.payloadKey, currentPayload.value)
        : createPayload(currentPayload.value));

    ElMessage.success(`报文 ${currentPayload.value.payloadKey} ${isEditingPayload.value ? '更新' : '创建'}成功!`);
    payloadDialogVisible.value = false;
    fetchPayloads();
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') router.push('/login');
    else ElMessage.error(`保存报文失败: ${error.message}`);
  }
}

async function deletePayloadHandler(payloadKey) {
  try {
    await deletePayload(payloadKey);
    ElMessage.success(`报文 ${payloadKey} 删除成功!`);
    fetchPayloads();
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') router.push('/login');
    else ElMessage.error(`删除报文 ${payloadKey} 失败: ${error.message}`);
  }
}
</script>

<style scoped>
/* Add any component-specific styles here */
</style>
