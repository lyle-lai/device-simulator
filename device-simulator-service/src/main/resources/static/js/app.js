const App = {
    data() {
        return {
            activeMenu: 'instances',
            profileStatuses: [], // 重命名：实例列表改为画像状态列表
            profiles: [],
            payloads: [],
            rules: [],
            profileDialogVisible: false,
            payloadDialogVisible: false,
            ruleDialogVisible: false,
            isEditing: false,
            isEditingPayload: false,
            isEditingRule: false,
            currentProfile: { // 初始化画像结构
                profileName: '',
                enabled: true,
                device: { id: '' },
                protocol: { type: 'passthrough', properties: { host: '', port: null } },
                codec: { type: 'passthrough' },
                strategy: {
                    type: 'passthrough',
                    properties: { intervalMillis: 0, filePath: '', speedFactor: 1.0 } // 确保数字类型有默认值
                },
                dataGenerator: {
                    type: 'passthrough',
                    properties: { payloadName: '', ruleGroup: '', groupKey: '' },
                    propertiesJson: ''
                }
            },
            currentPayload: { // 新增：当前编辑的报文
                id: null,
                payloadKey: '',
                groupKey: '',
                payloadType: '',
                content: '',
                description: ''
            },
            currentRule: { // 新增：当前编辑的规则
                id: null,
                ruleGroup: '',
                requestKey: '',
                responseKey: '',
                description: ''
            },
            availablePayloadGroupKeys: [], // 新增：可用的报文分组键列表
            availablePayloadKeys: [],    // 新增：可用的报文键列表 (用于规则响应)
            availableRuleGroups: [],  // 新增：可用的规则分组列表
            currentProfileJson: '',
            logDialogVisible: false,
            currentLogProfileName: '',
            logs: [],
            stompClient: null,
            logSubscription: null
        };
    },
    methods: {
        handleMenuSelect(index) {
            this.activeMenu = index;
            if (index === 'instances') {
                this.fetchProfileStatuses(); // 调用新的方法
            } else if (index === 'profiles') {
                this.fetchProfiles();
            } else if (index === 'payloads') {
                this.fetchPayloads();
            } else if (index === 'rules') {
                this.fetchRules();
            }
        },
        // 重命名：获取所有画像的状态
        async fetchProfileStatuses() {
            try {
                const response = await fetch('/api/instances'); // 调用新的API端点
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                this.profileStatuses = await response.json();
            } catch (error) {
                console.error('获取画像状态失败:', error);
                this.$message.error('获取画像状态失败!');
            }
        },
        async fetchProfiles() {
            try {
                const response = await fetch('/api/profiles');
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                this.profiles = await response.json();
            } catch (error) {
                console.error('获取画像失败:', error);
                this.$message.error('获取画像失败!');
            }
        },
        // 新增：获取报文列表
        async fetchPayloads() {
            try {
                const response = await fetch('/api/payloads');
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                this.payloads = await response.json();
            } catch (error) {
                console.error('获取报文失败:', error);
                this.$message.error('获取报文失败!');
            }
        },
        // 新增：获取规则列表
        async fetchRules() {
            try {
                const response = await fetch('/api/rules');
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                this.rules = await response.json();
            } catch (error) {
                console.error('获取规则失败:', error);
                this.$message.error('获取规则失败!');
            }
        },
        // 新增：获取所有可用的报文分组键
        async fetchAvailablePayloadGroupKeys() {
            try {
                const response = await fetch('/api/payloads');
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                const payloads = await response.json();
                // 提取并去重分组键
                this.availablePayloadGroupKeys = [...new Set(payloads.map(p => p.groupKey))].filter(key => key);
            } catch (error) {
                console.error('获取可用报文分组键失败:', error);
                this.$message.error('获取可用报文分组键失败!');
            }
        },
        // 新增：获取所有可用的报文键 (用于规则响应)
        async fetchAvailablePayloadKeys() {
            try {
                const response = await fetch('/api/payloads');
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                const payloads = await response.json();
                this.availablePayloadKeys = payloads.map(p => p.payloadKey);
            } catch (error) {
                console.error('获取可用报文键失败:', error);
                this.$message.error('获取可用报文键失败!');
            }
        },
        // 新增：获取所有可用的规则分组
        async fetchAvailableRuleGroups() {
            try {
                const response = await fetch('/api/rules');
                if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
                const rules = await response.json();
                // 提取并去重规则分组
                this.availableRuleGroups = [...new Set(rules.map(r => r.ruleGroup))];
            } catch (error) {
                console.error('获取可用规则分组失败:', error);
                this.$message.error('获取可用规则分组失败!');
            }
        },
        async startInstance(profileName) {
            try {
                const response = await fetch(`/api/instances/${profileName}/_start`, {
                    method: 'POST'
                });
                if (!response.ok) {
                    const errorText = await response.text();
                    throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
                }
                const updatedStatus = await response.json();
                // 更新profileStatuses中对应的状态
                const index = this.profileStatuses.findIndex(s => s.profile.profileName === profileName);
                if (index !== -1) {
                    this.profileStatuses.splice(index, 1, updatedStatus);
                }
                this.$message.success(`实例 ${profileName} 启动成功!`);
            } catch (error) {
                console.error(`启动实例 ${profileName} 失败:`, error);
                this.$message.error(`启动实例 ${profileName} 失败: ${error.message}`);
            }
        },
        async stopInstance(profileName) {
            try {
                const response = await fetch(`/api/instances/${profileName}/_stop`, {
                    method: 'POST'
                });
                if (!response.ok) {
                    const errorText = await response.text();
                    throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
                }
                const updatedStatus = await response.json();
                // 更新profileStatuses中对应的状态
                const index = this.profileStatuses.findIndex(s => s.profile.profileName === profileName);
                if (index !== -1) {
                    this.profileStatuses.splice(index, 1, updatedStatus);
                }
                this.$message.success(`实例 ${profileName} 停止成功!`);
            } catch (error) {
                console.error(`停止实例 ${profileName} 失败:`, error);
                this.$message.error(`停止实例 ${profileName} 失败: ${error.message}`);
            }
        },
        openProfileDialog(profile) {
            this.isEditing = !!profile;
            // 在打开画像对话框时，获取可用的报文键和规则分组
            this.fetchAvailablePayloadGroupKeys();
            this.fetchAvailableRuleGroups();

            if (profile) {
                this.currentProfile = JSON.parse(JSON.stringify(profile));
                // 确保所有嵌套对象和属性都存在，以避免v-model报错
                this.currentProfile.device = this.currentProfile.device || { id: '' };
                this.currentProfile.protocol = this.currentProfile.protocol || { type: 'passthrough', properties: { host: '', port: null } };
                this.currentProfile.protocol.properties = this.currentProfile.protocol.properties || { host: '', port: null };
                this.currentProfile.codec = this.currentProfile.codec || { type: 'passthrough' };
                this.currentProfile.strategy = this.currentProfile.strategy || { type: 'passthrough', properties: { intervalMillis: null, filePath: '', speedFactor: null } };
                this.currentProfile.strategy.properties = this.currentProfile.strategy.properties || { intervalMillis: null, filePath: '', speedFactor: null };
                this.currentProfile.dataGenerator = this.currentProfile.dataGenerator || { type: 'passthrough', properties: { groupKey: '', ruleGroup: '', groupKey: '' } };
                this.currentProfile.dataGenerator.properties = this.currentProfile.dataGenerator.properties || { groupKey: '', ruleGroup: '', groupKey: '' };

                // 对于随机生成器，将properties对象转换为JSON字符串以便在textarea中显示
                if (this.currentProfile.dataGenerator.type === 'random' && this.currentProfile.dataGenerator.properties) {
                    this.currentProfile.dataGenerator.propertiesJson = JSON.stringify(this.currentProfile.dataGenerator.properties, null, 2);
                } else {
                    this.currentProfile.dataGenerator.propertiesJson = '';
                }

                this.currentProfileJson = JSON.stringify(this.currentProfile, null, 2);
            } else {
                // 重置为默认结构以创建新画像
                this.currentProfile = {
                    profileName: '',
                    enabled: true,
                    device: { id: '' },
                    protocol: { type: 'passthrough', properties: { host: '', port: null } },
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
        },
        async saveProfile() {
            try {
                let profileDataToSend;

                if (this.currentProfileJson.trim() !== '') {
                    profileDataToSend = JSON.parse(this.currentProfileJson);
                } else {
                    profileDataToSend = JSON.parse(JSON.stringify(this.currentProfile));

                    // 处理随机生成器的propertiesJson
                    if (profileDataToSend.dataGenerator && profileDataToSend.dataGenerator.type === 'random' && profileDataToSend.dataGenerator.propertiesJson) {
                        profileDataToSend.dataGenerator.properties = JSON.parse(profileDataToSend.dataGenerator.propertiesJson);
                    }
                    if (profileDataToSend.dataGenerator) {
                        delete profileDataToSend.dataGenerator.propertiesJson;
                    }

                    // 确保协议属性正确嵌套
                    if (profileDataToSend.protocol && !profileDataToSend.protocol.properties) {
                        profileDataToSend.protocol.properties = {};
                    }
                    // 确保策略属性正确嵌套
                    if (profileDataToSend.strategy && !profileDataToSend.strategy.properties) {
                        profileDataToSend.strategy.properties = {};
                    }
                    // Ensure dataGenerator properties are nested correctly
                    if (profileDataToSend.dataGenerator && !profileDataToSend.dataGenerator.properties) {
                        profileDataToSend.dataGenerator.properties = {};
                    }
                    if (profileDataToSend.dataGenerator.type === 'database') {
                        profileDataToSend.dataGenerator.properties.groupKey = this.currentProfile.dataGenerator.properties.groupKey;
                    } else if (profileDataToSend.dataGenerator.type === 'db-rule-based') {
                        profileDataToSend.dataGenerator.properties.ruleGroup = this.currentProfile.dataGenerator.properties.ruleGroup;
                    }
                }

                const url = this.isEditing ? `/api/profiles/${profileDataToSend.profileName}` : '/api/profiles';
                const method = this.isEditing ? 'PUT' : 'POST';

                const response = await fetch(url, {
                    method: method,
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(profileDataToSend)
                });

                if (response.ok) {
                    this.$message.success(`画像 ${profileDataToSend.profileName} ${this.isEditing ? '更新' : '创建'}成功!`);
                    this.profileDialogVisible = false;
                    this.fetchProfiles();
                } else if (response.status === 409) {
                    this.$message.error(`画像 ${profileDataToSend.profileName} 已存在!`);
                } else {
                    const errorText = await response.text();
                    throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
                }
            } catch (error) {
                console.error('保存画像失败:', error);
                this.$message.error(`保存画像失败: ${error.message}`);
            }
        },
        async deleteProfile(profileName) {
            try {
                const response = await fetch(`/api/profiles/${profileName}`, {
                    method: 'DELETE'
                });

                if (response.ok) {
                    this.$message.success(`画像 ${profileName} 删除成功!`);
                    this.fetchProfiles();
                } else if (response.status === 409) {
                    this.$message.error(`画像 ${profileName} 正在运行, 无法删除!`);
                }
                else {
                    const errorText = await response.text();
                    throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
                }
            } catch (error) {
                console.error(`删除画像 ${profileName} 失败:`, error);
                this.$message.error(`删除画像 ${profileName} 失败: ${error.message}`);
            }
        },
        // 新增：打开报文编辑/新建对话框
        openPayloadDialog(payload) {
            this.isEditingPayload = !!payload;
            if (payload) {
                this.currentPayload = JSON.parse(JSON.stringify(payload));
            } else {
                this.currentPayload = {
                    id: null,
                    payloadKey: '',
                    groupKey: '',
                    payloadType: '',
                    content: '',
                    description: ''
                };
            }
            this.payloadDialogVisible = true;
        },
        // 新增：保存报文
        async savePayload() {
            try {
                const url = this.isEditingPayload ? `/api/payloads/${this.currentPayload.payloadKey}` : '/api/payloads';
                const method = this.isEditingPayload ? 'PUT' : 'POST';

                const response = await fetch(url, {
                    method: method,
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(this.currentPayload)
                });

                if (response.ok) {
                    this.$message.success(`报文 ${this.currentPayload.payloadKey} ${this.isEditingPayload ? '更新' : '创建'}成功!`);
                    this.payloadDialogVisible = false;
                    this.fetchPayloads();
                } else if (response.status === 409) {
                    this.$message.error(`报文键 ${this.currentPayload.payloadKey} 已存在!`);
                } else {
                    const errorText = await response.text();
                    throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
                }
            } catch (error) {
                console.error('保存报文失败:', error);
                this.$message.error(`保存报文失败: ${error.message}`);
            }
        },
        // 新增：删除报文
        async deletePayload(payloadKey) {
            try {
                const response = await fetch(`/api/payloads/${payloadKey}`, {
                    method: 'DELETE'
                });

                if (response.ok) {
                    this.$message.success(`报文 ${payloadKey} 删除成功!`);
                    this.fetchPayloads();
                } else {
                    const errorText = await response.text();
                    throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
                }
            } catch (error) {
                console.error(`删除报文 ${payloadKey} 失败:`, error);
                this.$message.error(`删除报文 ${payloadKey} 失败: ${error.message}`);
            }
        },
        // 新增：打开规则编辑/新建对话框
        openRuleDialog(rule) {
            this.isEditingRule = !!rule;
            this.fetchAvailablePayloadKeys(); // 获取可用的报文键
            if (rule) {
                this.currentRule = JSON.parse(JSON.stringify(rule));
            } else {
                this.currentRule = {
                    id: null,
                    ruleGroup: '',
                    requestKey: '',
                    responseKey: '',
                    description: ''
                };
            }
            this.ruleDialogVisible = true;
        },
        // 新增：保存规则
        async saveRule() {
            try {
                const url = this.isEditingRule ? `/api/rules/${this.currentRule.id}` : '/api/rules';
                const method = this.isEditingRule ? 'PUT' : 'POST';

                const response = await fetch(url, {
                    method: method,
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify(this.currentRule)
                });

                if (response.ok) {
                    this.$message.success(`规则 ${this.currentRule.ruleGroup} ${this.isEditingRule ? '更新' : '创建'}成功!`);
                    this.ruleDialogVisible = false;
                    this.fetchRules();
                } else if (response.status === 409) {
                    this.$message.error(`规则 ${this.currentRule.ruleGroup} - ${this.currentRule.requestKey} 已存在!`);
                } else {
                    const errorText = await response.text();
                    throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
                }
            } catch (error) {
                console.error('保存规则失败:', error);
                this.$message.error(`保存规则失败: ${error.message}`);
            }
        },
        // 新增：删除规则
        async deleteRule(id) {
            try {
                const response = await fetch(`/api/rules/${id}`, {
                    method: 'DELETE'
                });

                if (response.ok) {
                    this.$message.success(`规则 (ID: ${id}) 删除成功!`);
                    this.fetchRules();
                } else {
                    const errorText = await response.text();
                    throw new Error(`HTTP error! status: ${response.status}, message: ${errorText}`);
                }
            } catch (error) {
                console.error(`删除规则 (ID: ${id}) 失败:`, error);
                this.$message.error(`删除规则 (ID: ${id}) 失败: ${error.message}`);
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

            const socket = new SockJS('/ws');
            this.stompClient = Stomp.over(socket);

            this.stompClient.connect({}, frame => {
                console.log('Connected: ' + frame);
                this.logSubscription = this.stompClient.subscribe(`/topic/logs/${profileName}`, message => {
                    const log = JSON.parse(message.body);
                    this.logs.push(log);
                    this.$nextTick(() => {
                        const logContainer = document.querySelector('.log-container');
                        if (logContainer) {
                            logContainer.scrollTop = logContainer.scrollHeight;
                        }
                    });
                });
            }, error => {
                console.error('WebSocket连接错误:', error);
                this.$message.error('WebSocket连接失败，无法获取实时日志!');
            });
        },
        disconnectWebSocket() {
            if (this.stompClient && this.stompClient.connected) {
                if (this.logSubscription) {
                    this.logSubscription.unsubscribe();
                    this.logSubscription = null;
                }
                this.stompClient.disconnect(() => {
                    console.log('Disconnected from WebSocket');
                });
                this.stompClient = null;
            }
        }
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
                        if (JSON.stringify(this.currentProfile) !== JSON.stringify(parsed)) {
                            // For now, structured input will update raw JSON, and raw JSON will override structured on save. (This is handled in saveProfile)
                        }
                    } catch (e) {
                        // Ignore invalid JSON in raw textarea for now, error will be caught on save
                    }
                }
            }
        }
    },
    mounted() {
        this.fetchProfileStatuses();
        this.fetchProfiles();
    },
    beforeUnmount() {
        this.disconnectWebSocket();
    }
};; 

const app = Vue.createApp(App);
app.use(ElementPlus);
app.mount('#app');
