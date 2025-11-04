<template>
  <div>
    <el-button type="primary" @click="openRuleDialog()">新建规则</el-button>
    <el-table :data="rules" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="ruleGroup" label="规则分组"></el-table-column>
      <el-table-column prop="requestKey" label="请求关键字" show-overflow-tooltip></el-table-column>
      <el-table-column prop="responseKey" label="响应报文键" show-overflow-tooltip></el-table-column>
      <el-table-column prop="description" label="描述"></el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="small" @click="openRuleDialog(scope.row)">编辑</el-button>
          <el-popconfirm
              title="确定要删除这个规则吗？"
              confirm-button-text="确定"
              cancel-button-text="取消"
              @confirm="deleteRuleHandler(scope.row.id)"
          >
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 规则编辑/新建对话框 -->
    <el-dialog :title="isEditingRule ? '编辑规则' : '新建规则'" v-model="ruleDialogVisible" width="50%">
      <el-form :model="currentRule" label-width="100px">
        <el-form-item label="规则分组">
          <el-tooltip content="选择一个已有分组或输入新分组名后按回车创建" placement="top">
            <el-select v-model="currentRule.ruleGroup" placeholder="请选择或输入规则分组" filterable allow-create default-first-option>
              <el-option v-for="group in availableRuleGroups" :key="group" :label="group" :value="group"></el-option>
            </el-select>
          </el-tooltip>
        </el-form-item>
        <el-form-item label="请求关键字">
          <el-tooltip content="匹配传入请求的关键字" placement="top">
            <el-input v-model="currentRule.requestKey"></el-input>
          </el-tooltip>
        </el-form-item>
        <el-form-item label="响应报文键">
          <el-tooltip content="选择一个已存在的报文键作为响应内容" placement="top">
            <el-select v-model="currentRule.responseKey" placeholder="请选择响应报文键">
              <el-option v-for="key in availablePayloadKeys" :key="key" :label="key" :value="key"></el-option>
            </el-select>
          </el-tooltip>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="currentRule.description"></el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="ruleDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveRule">保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { getRules, createRule, updateRule, deleteRule, getAvailableRuleGroups } from '../api/rules.js';
import { getAvailablePayloadKeys } from '../api/payloads.js';

const router = useRouter();

const rules = ref([]);
const ruleDialogVisible = ref(false);
const isEditingRule = ref(false);
const currentRule = ref({
  id: null,
  ruleGroup: '',
  requestKey: '',
  responseKey: '',
  description: ''
});
const availableRuleGroups = ref([]);
const availablePayloadKeys = ref([]);

async function fetchRules() {
  try {
    rules.value = await getRules();
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') router.push('/login');
    else ElMessage.error('获取规则失败!');
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

async function fetchAvailablePayloadKeys() {
  try {
    availablePayloadKeys.value = await getAvailablePayloadKeys();
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') router.push('/login');
    else ElMessage.error('获取可用报文键失败!');
  }
}

onMounted(fetchRules);

function openRuleDialog(rule) {
  isEditingRule.value = !!rule;
  fetchAvailablePayloadKeys();
  fetchAvailableRuleGroups();
  if (rule) {
    currentRule.value = JSON.parse(JSON.stringify(rule));
  } else {
    currentRule.value = { id: null, ruleGroup: '', requestKey: '', responseKey: '', description: '' };
  }
  ruleDialogVisible.value = true;
}

async function saveRule() {
  try {
    await (isEditingRule.value
        ? updateRule(currentRule.value.id, currentRule.value)
        : createRule(currentRule.value));

    ElMessage.success(`规则 ${currentRule.value.ruleGroup} ${isEditingRule.value ? '更新' : '创建'}成功!`);
    ruleDialogVisible.value = false;
    fetchRules();
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') router.push('/login');
    else ElMessage.error(`保存规则失败: ${error.message}`);
  }
}

async function deleteRuleHandler(id) {
  try {
    await deleteRule(id);
    ElMessage.success(`规则 (ID: ${id}) 删除成功!`);
    fetchRules();
  } catch (error) {
    if (error.message === 'AUTH_FAILURE') router.push('/login');
    else ElMessage.error(`删除规则 (ID: ${id}) 失败: ${error.message}`);
  }
}
</script>

<style scoped>
/* Add any component-specific styles here */
</style>
