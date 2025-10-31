import router from './router/index.js';

// 导入 Element Plus 图标
import * as ElementPlusIconsVue from 'https://unpkg.com/@element-plus/icons-vue';

// 根组件，后续会替换为更复杂的布局组件
const App = {
    template: '<router-view></router-view>',
};

const app = Vue.createApp(App);
app.use(ElementPlus);
app.use(router);

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
}

app.mount('#app');
