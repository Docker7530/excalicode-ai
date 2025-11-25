<!--

  EXCALICODE AI 首页

  提供功能导航和产品介绍

-->

<template>
  <div class="home-page">
    <AppHeader :show-home-button="false" />
    <!-- Hero 区域 -->
    <div class="hero-section">
      <div class="hero-container">
        <!-- 标题与简介 -->
        <div class="hero-header">
          <h1 class="hero-title">EXCALICODE AI</h1>
          <p class="hero-subtitle">智能化软件需求分析与AI助手平台</p>
        </div>

        <!-- 功能卡片 -->
        <div class="features-grid">
          <div class="feature-card" @click="navigateToRequirement">
            <div class="card-content">
              <h3 class="card-title">COSMIC 需求分析</h3>
            </div>
          </div>

          <div class="feature-card" @click="navigateToVacation">
            <div class="card-content">
              <h3 class="card-title">员工休假记录拆分</h3>
            </div>
          </div>

          <template v-if="isAdmin">
            <div class="feature-card provider-card" @click="navigateToProvider">
              <div class="card-content">
                <h3 class="card-title">AI 模型厂商管理</h3>
              </div>
            </div>

            <div
              class="feature-card prompt-template-card"
              @click="navigateToPromptTemplate"
            >
              <div class="card-content">
                <h3 class="card-title">提示词模板管理</h3>
              </div>
            </div>

            <div
              class="feature-card function-config-card"
              @click="navigateToFunctionConfiguration"
            >
              <div class="card-content">
                <h3 class="card-title">功能配置管理</h3>
              </div>
            </div>

            <div class="feature-card cache-manage-card" @click="navigateToCacheManage">
              <div class="card-content">
                <h3 class="card-title">缓存管理</h3>
              </div>
            </div>
          </template>

          <div
            v-if="isAdmin"
            class="feature-card task-card"
            @click="navigateToTaskAllocation"
          >
            <div class="card-content">
              <h3 class="card-title">任务分配</h3>
            </div>
          </div>

          <div
            v-if="isRegularUser"
            class="feature-card my-task-card"
            @click="navigateToMyTasks"
          >
            <div class="card-content">
              <h3 class="card-title">我的任务</h3>
            </div>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import AppHeader from '@/components/AppHeader.vue';

const router = useRouter();
const role = ref(localStorage.getItem('role') || 'USER');

const isAdmin = computed(() => role.value === 'ADMIN');
const isRegularUser = computed(() => role.value !== 'ADMIN');

const navigateToRequirement = () => {
  router.push('/requirement-analysis');
};

const navigateToVacation = () => {
  router.push('/vacation-split');
};

const navigateToProvider = () => {
  router.push('/ai-provider-manage');
};

const navigateToPromptTemplate = () => {
  router.push('/prompt-template-manage');
};

const navigateToFunctionConfiguration = () => {
  router.push('/function-configuration');
};

const navigateToCacheManage = () => {
  router.push('/cache-manage');
};

const navigateToTaskAllocation = () => {
  router.push('/tasks/allocation');
};

const navigateToMyTasks = () => {
  router.push('/tasks/my');
};

const refreshRole = () => {
  role.value = localStorage.getItem('role') || 'USER';
};

onMounted(() => {
  refreshRole();
  window.addEventListener('storage', refreshRole);
});

onBeforeUnmount(() => {
  window.removeEventListener('storage', refreshRole);
});
</script>

<style lang="scss" scoped>
.home-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 50%, #f1f5f9 100%);
}

.hero-section {
  padding: 80px 0 120px;

  @media (max-width: 768px) {
    padding: 60px 0 80px;
  }
}

.hero-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
}

.hero-header {
  text-align: center;
  margin-bottom: 80px;

  @media (max-width: 768px) {
    margin-bottom: 60px;
  }
}

.hero-title {
  font-size: 4rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 16px 0;
  letter-spacing: -0.02em;
  background: linear-gradient(135deg, #409eff, #67c23a);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;

  @media (max-width: 768px) {
    font-size: 2.5rem;
  }
}

.hero-subtitle {
  font-size: 1.5rem;
  font-weight: 500;
  color: #475569;
  margin: 0 0 24px 0;

  @media (max-width: 768px) {
    font-size: 1.25rem;
  }
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
  gap: 32px;
  margin-top: 80px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    gap: 24px;
    margin-top: 60px;
  }
}

.feature-card {
  background: #ffffff;
  border-radius: 24px;
  padding: 40px 32px;
  box-shadow:
    0 4px 6px rgba(0, 0, 0, 0.05),
    0 10px 15px rgba(0, 0, 0, 0.03);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 2px solid transparent;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  overflow: hidden;
  min-height: 180px;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, #409eff, #67c23a);
    transform: translateX(-100%);
    transition: transform 0.3s ease;
  }

  &:hover {
    transform: translateY(-8px);
    box-shadow:
      0 20px 25px rgba(0, 0, 0, 0.1),
      0 10px 10px rgba(0, 0, 0, 0.04);

    &::before {
      transform: translateX(0);
    }
  }

  @media (max-width: 768px) {
    padding: 32px 24px;
    border-radius: 16px;
    min-height: 140px;
  }
}

.provider-card {
  border: 2px solid rgba(64, 158, 255, 0.15);

  &::before {
    background: linear-gradient(90deg, #409eff, #5cacee);
  }

  &:hover {
    border-color: rgba(64, 158, 255, 0.4);
  }
}

.prompt-template-card {
  border: 2px solid rgba(245, 158, 11, 0.15);

  &::before {
    background: linear-gradient(90deg, #f59e0b, #fbbf24);
  }

  &:hover {
    border-color: rgba(251, 191, 36, 0.45);
  }
}

.function-config-card {
  border: 2px solid rgba(52, 211, 153, 0.2);

  &::before {
    background: linear-gradient(90deg, #34d399, #10b981);
  }

  &:hover {
    border-color: rgba(16, 185, 129, 0.45);
  }
}

.cache-manage-card {
  border: 2px solid rgba(244, 114, 182, 0.2);

  &::before {
    background: linear-gradient(90deg, #f472b6, #ec4899);
  }

  &:hover {
    border-color: rgba(236, 72, 153, 0.45);
  }
}

.task-card {
  border: 2px solid rgba(34, 197, 94, 0.15);

  &::before {
    background: linear-gradient(90deg, #34d399, #10b981);
  }

  &:hover {
    border-color: rgba(16, 185, 129, 0.4);
  }
}

.my-task-card {
  border: 2px solid rgba(96, 165, 250, 0.15);

  &::before {
    background: linear-gradient(90deg, #60a5fa, #3b82f6);
  }

  &:hover {
    border-color: rgba(59, 130, 246, 0.35);
  }
}

.card-content {
  margin: 0;
  width: 100%;
}

.card-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

</style>
