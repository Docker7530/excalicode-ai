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
            <div class="card-header">
              <div class="card-icon requirement-icon">
                <ElIcon :size="48">
                  <Document />
                </ElIcon>
              </div>
              <div class="card-badge">核心功能</div>
            </div>
            <div class="card-content">
              <h3 class="card-title">COSMIC</h3>
            </div>
            <div class="card-action">
              <ElButton type="primary" size="large" round class="action-button">
                <ElIcon class="button-icon">
                  <ArrowRight />
                </ElIcon>
                开始分析
              </ElButton>
            </div>
          </div>

          <div class="feature-card" @click="navigateToVacation">
            <div class="card-header">
              <div class="card-icon vacation-icon">
                <ElIcon :size="48">
                  <List />
                </ElIcon>
              </div>
              <div class="card-badge">数据处理</div>
            </div>
            <div class="card-content">
              <h3 class="card-title">员工休假记录拆分</h3>
            </div>
            <div class="card-action">
              <ElButton type="warning" size="large" round class="action-button">
                <ElIcon class="button-icon">
                  <ArrowRight />
                </ElIcon>
                开始拆分
              </ElButton>
            </div>
          </div>

          <template v-if="isAdmin">
            <div class="feature-card provider-card" @click="navigateToProvider">
              <div class="card-header">
                <div class="card-icon provider-icon">
                  <ElIcon :size="48">
                    <OfficeBuilding />
                  </ElIcon>
                </div>
                <div class="card-badge provider-badge">系统管理</div>
              </div>
              <div class="card-content">
                <h3 class="card-title">AI 模型厂商管理</h3>
              </div>
              <div class="card-action">
                <ElButton type="primary" size="large" round class="action-button">
                  <ElIcon class="button-icon">
                    <ArrowRight />
                  </ElIcon>
                  管理厂商
                </ElButton>
              </div>
            </div>

            <div
              class="feature-card prompt-template-card"
              @click="navigateToPromptTemplate"
            >
              <div class="card-header">
                <div class="card-icon prompt-template-icon">
                  <ElIcon :size="48">
                    <Document />
                  </ElIcon>
                </div>
                <div class="card-badge prompt-template-badge">提示词</div>
              </div>
              <div class="card-content">
                <h3 class="card-title">提示词模板管理</h3>
              </div>
              <div class="card-action">
                <ElButton type="warning" size="large" round class="action-button">
                  <ElIcon class="button-icon">
                    <ArrowRight />
                  </ElIcon>
                  编辑模板
                </ElButton>
              </div>
            </div>

            <div
              class="feature-card function-config-card"
              @click="navigateToFunctionConfiguration"
            >
              <div class="card-header">
                <div class="card-icon function-config-icon">
                  <ElIcon :size="48">
                    <Connection />
                  </ElIcon>
                </div>
                <div class="card-badge function-config-badge">功能配置</div>
              </div>
              <div class="card-content">
                <h3 class="card-title">功能配置管理</h3>
              </div>
              <div class="card-action">
                <ElButton type="success" size="large" round class="action-button">
                  <ElIcon class="button-icon">
                    <ArrowRight />
                  </ElIcon>
                  配置映射
                </ElButton>
              </div>
            </div>

            <div class="feature-card cache-manage-card" @click="navigateToCacheManage">
              <div class="card-header">
                <div class="card-icon cache-manage-icon">
                  <ElIcon :size="48">
                    <Coin />
                  </ElIcon>
                </div>
                <div class="card-badge cache-manage-badge">系统维护</div>
              </div>
              <div class="card-content">
                <h3 class="card-title">缓存管理</h3>
              </div>
              <div class="card-action">
                <ElButton type="info" size="large" round class="action-button">
                  <ElIcon class="button-icon">
                    <ArrowRight />
                  </ElIcon>
                  清理缓存
                </ElButton>
              </div>
            </div>
          </template>

          <div
            v-if="isAdmin"
            class="feature-card task-card"
            @click="navigateToTaskAllocation"
          >
            <div class="card-header">
              <div class="card-icon task-icon">
                <ElIcon :size="48">
                  <Collection />
                </ElIcon>
              </div>
              <div class="card-badge task-badge">任务调度</div>
            </div>
            <div class="card-content">
              <h3 class="card-title">任务分配</h3>
            </div>
            <div class="card-action">
              <ElButton type="success" size="large" round class="action-button">
                <ElIcon class="button-icon">
                  <ArrowRight />
                </ElIcon>
                发布任务
              </ElButton>
            </div>
          </div>

          <div
            v-if="isRegularUser"
            class="feature-card my-task-card"
            @click="navigateToMyTasks"
          >
            <div class="card-header">
              <div class="card-icon my-task-icon">
                <ElIcon :size="48">
                  <List />
                </ElIcon>
              </div>
              <div class="card-badge my-task-badge">个人任务</div>
            </div>
            <div class="card-content">
              <h3 class="card-title">我的任务</h3>
            </div>
            <div class="card-action">
              <ElButton type="primary" size="large" round class="action-button">
                <ElIcon class="button-icon">
                  <ArrowRight />
                </ElIcon>
                查看任务
              </ElButton>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 特性介绍区域 -->
    <div class="highlights-section">
      <div class="highlights-container">
        <h2 class="section-title">为什么选择 EXCALICODE AI？</h2>
        <div class="highlights-grid">
          <div class="highlight-item">
            <div class="highlight-icon">
              <ElIcon :size="32" color="#67C23A">
                <Star />
              </ElIcon>
            </div>
            <h4 class="highlight-title">专业精准</h4>
            <p class="highlight-text">
              基于COSMIC国际标准，确保需求分析的专业性与准确性
            </p>
          </div>
          <div class="highlight-item">
            <div class="highlight-icon">
              <ElIcon :size="32" color="#E6A23C">
                <Lightning />
              </ElIcon>
            </div>
            <h4 class="highlight-title">高效智能</h4>
            <p class="highlight-text">
              AI技术加持，大幅提升需求分析速度，节省项目时间成本
            </p>
          </div>
          <div class="highlight-item">
            <div class="highlight-icon">
              <ElIcon :size="32" color="#F56C6C">
                <Tools />
              </ElIcon>
            </div>
            <h4 class="highlight-title">一站式服务</h4>
            <p class="highlight-text">
              从需求分析到文档生成，提供完整的需求管理解决方案
            </p>
          </div>
          <div class="highlight-item">
            <div class="highlight-icon">
              <ElIcon :size="32" color="#909399">
                <Monitor />
              </ElIcon>
            </div>
            <h4 class="highlight-title">现代体验</h4>
            <p class="highlight-text">
              简洁优雅的界面设计，提供流畅的用户交互体验
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import AppHeader from '@/components/AppHeader.vue';
import {
  ArrowRight,
  Coin,
  Collection,
  Connection,
  Document,
  Lightning,
  List,
  Monitor,
  OfficeBuilding,
  Star,
  Tools,
} from '@element-plus/icons-vue';

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

.hero-description {
  font-size: 1.125rem;
  color: #64748b;
  line-height: 1.7;
  max-width: 800px;
  margin: 0 auto;

  @media (max-width: 768px) {
    font-size: 1rem;
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
  overflow: hidden;

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
  }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.card-icon {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f8fafc, #e2e8f0);

  &.requirement-icon {
    color: #409eff;
  }

  &.vacation-icon {
    color: #e6a23c;
  }

  &.task-icon {
    color: #22c55e;
    background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  }

  &.my-task-icon {
    color: #409eff;
    background: linear-gradient(135deg, #eff6ff, #dbeafe);
  }

  &.provider-icon {
    color: #409eff;
    background: linear-gradient(135deg, #eff6ff, #dbeafe);
  }

  &.prompt-template-icon {
    color: #e6a23c;
    background: linear-gradient(135deg, #fff7ed, #ffedd5);
  }

  &.function-config-icon {
    color: #16a34a;
    background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  }

  &.cache-manage-icon {
    color: #f472b6;
    background: linear-gradient(135deg, #fdf2f8, #fce7f3);
  }
}

.card-badge {
  background: linear-gradient(135deg, #409eff, #67c23a);
  color: white;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 500;

  &.task-badge {
    background: linear-gradient(135deg, #34d399, #10b981);
  }

  &.my-task-badge {
    background: linear-gradient(135deg, #60a5fa, #3b82f6);
  }

  &.provider-badge {
    background: linear-gradient(135deg, #409eff, #5cacee);
  }

  &.prompt-template-badge {
    background: linear-gradient(135deg, #f59e0b, #fbbf24);
  }

  &.function-config-badge {
    background: linear-gradient(135deg, #34d399, #10b981);
  }

  &.cache-manage-badge {
    background: linear-gradient(135deg, #f472b6, #ec4899);
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
  margin-bottom: 32px;
}

.card-title {
  font-size: 1.75rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 24px 0;
}

.card-action {
  text-align: center;
}

.action-button {
  width: 100%;
  height: 56px;
  font-size: 1.125rem;
  font-weight: 600;
  border: none;
  transition: all 0.3s ease;

  .button-icon {
    margin-right: 8px;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 20px rgba(64, 158, 255, 0.3);
  }
}

.highlights-section {
  background: #ffffff;
  padding: 100px 0;

  @media (max-width: 768px) {
    padding: 80px 0;
  }
}

.highlights-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  text-align: center;
}

.section-title {
  font-size: 2.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 80px 0;

  @media (max-width: 768px) {
    font-size: 2rem;
    margin-bottom: 60px;
  }
}

.highlights-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 48px;
}

.highlight-item {
  text-align: center;
}

.highlight-icon {
  width: 80px;
  height: 80px;
  border-radius: 20px;
  background: #f8fafc;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 24px;
  transition: all 0.3s ease;
}

.highlight-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 12px 0;
}

.highlight-text {
  font-size: 0.975rem;
  color: #64748b;
  line-height: 1.6;
  margin: 0;
}

.highlight-item:hover .highlight-icon {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.1);
}
</style>
