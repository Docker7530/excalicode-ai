import { createRouter, createWebHistory } from 'vue-router';

/**
 * 应用路由表
 * path 与 meta.title 保持一致，便于统一维护导航与 SEO
 */
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginPage.vue'),
    meta: {
      title: '登录 - EXCALICODE AI',
      requiresAuth: false, // 登录页不需要认证
    },
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('@/views/HomePage.vue'),
    meta: {
      title: '首页 - EXCALICODE AI',
      requiresAuth: true,
    },
  },
  {
    path: '/requirement-analysis',
    name: 'RequirementAnalysis',
    component: () => import('@/views/RequirementAnalysisPage.vue'),
    meta: {
      title: '需求分析 - EXCALICODE AI',
      requiresAuth: true,
    },
  },
  {
    path: '/vacation-split',
    name: 'VacationSplit',
    component: () => import('@/views/VacationSplitPage.vue'),
    meta: {
      title: '员工休假记录拆分 - EXCALICODE AI',
      requiresAuth: true,
    },
  },
  {
    path: '/ai-provider-manage',
    name: 'AiProviderManage',
    component: () => import('@/views/AiProviderManagePage.vue'),
    meta: {
      title: 'AI模型厂商管理 - EXCALICODE AI',
      requiresAuth: true,
    },
  },
  {
    path: '/ai-function-mapping',
    name: 'AiFunctionMapping',
    component: () => import('@/views/AiFunctionMappingManagePage.vue'),
    meta: {
      title: 'AI功能-模型映射管理 - EXCALICODE AI',
      requiresAuth: true,
    },
  },
  {
    path: '/backend-manage',
    name: 'BackendManage',
    component: () => import('@/views/BackendManagePage.vue'),
    meta: {
      title: '后台管理 - EXCALICODE AI',
      requiresAuth: true,
    },
  },
  {
    path: '/user-manage',
    name: 'UserManage',
    component: () => import('@/views/UserManagePage.vue'),
    meta: {
      title: '人员管理 - EXCALICODE AI',
      requiresAuth: true,
      requiredRole: 'ADMIN',
    },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

/**
 * 全局前置守卫：认证检查 + 同步更新浏览器标题
 */
router.beforeEach((to, _from, next) => {
  // 更新浏览器标题
  document.title = to.meta.title || 'EXCALICODE AI';

  // 检查认证状态
  const requiresAuth = to.meta.requiresAuth !== false; // 默认需要认证
  const token = localStorage.getItem('token');
  const currentRole = localStorage.getItem('role');

  if (requiresAuth && !token) {
    next('/login');
    return;
  }

  if (to.path === '/login' && token) {
    next('/');
    return;
  }

  if (to.meta.requiredRole && to.meta.requiredRole !== currentRole) {
    ElMessage.warning('暂无访问该页面的权限');
    next('/');
    return;
  }

  next();
});

export default router;
