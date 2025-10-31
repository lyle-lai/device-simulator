
import { getProfiles, createProfile, updateProfile, deleteProfile } from '../api/profiles.js';

import { getAvailablePayloadGroupKeys } from '../api/payloads.js';

import { getAvailableRuleGroups } from '../api/rules.js';



export default {

    data() {

        return {

            profiles: [],

            profileDialogVisible: false,

            isEditing: false,

            currentProfile: {

                profileName: '',

                enabled: true,

                device: { id: '' },

                protocol: { type: 'passthrough', properties: { host: '', port: null, localAddress: '', reconnectDelay: 5000 } },

                codec: { type: 'passthrough' },

                strategy: {

                    type: 'passthrough',

                    properties: { intervalMillis: null, filePath: '', speedFactor: null }

                },

                dataGenerator: {

                    type: 'passthrough',

                    properties: { payloadName: '', ruleGroup: '', groupKey: '' },

                    propertiesJson: ''

                }

            },

            currentProfileJson: '',

            profileRules: {

                profileName: [{ required: true, message: '请输入画像名称', trigger: 'blur' }],

                'device.id': [{ required: true, message: '请输入设备ID', trigger: 'blur' }],

                'protocol.properties.host': [{ required: true, message: '请输入主机地址', trigger: 'blur' }],

                'protocol.properties.port': [{ required: true, message: '请输入端口号', trigger: ['blur', 'change'] }],

                'strategy.properties.intervalMillis': [{ required: true, message: '请输入推送间隔', trigger: ['blur', 'change'] }],

                'dataGenerator.properties.groupKey': [{ required: true, message: '请选择报文分组键', trigger: 'change' }],

                'dataGenerator.properties.ruleGroup': [{ required: true, message: '请选择规则分组', trigger: 'change' }]

            },

            availablePayloadGroupKeys: [],

            availableRuleGroups: [],

        };

    },

    created() {

        this.fetchProfiles();

    },

    watch: {

        currentProfile: {

            handler(newValue) {

                if (this.profileDialogVisible) {

                    const tempProfile = JSON.parse(JSON.stringify(newValue));

                    if (tempProfile.dataGenerator) {

                        delete tempProfile.dataGenerator.propertiesJson;

                    }

                    if (tempProfile.protocol && !tempProfile.protocol.properties) {

                        tempProfile.protocol.properties = {};

                    }

                    if (tempProfile.strategy && !tempProfile.strategy.properties) {

                        tempProfile.strategy.properties = {};

                    }

                    if (tempProfile.dataGenerator && !tempProfile.dataGenerator.properties) {

                        tempProfile.dataGenerator.properties = {};

                    }

                    this.currentProfileJson = JSON.stringify(tempProfile, null, 2);

                }

            },

            deep: true

        },

        currentProfileJson: {

            handler(newValue) {

                if (this.profileDialogVisible && newValue.trim() !== '') {

                    try {

                        const parsed = JSON.parse(newValue);

                        // Optionally, you can add more sophisticated checks here

                    } catch (e) {

                        // Handle JSON parsing error if needed

                    }

                }

            }

        }

    },

    methods: {

        async fetchProfiles() {

            try {

                this.profiles = await getProfiles();

            } catch (error) {

                if (error.message === 'AUTH_FAILURE') {

                    this.$router.push('/login');

                } else {

                    console.error('获取画像列表失败:', error);

                    this.$message.error('获取画像列表失败!');

                }

            }

        },

        async fetchAvailablePayloadGroupKeys() {

            try {

                this.availablePayloadGroupKeys = await getAvailablePayloadGroupKeys();

            } catch (error) {

                if (error.message === 'AUTH_FAILURE') {

                    this.$router.push('/login');

                } else {

                    console.error('获取可用报文分组键失败:', error);

                    this.$message.error('获取可用报文分组键失败!');

                }

            }

        },

        async fetchAvailableRuleGroups() {

            try {

                this.availableRuleGroups = await getAvailableRuleGroups();

            } catch (error) {

                if (error.message === 'AUTH_FAILURE') {

                    this.$router.push('/login');

                } else {

                    console.error('获取可用规则分组失败:', error);

                    this.$message.error('获取可用规则分组失败!');

                }

            }

        },

        openProfileDialog(profile) {

            this.isEditing = !!profile;

            this.fetchAvailablePayloadGroupKeys();

            this.fetchAvailableRuleGroups();



            if (profile) {

                this.currentProfile = JSON.parse(JSON.stringify(profile));

                this.currentProfile.device = this.currentProfile.device || { id: '' };

                this.currentProfile.protocol = this.currentProfile.protocol || { type: 'passthrough', properties: { host: '', port: null, localAddress: '', reconnectDelay: 5000 } };

                this.currentProfile.protocol.properties = this.currentProfile.protocol.properties || { host: '', port: null, localAddress: '', reconnectDelay: 5000 };

                this.currentProfile.codec = this.currentProfile.codec || { type: 'passthrough' };

                this.currentProfile.strategy = this.currentProfile.strategy || { type: 'passthrough', properties: { intervalMillis: null, filePath: '', speedFactor: null } };

                this.currentProfile.strategy.properties = this.currentProfile.strategy.properties || { intervalMillis: null, filePath: '', speedFactor: null };

                this.currentProfile.dataGenerator = this.currentProfile.dataGenerator || { type: 'passthrough', properties: { groupKey: '', ruleGroup: '', groupKey: '' } };

                this.currentProfile.dataGenerator.properties = this.currentProfile.dataGenerator.properties || { groupKey: '', ruleGroup: '', groupKey: '' };



                if (this.currentProfile.dataGenerator.type === 'random' && this.currentProfile.dataGenerator.properties) {

                    this.currentProfile.dataGenerator.propertiesJson = JSON.stringify(this.currentProfile.dataGenerator.properties, null, 2);

                } else {

                    this.currentProfile.dataGenerator.propertiesJson = '';

                }



                this.currentProfileJson = JSON.stringify(this.currentProfile, null, 2);

            } else {

                this.currentProfile = {

                    profileName: '',

                    enabled: true,

                    device: { id: '' },

                    protocol: { type: 'passthrough', properties: { host: '', port: null, localAddress: '', reconnectDelay: 5000 } },

                    codec: { type: 'passthrough' },

                    strategy: {

                        type: 'passthrough',

                        properties: { intervalMillis: null, filePath: '', speedFactor: null }

                    },

                    dataGenerator: {

                        type: 'passthrough',

                        properties: { payloadName: '', ruleGroup: '' },

                        propertiesJson: ''

                    }

                };

                this.currentProfileJson = '';

            }

            this.profileDialogVisible = true;

            this.$nextTick(() => {

                if (this.$refs.profileForm) {

                    this.$refs.profileForm.clearValidate();

                }

            });

        },

        saveProfile() {

            this.$refs.profileForm.validate(async (valid) => {

                if (valid) {

                    try {

                        let profileDataToSend;

                        if (this.currentProfileJson.trim() !== '') {

                            profileDataToSend = JSON.parse(this.currentProfileJson);

                        } else {

                            profileDataToSend = JSON.parse(JSON.stringify(this.currentProfile));

                            if(profileDataToSend.dataGenerator) delete profileDataToSend.dataGenerator.propertiesJson;

                        }



                        let response;

                        if (this.isEditing) {

                            response = await updateProfile(profileDataToSend.profileName, profileDataToSend);

                        } else {

                            response = await createProfile(profileDataToSend);

                        }

                        

                        this.$message.success(`画像 ${profileDataToSend.profileName} ${this.isEditing ? '更新' : '创建'}成功!`);

                        this.profileDialogVisible = false;

                        this.fetchProfiles();

                    } catch (error) {

                        if (error.message === 'AUTH_FAILURE') {

                            this.$router.push('/login');

                        }

                        else if (error.message.includes('已存在')) {

                            this.$message.error(error.message);

                        } else {

                            console.error('保存画像失败:', error);

                            this.$message.error(`保存画像失败: ${error.message}`);

                        }

                    }

                } else {

                    console.log('表单校验失败!');

                    this.$message.error('请检查输入项是否都已正确填写！');

                    return false;

                }

            });

        },

        async deleteProfile(profileName) {

            try {

                await deleteProfile(profileName);

                this.$message.success(`画像 ${profileName} 删除成功!`);

                this.fetchProfiles();

            } catch (error) {

                if (error.message === 'AUTH_FAILURE') {

                    this.$router.push('/login');

                } else if (error.message.includes('正在运行')) {

                    this.$message.error(error.message);

                }

                 else {

                    console.error(`删除画像 ${profileName} 失败:`, error);

                    this.$message.error(`删除画像 ${profileName} 失败: ${error.message}`);

                }

            }

        },

    },

    template: `

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

                        <el-button size="small" type="danger" @click="deleteProfile(scope.row.profileName)">删除</el-button>

                    </template>

                </el-table-column>

            </el-table>



            <!-- Profile编辑/新建对话框 -->

            <el-dialog :title="isEditing ? '编辑画像' : '新建画像'" v-model="profileDialogVisible" width="60%">

                <el-form :model="currentProfile" :rules="profileRules" ref="profileForm" label-width="120px">

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

                        <template #header>

                            <div class="card-header">

                                <span>协议配置</span>

                            </div>

                        </template>

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

                                <el-tooltip content="可选，指定客户端连接时绑定的本地IP地址，用于IP鉴权等场景" placement="top">

                                    <el-input v-model="currentProfile.protocol.properties.localAddress" placeholder="留空则使用默认IP"></el-input>

                                </el-tooltip>

                            </el-form-item>

                            <el-form-item label="重连间隔(毫秒)">

                                <el-tooltip content="客户端断线后，尝试重新连接的延迟时间，单位为毫秒" placement="top">

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

                        <template #header>

                            <div class="card-header">

                                <span>编解码器配置</span>

                            </div>

                        </template>

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

                        <template #header>

                            <div class="card-header">

                                <span>策略配置</span>

                            </div>

                        </template>

                        <el-form-item label="策略类型" prop="strategy.type">

                            <el-select v-model="currentProfile.strategy.type" placeholder="请选择策略类型">

                                <el-option label="周期性推送" value="periodic-push"></el-option>

                                <el-option label="请求-响应" value="request-response"></el-option>

                                <el-option label="原始数据回放 (已废弃)" value="raw-replay"></el-option>

                            </el-select>

                        </el-form-item>

                        

                        <!-- 周期性推送策略配置 -->

                        <template v-if="currentProfile.strategy.type === 'periodic-push'">

                            <el-form-item label="推送间隔(毫秒)" prop="strategy.properties.intervalMillis">

                                <el-input-number v-model="currentProfile.strategy.properties.intervalMillis" :min="100"></el-input-number>

                            </el-form-item>

                            <el-form-item label="数据生成器类型" prop="dataGenerator.type">

                                <el-select v-model="currentProfile.dataGenerator.type" placeholder="请选择数据生成器类型">

                                    <el-option label="随机生成器" value="random-generator"></el-option>

                                    <el-option label="数据库生成器" value="database-generator"></el-option>

                                    <el-option label="直通生成器" value="passthrough-generator"></el-option>

                            </el-select>

                            </el-form-item>

                            <template v-if="currentProfile.dataGenerator.type === 'random-generator'">

                                <el-form-item label="随机属性 (JSON)">

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

                            <el-form-item label="数据生成器类型" prop="dataGenerator.type">

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

                            <el-alert title="此策略已废弃，建议使用周期性推送策略配合自定义数据生成器实现相同功能。" type="warning" show-icon :closable="false"></el-alert>

                            <el-form-item label="回放文件路径">

                                <el-input v-model="currentProfile.strategy.properties.filePath"></el-input>

                            </el-form-item>

                            <el-form-item label="回放速度(倍)">

                                <el-input-number v-model="currentProfile.strategy.properties.speedFactor" :min="0.1"></el-input-number>

                            </el-form-item>

                        </template>

                    </el-card>



                    <!-- 原始JSON配置 (作为备用或高级配置) -->

                    <el-card class="box-card">

                        <template #header>

                            <div class="card-header">

                                <span>原始JSON配置 (高级)</span>

                            </div>

                        </template>

                        <el-form-item label="原始JSON">

                            <el-input type="textarea" :rows="10" v-model="currentProfileJson" placeholder="请输入完整的JSON配置"></el-input>

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

    `

};


