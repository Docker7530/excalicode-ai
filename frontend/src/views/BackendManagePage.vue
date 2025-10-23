<template>
  <div class="backend-manage-page">
    <!-- Hero 区域 -->
    <div class="hero-section">
      <div class="hero-background">
        <div class="animated-gradient"></div>
      </div>
      <div class="hero-content">
        <ElButton
          :icon="ArrowLeft"
          circle
          size="large"
          class="back-button"
          @click="goBack"
        />
        <div class="hero-text">
          <h1 class="hero-title">
            <ElIcon class="title-icon"><Setting /></ElIcon>
            后台管理中心
          </h1>
        </div>
      </div>
    </div>

    <!-- 管理模块卡片 -->
    <div class="modules-section">
      <div class="modules-container">
        <div class="module-card provider-card" @click="navigateToProvider">
          <div class="card-icon">
            <ElIcon :size="72"><OfficeBuilding /></ElIcon>
          </div>
          <h2 class="card-title">AI 模型厂商管理</h2>
        </div>

        <div class="module-card mapping-card" @click="navigateToMapping">
          <div class="card-icon">
            <ElIcon :size="72"><Connection /></ElIcon>
          </div>
          <h2 class="card-title">功能-模型映射管理</h2>
        </div>

        <div class="module-card prompt-card" @click="navigateToPromptTemplate">
          <div class="card-icon">
            <ElIcon :size="72"><Document /></ElIcon>
          </div>
          <h2 class="card-title">提示词模板管理</h2>
        </div>

        <div
          class="module-card prompt-mapping-card"
          @click="navigateToPromptMapping"
        >
          <div class="card-icon">
            <ElIcon :size="72"><Link /></ElIcon>
          </div>
          <h2 class="card-title">功能-提示词映射管理</h2>
        </div>
      </div>
    </div>

    <!-- 功能说明区域 -->
    <div class="info-section">
      <div class="info-container">
        <div class="info-card">
          <ElIcon :size="32" color="#409EFF"><InfoFilled /></ElIcon>
          <div class="info-content">
            <h3>配置说明</h3>
            <p>
              先在 <strong>AI 模型厂商管理</strong> 中配置厂商和模型，在
              <strong>提示词模板管理</strong> 中编辑提示词内容。然后在
              <strong>功能-模型映射管理</strong> 和
              <strong>功能-提示词映射管理</strong>
              中为每个功能指定使用的模型和提示词。未配置的功能将自动使用默认配置。
            </p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import {
  ArrowLeft,
  Connection,
  Document,
  InfoFilled,
  Link,
  OfficeBuilding,
  Setting,
} from '@element-plus/icons-vue';

const router = useRouter();

const goBack = () => {
  router.push('/');
};

const navigateToProvider = () => {
  router.push('/ai-provider-manage');
};

const navigateToMapping = () => {
  router.push('/ai-function-mapping');
};

const navigateToPromptTemplate = () => {
  router.push('/prompt-template-manage');
};

const navigateToPromptMapping = () => {
  router.push('/function-prompt-mapping');
};
</script>

<style lang="scss" scoped>
.backend-manage-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  position: relative;
  overflow: hidden;
}

// Hero 区域
.hero-section {
  position: relative;
  padding: 80px 24px 60px;
  z-index: 2;
}

.hero-background {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 400px;
  overflow: hidden;
  opacity: 0.3;
}

.animated-gradient {
  position: absolute;
  width: 200%;
  height: 200%;
  background:
    radial-gradient(
      circle at 20% 50%,
      rgba(255, 255, 255, 0.3) 0%,
      transparent 50%
    ),
    radial-gradient(
      circle at 80% 50%,
      rgba(255, 255, 255, 0.2) 0%,
      transparent 50%
    );
  animation: gradientMove 15s ease infinite;
}

@keyframes gradientMove {
  0%,
  100% {
    transform: translate(0, 0);
  }
  50% {
    transform: translate(-50px, -50px);
  }
}

.hero-content {
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  text-align: center;
}

.back-button {
  position: absolute;
  left: 0;
  top: 0;
  background: rgba(255, 255, 255, 0.2);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  color: white;
  transition: all 0.3s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.3);
    transform: translateX(-4px);
  }
}

.hero-text {
  padding: 0 80px;
}

.hero-title {
  font-size: 3.5rem;
  font-weight: 700;
  color: white;
  margin: 0 0 24px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  text-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);

  .title-icon {
    animation: rotate 20s linear infinite;
  }

  @media (max-width: 768px) {
    font-size: 2.5rem;
  }
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

// 模块卡片区域
.modules-section {
  padding: 40px 24px 80px;
  position: relative;
  z-index: 2;
}

.modules-container {
  max-width: 1000px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 32px;

  @media (max-width: 992px) {
    grid-template-columns: repeat(2, 1fr);
    gap: 24px;
  }

  @media (max-width: 640px) {
    grid-template-columns: 1fr;
    gap: 20px;
  }
}

.module-card {
  background: white;
  border-radius: 24px;
  padding: 48px 32px;
  position: relative;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  min-height: 240px;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, #409eff, #67c23a);
    opacity: 0;
    transition: opacity 0.3s ease;
  }

  &:hover {
    transform: translateY(-8px);
    box-shadow: 0 16px 48px rgba(0, 0, 0, 0.12);

    &::before {
      opacity: 1;
    }

    .card-icon {
      transform: scale(1.05);
    }
  }
}

.provider-card::before {
  background: linear-gradient(90deg, #409eff, #5cacee);
}

.mapping-card::before {
  background: linear-gradient(90deg, #67c23a, #85ce61);
}

.prompt-card::before {
  background: linear-gradient(90deg, #e6a23c, #f0c78a);
}

.prompt-mapping-card::before {
  background: linear-gradient(90deg, #909399, #a6a9ad);
}

.card-icon {
  width: 120px;
  height: 120px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f8fafc, #e2e8f0);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  transition: all 0.3s ease;
  margin-bottom: 24px;
}

.mapping-card .card-icon {
  color: #67c23a;
}

.prompt-card .card-icon {
  color: #e6a23c;
}

.prompt-mapping-card .card-icon {
  color: #909399;
}

.card-title {
  font-size: 1.25rem;
  font-weight: 600;
  color: #1e293b;
  margin: 0;
  line-height: 1.5;

  @media (max-width: 768px) {
    font-size: 1.125rem;
  }
}

// 信息区域
.info-section {
  padding: 0 24px 80px;
  position: relative;
  z-index: 2;
}

.info-container {
  max-width: 1200px;
  margin: 0 auto;
}

.info-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(10px);
  border-radius: 24px;
  padding: 32px 40px;
  display: flex;
  align-items: flex-start;
  gap: 24px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);

  @media (max-width: 768px) {
    flex-direction: column;
    padding: 24px;
  }
}

.info-content {
  flex: 1;

  h3 {
    font-size: 1.5rem;
    font-weight: 600;
    color: #1e293b;
    margin: 0 0 12px 0;
  }

  p {
    font-size: 1rem;
    color: #64748b;
    line-height: 1.7;
    margin: 0;

    strong {
      color: #409eff;
      font-weight: 600;
    }
  }
}
</style>
