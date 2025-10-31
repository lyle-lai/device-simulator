
import { getPayloads, createPayload, updatePayload, deletePayload, getAvailablePayloadGroupKeys } from '../api/payloads.js';

export default {
    data() {
        return {
            payloads: [],
            payloadDialogVisible: false,
            isEditingPayload: false,
            currentPayload: {
                id: null,
                payloadKey: '',
                groupKey: '',
                payloadType: 'JSON',
                content: '',
                description: ''
            },
            availablePayloadGroupKeys: [],
        };
    },
    created() {
        this.fetchPayloads();
    },
    methods: {
        async fetchPayloads() {
            try {
                this.payloads = await getPayloads();
            } catch (error) {
                if (error.message === 'AUTH_FAILURE') {
                    this.$router.push('/login');
                } else {
                    console.error('获取报文失败:', error);
                    this.$message.error('获取报文失败!');
                }
            }
        },
        async fetchAvailablePayloadGroupKeys() {
            try {
                this.availablePayloadGroupKeys = await getAvailablePayloadGroupKeys();
            } catch (error) {
                if (error.message === 'AUTH_FAILURE') {
                    this.$router.push('/login');
                }
                else {
                    console.error('获取可用报文分组键失败:', error);
                    this.$message.error('获取可用报文分组键失败!');
                }
            }
        },
        openPayloadDialog(payload) {
            this.isEditingPayload = !!payload;
            this.fetchAvailablePayloadGroupKeys();
            if (payload) {
                this.currentPayload = JSON.parse(JSON.stringify(payload));
            } else {
                this.currentPayload = { id: null, payloadKey: '', groupKey: '', payloadType: 'JSON', content: '', description: '' };
            }
            this.payloadDialogVisible = true;
        },
        async savePayload() {
            try {
                let response;
                if (this.isEditingPayload) {
                    response = await updatePayload(this.currentPayload.payloadKey, this.currentPayload);
                } else {
                    response = await createPayload(this.currentPayload);
                }
                this.$message.success(`报文 ${this.currentPayload.payloadKey} ${this.isEditingPayload ? '更新' : '创建'}成功!`);
                this.payloadDialogVisible = false;
                this.fetchPayloads();
            } catch (error) {
                if (error.message === 'AUTH_FAILURE') {
                    this.$router.push('/login');
                } else if (error.message.includes('已存在')) {
                    this.$message.error(error.message);
                } else {
                    console.error('保存报文失败:', error);
                    this.$message.error(`保存报文失败: ${error.message}`);
                }
            }
        },
        async deletePayload(payloadKey) {
            try {
                await deletePayload(payloadKey);
                this.$message.success(`报文 ${payloadKey} 删除成功!`);
                this.fetchPayloads();
            } catch (error) {
                if (error.message === 'AUTH_FAILURE') {
                    this.$router.push('/login');
                } else {
                    console.error(`删除报文 ${payloadKey} 失败:`, error);
                    this.$message.error(`删除报文 ${payloadKey} 失败: ${error.message}`);
                }
            }
        },
    },
    template: `
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
                        <el-button size="small" type="danger" @click="deletePayload(scope.row.payloadKey)">删除</el-button>
                    </template>
                </el-table-column>
            </el-table>

            <!-- 报文编辑/新建对话框 -->
            <el-dialog :title="isEditingPayload ? '编辑报文' : '新建报文'" v-model="payloadDialogVisible" width="50%">
                <el-form :model="currentPayload" label-width="100px">
                    <el-form-item label="报文键">
                        <el-tooltip content="报文的唯一标识符，例如 'heartbeat_data'" placement="top">
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
                        <el-tooltip content="报文内容的类型，例如 'JSON', 'HEX', 'STRING'" placement="top">
                            <el-select v-model="currentPayload.payloadType" placeholder="请选择报文类型">
                                <el-option label="JSON" value="JSON"></el-option>
                                <el-option label="HEX" value="HEX"></el-option>
                                <el-option label="STRING" value="STRING"></el-option>
                            </el-select>
                        </el-tooltip>
                    </el-form-item>
                    <el-form-item label="内容">
                        <el-tooltip content="报文的实际内容，根据报文类型输入，例如 JSON 字符串或 HEX 字符串" placement="top">
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
    `
};
