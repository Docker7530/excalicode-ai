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
              <ul class="card-features">
                <li>AI驱动的需求理解与拆解</li>
                <li>COSMIC功能点自动计算</li>
                <li>专业文档一键生成</li>
              </ul>
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
              <ul class="card-features">
                <li>智能识别Excel列信息</li>
                <li>自动筛选有效记录</li>
                <li>一键导出结果数据</li>
              </ul>
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

          <div class="feature-card admin-card" @click="navigateToBackend">
            <div class="card-header">
              <div class="card-icon admin-icon">
                <ElIcon :size="48">
                  <Setting />
                </ElIcon>
              </div>
              <div class="card-badge admin-badge">系统管理</div>
            </div>
            <div class="card-content">
              <h3 class="card-title">后台管理</h3>
              <ul class="card-features">
                <li>AI 模型厂商配置</li>
                <li>功能-模型映射管理</li>
                <li>智能模型动态切换</li>
              </ul>
            </div>
            <div class="card-action">
              <ElButton
                type="info"
                size="large"
                round
                class="action-button admin-button"
              >
                <ElIcon class="button-icon">
                  <Tools />
                </ElIcon>
                管理配置
              </ElButton>
            </div>
          </div>

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
              <ul class="card-features">
                <li>Excel 批量导入任务</li>
                <li>快速为执行人分派</li>
                <li>实时追踪执行状态</li>
              </ul>
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
              <ul class="card-features">
                <li>查看分配给我的任务</li>
                <li>随时标记完成状态</li>
                <li>掌握个人工作进度</li>
              </ul>
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
  Document,
  Lightning,
  List,
  Monitor,
  Setting,
  Star,
  Tools,
  Collection,
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

const navigateToBackend = () => {
  router.push('/backend-manage');
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

  &.admin-icon {
    color: #909399;
    background: linear-gradient(135deg, #f8f9fa, #e9ecef);
  }

  &.task-icon {
    color: #22c55e;
    background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  }

  &.my-task-icon {
    color: #409eff;
    background: linear-gradient(135deg, #eff6ff, #dbeafe);
  }
}

.card-badge {
  background: linear-gradient(135deg, #409eff, #67c23a);
  color: white;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 0.875rem;
  font-weight: 500;

  &.admin-badge {
    background: linear-gradient(135deg, #667eea, #764ba2);
  }

  &.task-badge {
    background: linear-gradient(135deg, #34d399, #10b981);
  }

  &.my-task-badge {
    background: linear-gradient(135deg, #60a5fa, #3b82f6);
  }
}

.admin-card {
  border: 2px solid rgba(102, 126, 234, 0.1);

  &::before {
    background: linear-gradient(90deg, #667eea, #764ba2);
  }

  &:hover {
    border-color: rgba(102, 126, 234, 0.3);
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

.admin-button {
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  border: none !important;

  &:hover {
    background: linear-gradient(135deg, #764ba2, #667eea) !important;
    box-shadow: 0 8px 20px rgba(102, 126, 234, 0.4) !important;
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

.card-features {
  list-style: none;
  padding: 0;
  margin: 0;

  li {
    font-size: 0.875rem;
    color: #475569;
    padding: 8px 0;
    position: relative;
    padding-left: 20px;

    &::before {
      content: '✦';
      position: absolute;
      left: 0;
      color: #67c23a;
      font-weight: 600;
    }
  }
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
