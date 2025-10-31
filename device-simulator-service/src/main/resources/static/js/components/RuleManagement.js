

import { getRules, createRule, updateRule, deleteRule, getAvailableRuleGroups } from '../api/rules.js';


import { getAvailablePayloadKeys } from '../api/payloads.js';





export default {


    data() {


        return {


            rules: [],


            ruleDialogVisible: false,


            isEditingRule: false,


            currentRule: {


                id: null,


                ruleGroup: '',


                requestKey: '',


                responseKey: '',


                description: ''


            },


            availableRuleGroups: [],


            availablePayloadKeys: [],


        };


    },


    created() {


        this.fetchRules();


    },


    methods: {


        async fetchRules() {


            try {


                this.rules = await getRules();


            } catch (error) {


                if (error.message === 'AUTH_FAILURE') {


                    this.$router.push('/login');


                } else {


                    console.error('获取规则失败:', error);


                    this.$message.error('获取规则失败!');


                }


            }


        },


        async fetchAvailableRuleGroups() {


            try {


                this.availableRuleGroups = await getAvailableRuleGroups();


            } catch (error) {


                if (error.message === 'AUTH_FAILURE') {


                    this.$router.push('/login');


                }


                else {


                    console.error('获取可用规则分组失败:', error);


                    this.$message.error('获取可用规则分组失败!');


                }


            }


        },


        async fetchAvailablePayloadKeys() {


            try {


                this.availablePayloadKeys = await getAvailablePayloadKeys();


            } catch (error) {


                if (error.message === 'AUTH_FAILURE') {


                    this.$router.push('/login');


                }


                else {


                    console.error('获取可用报文键失败:', error);


                    this.$message.error('获取可用报文键失败!');


                }


            }


        },


        openRuleDialog(rule) {


            this.isEditingRule = !!rule;


            this.fetchAvailablePayloadKeys();


            this.fetchAvailableRuleGroups();


            if (rule) {


                this.currentRule = JSON.parse(JSON.stringify(rule));


            } else {


                this.currentRule = { id: null, ruleGroup: '', requestKey: '', responseKey: '', description: '' };


            }


            this.ruleDialogVisible = true;


        },


        async saveRule() {


            try {


                let response;


                if (this.isEditingRule) {


                    response = await updateRule(this.currentRule.id, this.currentRule);


                } else {


                    response = await createRule(this.currentRule);


                }


                this.$message.success(`规则 ${this.currentRule.ruleGroup} ${this.isEditingRule ? '更新' : '创建'}成功!`);


                this.ruleDialogVisible = false;


                this.fetchRules();


            } catch (error) {


                if (error.message === 'AUTH_FAILURE') {


                    this.$router.push('/login');


                } else if (error.message.includes('已存在')) {


                    this.$message.error(error.message);


                } else {


                    console.error('保存规则失败:', error);


                    this.$message.error(`保存规则失败: ${error.message}`);


                }


            }


        },


        async deleteRule(id) {


            try {


                await deleteRule(id);


                this.$message.success(`规则 (ID: ${id}) 删除成功!`);


                this.fetchRules();


            } catch (error) {


                if (error.message === 'AUTH_FAILURE') {


                    this.$router.push('/login');


                }


                else {


                    console.error(`删除规则 (ID: ${id}) 失败:`, error);


                    this.$message.error(`删除规则 (ID: ${id}) 失败: ${error.message}`);


                }


            }


        },


    },


    template: `


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


                        <el-button size="small" type="danger" @click="deleteRule(scope.row.id)">删除</el-button>


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


                        <el-tooltip content="匹配传入请求的关键字，例如 '010203' 或 '{\"cmd\":\"status\"}'" placement="top">


                            <el-input v-model="currentRule.requestKey"></el-input>


                        </el-tooltip>


                    </el-form-item>


                    <el-form-item label="响应报文键">


                        <el-tooltip content="选择一个已存在的报文键作为匹配成功后的响应内容" placement="top">


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


    `


};
