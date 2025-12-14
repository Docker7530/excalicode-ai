import { validateSession } from '@/api/auth';
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
    path: '/prompt-template-manage',
    name: 'PromptTemplateManage',
    component: () => import('@/views/PromptTemplateManagePage.vue'),
    meta: {
      title: '提示词模板管理 - EXCALICODE AI',
      requiresAuth: true,
    },
  },
  {
    path: '/function-configuration',
    name: 'FunctionConfigurationManage',
    component: () => import('@/views/FunctionConfigurationManagePage.vue'),
    meta: {
      title: '功能配置管理 - EXCALICODE AI',
      requiresAuth: true,
    },
  },
  {
    path: '/cache-manage',
    name: 'CacheManage',
    component: () => import('@/views/CacheManagePage.vue'),
    meta: {
      title: '缓存管理 - EXCALICODE AI',
      requiresAuth: true,
    },
  },
  {
    path: '/requirement-knowledge',
    name: 'RequirementKnowledge',
    component: () => import('@/views/RequirementKnowledgePage.vue'),
    meta: {
      title: '知识库管理 - EXCALICODE AI',
      requiresAuth: true,
      requiredRole: 'ADMIN',
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
  {
    path: '/tasks/allocation',
    name: 'TaskAllocation',
    component: () => import('@/views/TaskAllocationPage.vue'),
    meta: {
      title: '任务分配 - EXCALICODE AI',
      requiresAuth: true,
      requiredRole: 'ADMIN',
    },
  },
  {
    path: '/tasks/my',
    name: 'MyTasks',
    component: () => import('@/views/MyTasksPage.vue'),
    meta: {
      title: '我的任务 - EXCALICODE AI',
      requiresAuth: true,
    },
  },
  {
    path: '/system-settings',
    name: 'SystemSettings',
    component: () => import('@/views/SystemSettingManagePage.vue'),
    meta: {
      title: '系统设置 - EXCALICODE AI',
      requiresAuth: true,
      requiredRole: 'ADMIN',
    },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

let validatedToken = null;
let sessionValidationPromise = null;

const clearSession = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('username');
  localStorage.removeItem('role');
  validatedToken = null;
  sessionValidationPromise = null;
};

const ensureSessionValid = async () => {
  const token = localStorage.getItem('token');

  if (!token) {
    validatedToken = null;
    throw new Error('缺少凭证');
  }

  if (validatedToken === token) {
    return true;
  }

  if (!sessionValidationPromise) {
    sessionValidationPromise = validateSession()
      .then((data) => {
        if (data?.username) {
          localStorage.setItem('username', data.username);
        }

        if (data?.role) {
          localStorage.setItem('role', data.role);
        }

        validatedToken = token;
        return true;
      })
      .catch((error) => {
        validatedToken = null;
        throw error;
      })
      .finally(() => {
        sessionValidationPromise = null;
      });
  }

  return sessionValidationPromise;
};

/**
 * 全局前置守卫：认证检查 + 同步更新浏览器标题
 */
router.beforeEach(async (to, _from, next) => {
  // 更新浏览器标题
  document.title = to.meta.title || 'EXCALICODE AI';

  // 检查认证状态
  const requiresAuth = to.meta.requiresAuth !== false; // 默认需要认证
  const token = localStorage.getItem('token');

  if (requiresAuth && !token) {
    clearSession();
    next('/login');
    return;
  }

  if (requiresAuth && token) {
    try {
      await ensureSessionValid();
    } catch (error) {
      const isExpired = error?.response?.status === 401;
      clearSession();
      ElMessage.error(
        isExpired ? '登录已过期，请重新登录' : '登录状态校验失败，请稍后重试',
      );
      next('/login');
      return;
    }
  }

  if (to.path === '/login' && token) {
    try {
      await ensureSessionValid();
      next('/');
      return;
    } catch (_error) {
      clearSession();
    }
  }

  const currentRole = localStorage.getItem('role');

  if (to.meta.requiredRole && to.meta.requiredRole !== currentRole) {
    ElMessage.warning('暂无访问该页面的权限');
    next('/');
    return;
  }

  next();
});

export default router;
