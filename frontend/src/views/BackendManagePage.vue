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
          <p class="hero-subtitle">
            管理 AI 模型配置，实现智能化的功能级别模型切换
          </p>
        </div>
      </div>
    </div>

    <!-- 管理模块卡片 -->
    <div class="modules-section">
      <div class="modules-container">
        <div class="module-card provider-card" @click="navigateToProvider">
          <div class="card-glow"></div>
          <div class="card-content">
            <div class="card-icon-wrapper">
              <div class="card-icon">
                <ElIcon :size="64"><OfficeBuilding /></ElIcon>
              </div>
              <div class="icon-badge">厂商管理</div>
            </div>
            <h2 class="card-title">AI 模型厂商管理</h2>
            <p class="card-description">
              配置和管理所有 AI 厂商信息，包括 API 地址、密钥等核心配置
            </p>
            <ul class="card-features">
              <li>
                <ElIcon><Check /></ElIcon>
                <span>多厂商统一管理</span>
              </li>
              <li>
                <ElIcon><Check /></ElIcon>
                <span>API 密钥安全存储</span>
              </li>
              <li>
                <ElIcon><Check /></ElIcon>
                <span>模型快速配置</span>
              </li>
            </ul>
            <div class="card-action">
              <ElButton type="primary" size="large" round class="action-btn">
                <span>进入管理</span>
                <ElIcon class="btn-icon"><ArrowRight /></ElIcon>
              </ElButton>
            </div>
          </div>
          <div class="card-decoration">
            <div class="decoration-circle"></div>
            <div class="decoration-circle"></div>
            <div class="decoration-circle"></div>
          </div>
        </div>

        <div class="module-card mapping-card" @click="navigateToMapping">
          <div class="card-glow"></div>
          <div class="card-content">
            <div class="card-icon-wrapper">
              <div class="card-icon">
                <ElIcon :size="64"><Connection /></ElIcon>
              </div>
              <div class="icon-badge">映射管理</div>
            </div>
            <h2 class="card-title">功能-模型映射管理</h2>
            <p class="card-description">
              为每个功能独立配置 AI 模型，实现智能化的模型分配和切换
            </p>
            <ul class="card-features">
              <li>
                <ElIcon><Check /></ElIcon>
                <span>功能级别模型配置</span>
              </li>
              <li>
                <ElIcon><Check /></ElIcon>
                <span>智能故障降级</span>
              </li>
              <li>
                <ElIcon><Check /></ElIcon>
                <span>实时配置生效</span>
              </li>
            </ul>
            <div class="card-action">
              <ElButton type="success" size="large" round class="action-btn">
                <span>进入管理</span>
                <ElIcon class="btn-icon"><ArrowRight /></ElIcon>
              </ElButton>
            </div>
          </div>
          <div class="card-decoration">
            <div class="decoration-circle"></div>
            <div class="decoration-circle"></div>
            <div class="decoration-circle"></div>
          </div>
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
              先在 <strong>AI 模型厂商管理</strong> 中配置厂商和模型，然后在
              <strong>功能-模型映射管理</strong>
              中为每个功能指定使用的模型。未配置的功能将自动使用默认模型。
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
  ArrowRight,
  Check,
  Connection,
  InfoFilled,
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

.hero-subtitle {
  font-size: 1.25rem;
  color: rgba(255, 255, 255, 0.9);
  margin: 0;
  font-weight: 400;

  @media (max-width: 768px) {
    font-size: 1rem;
  }
}

// 模块卡片区域
.modules-section {
  padding: 40px 24px 80px;
  position: relative;
  z-index: 2;
}

.modules-container {
  max-width: 1200px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 40px;

  @media (max-width: 768px) {
    grid-template-columns: 1fr;
    gap: 24px;
  }
}

.module-card {
  background: white;
  border-radius: 32px;
  padding: 48px 40px;
  position: relative;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 6px;
    background: linear-gradient(90deg, #409eff, #67c23a);
    transform: scaleX(0);
    transform-origin: left;
    transition: transform 0.4s ease;
  }

  &:hover {
    transform: translateY(-12px);
    box-shadow: 0 30px 80px rgba(0, 0, 0, 0.2);

    &::before {
      transform: scaleX(1);
    }

    .card-glow {
      opacity: 1;
    }

    .card-icon {
      transform: scale(1.1) rotate(5deg);
    }

    .decoration-circle {
      animation: float 3s ease-in-out infinite;
    }

    .action-btn {
      transform: translateX(8px);
    }
  }
}

.provider-card::before {
  background: linear-gradient(90deg, #409eff, #5cacee);
}

.mapping-card::before {
  background: linear-gradient(90deg, #67c23a, #85ce61);
}

.card-glow {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(
    circle,
    rgba(64, 158, 255, 0.1) 0%,
    transparent 70%
  );
  opacity: 0;
  transition: opacity 0.4s ease;
  pointer-events: none;
}

.mapping-card .card-glow {
  background: radial-gradient(
    circle,
    rgba(103, 194, 58, 0.1) 0%,
    transparent 70%
  );
}

.card-content {
  position: relative;
  z-index: 2;
}

.card-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}

.card-icon {
  width: 100px;
  height: 100px;
  border-radius: 24px;
  background: linear-gradient(135deg, #f8fafc, #e2e8f0);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409eff;
  transition: all 0.3s ease;
}

.mapping-card .card-icon {
  color: #67c23a;
}

.icon-badge {
  background: linear-gradient(135deg, #409eff, #5cacee);
  color: white;
  padding: 8px 20px;
  border-radius: 24px;
  font-size: 0.875rem;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.mapping-card .icon-badge {
  background: linear-gradient(135deg, #67c23a, #85ce61);
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

.card-title {
  font-size: 2rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 16px 0;

  @media (max-width: 768px) {
    font-size: 1.5rem;
  }
}

.card-description {
  font-size: 1rem;
  color: #64748b;
  line-height: 1.6;
  margin: 0 0 32px 0;
}

.card-features {
  list-style: none;
  padding: 0;
  margin: 0 0 32px 0;

  li {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 12px 0;
    font-size: 0.95rem;
    color: #475569;

    .el-icon {
      color: #67c23a;
      font-size: 18px;
      flex-shrink: 0;
    }
  }
}

.card-action {
  margin-top: auto;
}

.action-btn {
  width: 100%;
  height: 56px;
  font-size: 1.125rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.3s ease;

  .btn-icon {
    transition: transform 0.3s ease;
  }
}

.card-decoration {
  position: absolute;
  bottom: 20px;
  right: 20px;
  display: flex;
  gap: 8px;
  opacity: 0.1;
  pointer-events: none;
}

.decoration-circle {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff, #67c23a);
}

@keyframes float {
  0%,
  100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-8px);
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
