import { createRouter, createWebHistory } from 'vue-router'; // 从npm包导入
import Login from '../components/Login.vue';
import MainLayout from '../components/MainLayout.vue';
import InstanceMonitor from '../components/InstanceMonitor.vue';
import ProfileManagement from '../components/ProfileManagement.vue';
import PayloadManagement from '../components/PayloadManagement.vue';
import RuleManagement from '../components/RuleManagement.vue';

const routes = [
    {
        path: '/',
        redirect: '/login'
    },
    {
        path: '/login',
        name: 'Login',
        component: Login,
        props: true // 允许通过路由传递 props
    },
    {
        path: '/main',
        name: 'MainLayout',
        component: MainLayout,
        children: [
            { path: 'instances', name: 'Instances', component: InstanceMonitor },
            { path: 'profiles', name: 'Profiles', component: ProfileManagement },
            { path: 'payloads', name: 'Payloads', component: PayloadManagement },
            { path: 'rules', name: 'Rules', component: RuleManagement },
        ],
        beforeEnter: (to, from, next) => {
            // 检查是否存在 JWT token
            if (localStorage.getItem('jwt')) {
                next(); // 如果有 token，则允许进入 /main 路由
            } else {
                next('/login'); // 如果没有 token，则重定向到登录页
            }
        }
    },
    {
        path: '/:catchAll(.*)',
        redirect: '/login'
    }
];

const router = createRouter({ // 直接使用导入的函数
    history: createWebHistory(), // 直接使用导入的函数
    routes,
});

router.beforeEach((to, from, next) => {
    const publicPages = ['/login'];
    const authRequired = !publicPages.includes(to.path);
    const loggedIn = localStorage.getItem('jwt');

    if (authRequired && !loggedIn) {
        return next('/login');
    }

    next();
});

export default router;
