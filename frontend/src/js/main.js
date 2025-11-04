import { createApp } from 'vue';
import App from '../App.vue'; // 导入根组件
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import router from './router/index.js';
import '../css/style.css';

const app = createApp(App); // 使用导入的App.vue组件

app.use(ElementPlus);
app.use(router);

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}

app.mount('#app');
