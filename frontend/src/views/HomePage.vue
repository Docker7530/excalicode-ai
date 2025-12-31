<!--

  EXCALICODE AI 首页

  提供功能导航和产品介绍

-->

<template>
  <div class="home-page">
    <AppHeader :show-home-button="false" show-github-button />
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
              <h3 class="card-title">COSMIC</h3>
            </div>
          </div>

          <div class="feature-card" @click="navigateToVacation">
            <div class="card-content">
              <h3 class="card-title">休假转换</h3>
            </div>
          </div>

          <div
            v-if="isAdmin"
            class="feature-card"
            @click="navigateToTaskAllocation"
          >
            <div class="card-content">
              <h3 class="card-title">任务发布</h3>
            </div>
          </div>

          <template v-if="isAdmin">
            <div class="feature-card" @click="navigateToProvider">
              <div class="card-content">
                <h3 class="card-title">AI 模型</h3>
              </div>
            </div>

            <div class="feature-card" @click="navigateToPromptTemplate">
              <div class="card-content">
                <h3 class="card-title">提示词</h3>
              </div>
            </div>

            <div class="feature-card" @click="navigateToFunctionConfiguration">
              <div class="card-content">
                <h3 class="card-title">功能配置</h3>
              </div>
            </div>

            <div class="feature-card" @click="navigateToKnowledge">
              <div class="card-content">
                <h3 class="card-title">知识库</h3>
              </div>
            </div>

            <div class="feature-card" @click="navigateToCacheManage">
              <div class="card-content">
                <h3 class="card-title">缓存管理</h3>
              </div>
            </div>

            <div class="feature-card" @click="navigateToSystemSettings">
              <div class="card-content">
                <h3 class="card-title">系统设置</h3>
              </div>
            </div>
          </template>

          <div
            v-if="isRegularUser"
            class="feature-card"
            @click="navigateToMyTasks"
          >
            <div class="card-content">
              <h3 class="card-title">我的任务</h3>
            </div>
          </div>
        </div>

        <!-- 使用技巧 -->
        <section class="tips-section">
          <div class="tips-card">
            <div class="tips-card-body">
              <div v-if="tipsLoading" class="tips-loading">加载中...</div>
              <MarkdownPreview
                v-else-if="usageTipsMarkdown"
                :content="usageTipsMarkdown"
              />
              <p v-else class="tips-empty">
                暂无内容，可在【系统设置】配置 key：home.usageTips
              </p>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import AppHeader from '@/components/AppHeader.vue';
import MarkdownPreview from '@/components/MarkdownPreview.vue';
import { getSettingByKey } from '@/api/sysSetting.js';

const router = useRouter();
const role = ref(localStorage.getItem('role') || 'USER');

const isAdmin = computed(() => role.value === 'ADMIN');
const isRegularUser = computed(() => role.value !== 'ADMIN');

const tipsLoading = ref(false);
const usageTipsMarkdown = ref('');

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

const navigateToKnowledge = () => {
  router.push('/requirement-knowledge');
};

const navigateToCacheManage = () => {
  router.push('/cache-manage');
};

const navigateToSystemSettings = () => {
  router.push('/system-settings');
};

const navigateToTaskAllocation = () => {
  router.push('/tasks/allocation');
};

const navigateToMyTasks = () => {
  router.push('/tasks/my');
};

const loadUsageTips = async () => {
  tipsLoading.value = true;
  try {
    const response = await getSettingByKey('home.usageTips');
    usageTipsMarkdown.value = response?.configValue || '';
  } catch (error) {
    console.error('加载使用技巧失败', error);
    usageTipsMarkdown.value = '';
  } finally {
    tipsLoading.value = false;
  }
};

const refreshRole = () => {
  role.value = localStorage.getItem('role') || 'USER';
};

onMounted(() => {
  refreshRole();
  loadUsageTips();
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

.tips-section {
  margin: 48px auto 0;
  max-width: 920px;
}

.tips-card {
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(148, 163, 184, 0.22);
  box-shadow: 0 16px 34px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.tips-card-body {
  padding: 18px 20px;
  color: #334155;
  line-height: 1.7;
}

.tips-loading {
  color: #64748b;
  font-weight: 500;
}

.tips-empty {
  margin: 0;
  color: #64748b;
  font-size: 0.95rem;
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
  transition:
    all 0.3s cubic-bezier(0.4, 0, 0.2, 1),
    color 0.3s ease;
  color: #1e293b;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
  overflow: hidden;
  min-height: 180px;
  border: 2px solid transparent;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: inherit;
    background: linear-gradient(135deg, #409eff, #67c23a);
    opacity: 0;
    transform: scale(1.05);
    transition:
      opacity 0.3s ease,
      transform 0.3s ease;
    z-index: 0;
    pointer-events: none;
  }

  &:hover {
    transform: translateY(-8px);
    color: #ffffff;
    box-shadow:
      0 20px 25px rgba(0, 0, 0, 0.1),
      0 10px 10px rgba(0, 0, 0, 0.04);

    &::before {
      opacity: 1;
      transform: scale(1);
    }
  }

  @media (max-width: 768px) {
    padding: 32px 24px;
    border-radius: 16px;
    min-height: 140px;
  }
}

.card-content {
  margin: 0;
  width: 100%;
  position: relative;
  z-index: 1;
}

.card-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: inherit;
  margin: 0;
}
</style>
