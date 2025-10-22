<!--
  AI 智能分析等待动画组件 (v3.2 - 白色主题最终版)
  设计理念：智能构想的几何草图，提供明亮、活泼、带互动的加载体验。
  更新：根据要求，移除了主标题的动画效果，使其保持静态。
-->
<template>
  <div v-if="active" class="loading-container-light">
    <!-- 主动画：构想画布 -->
    <div class="idea-canvas">
      <!-- 三个核心构想节点 -->
      <div class="node node-1"></div>
      <div class="node node-2"></div>
      <div class="node node-3"></div>

      <!-- 连接节点的动态线条 -->
      <svg class="connections" viewBox="0 0 200 200">
        <line class="line line-1" x1="50" y1="50" x2="150" y2="50" />
        <line class="line line-2" x1="150" y1="50" x2="100" y2="150" />
        <line class="line line-3" x1="100" y1="150" x2="50" y2="50" />
      </svg>
    </div>

    <!-- 静态标题 -->
    <div class="loading-title">正在翻阅我的“武林秘籍”...</div>

    <!-- 提示信息 (保留细微动画) -->
    <div class="loading-subtitle">保证给您一套拳拳到肉的绝妙方案！</div>
  </div>
</template>

<script>
/**
 * AI 智能分析等待动画组件 (白色主题版)
 * v3.2: 最终版本，主标题为静态文字。
 */
export default {
  name: 'AiLoadingAnimationV3',
  props: {
    /**
     * 是否激活动画
     */
    active: {
      type: Boolean,
      default: true,
    },
  },
};
</script>

<style lang="scss" scoped>
// 设计变量 (亮色主题)
$bg-color: #f7f9fc; // 非常浅的灰白色背景
$primary-color: #007aff; // 活力的主色调 (苹果蓝)
$secondary-color: #ff9500; // 辅助色1 (橙色)
$tertiary-color: #34c759; // 辅助色2 (绿色)
$text-primary: #1d1d1f; // 深灰色主文字
$text-secondary: #86868b; // 次要文字

.loading-container-light {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 48px;
  text-align: center;
  background-color: $bg-color;
  border-radius: 20px;
  position: relative;
  overflow: hidden;
  border: 1px solid #e5e5ea;
  transition: transform 0.3s ease;

  // 鼠标悬停时的互动效果
  &:hover {
    .node {
      animation-duration: 0.8s;
    }
    .line {
      animation-duration: 1.2s;
    }
  }
}

// 动画画布
.idea-canvas {
  position: relative;
  width: 200px;
  height: 200px;
  margin-bottom: 24px;
}

// 构想节点
.node {
  position: absolute;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  transform-origin: center;

  // 涟漪效果
  &::after {
    content: '';
    position: absolute;
    inset: -4px;
    border-radius: 50%;
    border: 2px solid currentColor;
    opacity: 0;
    animation: ripple 2s cubic-bezier(0.25, 0.46, 0.45, 0.94) infinite;
  }

  &.node-1 {
    top: calc(50% - 50px);
    left: calc(50% - 50px);
    background-color: $primary-color;
    box-shadow: 0 0 12px rgba($primary-color, 0.5);
    color: $primary-color;
    animation: bounce-in 1.5s cubic-bezier(0.68, -0.55, 0.27, 1.55) infinite;
    &::after {
      animation-delay: 0s;
    }
  }

  &.node-2 {
    top: calc(50% - 50px);
    left: calc(50% + 50px);
    background-color: $secondary-color;
    box-shadow: 0 0 12px rgba($secondary-color, 0.5);
    color: $secondary-color;
    animation: bounce-in 1.5s cubic-bezier(0.68, -0.55, 0.27, 1.55) infinite
      0.2s;
    &::after {
      animation-delay: 0.2s;
    }
  }

  &.node-3 {
    top: calc(50% + 50px);
    left: 50%;
    background-color: $tertiary-color;
    box-shadow: 0 0 12px rgba($tertiary-color, 0.5);
    color: $tertiary-color;
    animation: bounce-in 1.5s cubic-bezier(0.68, -0.55, 0.27, 1.55) infinite
      0.4s;
    &::after {
      animation-delay: 0.4s;
    }
  }
}

// 节点入场/跳动动画
@keyframes bounce-in {
  0%,
  100% {
    transform: translate(-50%, -50%) scale(0.8);
  }
  50% {
    transform: translate(-50%, -50%) scale(1.2);
  }
}

// 涟漪动画
@keyframes ripple {
  0% {
    transform: scale(0.8);
    opacity: 1;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

// SVG 连接线容器
.connections {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  transform: translateZ(0);
}

// SVG 线条样式
.line {
  stroke-width: 2;
  stroke: $primary-color;
  stroke-linecap: round;
  stroke-dasharray: 150;
  stroke-dashoffset: 150;
  animation: draw-line 2s cubic-bezier(0.65, 0, 0.35, 1) infinite;

  &.line-2 {
    animation-delay: 0.2s;
    stroke: $secondary-color;
  }
  &.line-3 {
    animation-delay: 0.4s;
    stroke: $tertiary-color;
  }
}

// 线条绘制动画
@keyframes draw-line {
  0% {
    stroke-dashoffset: 150;
  }
  40% {
    stroke-dashoffset: 0;
  }
  80% {
    stroke-dashoffset: -150;
  }
  100% {
    stroke-dashoffset: -150;
  }
}

/* --- 文字区域 --- */

// 标题 (静态，无动画)
.loading-title {
  font-size: 24px;
  font-weight: 600;
  color: $text-primary;
  margin-bottom: 8px;
}

// 副标题 (保留细微的淡入淡出动画)
.loading-subtitle {
  font-size: 14px;
  color: $text-secondary;
  animation: text-fade-in-out 3s ease-in-out infinite 0.2s;
}

// 副标题的动画 (透明度变化)
@keyframes text-fade-in-out {
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0.75;
  }
}

// 响应式设计
@media (max-width: 768px) {
  .loading-container-light {
    min-height: 320px;
    padding: 32px;
  }
  .idea-canvas {
    transform: scale(0.8);
  }
  .loading-title {
    font-size: 20px;
  }
}
</style>
