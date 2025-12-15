<template>
  <div class="sequence-viewer">
    <div class="viewer-header">
      <div class="title-block">
        <h2>系统时序图</h2>
      </div>
      <div class="header-actions">
        <ElButton text :icon="DocumentCopy" @click="copyMermaid">
          复制 Mermaid 语法
        </ElButton>
        <ElButton
          type="primary"
          :icon="PictureFilled"
          :loading="copyingImage"
          @click="copyDiagramAsImage"
        >
          复制为图片
        </ElButton>
      </div>
    </div>

    <div class="viewer-body">
      <ElAlert
        v-if="renderError"
        type="error"
        :closable="false"
        show-icon
        class="viewer-alert"
        :title="renderError"
      />
      <div ref="diagramContainer" class="diagram-container" />
    </div>

    <div class="mermaid-code">
      <div class="code-header">
        <span>Mermaid 语法</span>
        <ElTag type="info" effect="plain">sequenceDiagram</ElTag>
      </div>
      <pre>{{ diagram }}</pre>
    </div>
  </div>
</template>

<script setup>
/**
 * 时序图渲染器
 * 接收 Mermaid 语法，渲染 SVG 并提供复制功能
 */
import mermaid from 'mermaid';
import { DocumentCopy, PictureFilled } from '@element-plus/icons-vue';

const props = defineProps({
  diagram: {
    type: String,
    required: true,
  },
});

const diagramContainer = ref(null);
const renderError = ref('');
const copyingImage = ref(false);

const initializeMermaid = () => {
  mermaid.initialize({
    startOnLoad: false,
    securityLevel: 'loose',
    theme: 'default',
    fontFamily: '"PingFang SC", "Microsoft YaHei", sans-serif',
  });
};

const renderDiagram = async () => {
  if (!diagramContainer.value) {
    return;
  }
  const source = props.diagram?.trim();
  diagramContainer.value.innerHTML = '';

  if (!source) {
    renderError.value = '暂无可渲染的时序图数据';
    return;
  }

  renderError.value = '';
  const renderId = `seq-${Date.now()}`;

  try {
    const { svg } = await mermaid.render(renderId, source);
    diagramContainer.value.innerHTML = svg;
  } catch (error) {
    console.error('Mermaid 渲染失败', error);
    renderError.value = error?.message || 'Mermaid 渲染失败，请稍后重试';
  }
};

const copyMermaid = async () => {
  const source = props.diagram?.trim() || '';
  if (!source) {
    ElMessage.warning('暂无可复制的 Mermaid 语法');
    return;
  }

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(source);
      ElMessage.success('Mermaid 语法已复制');
      return;
    }

    legacyCopyText(source);
    ElMessage.success('Mermaid 语法已复制');
  } catch (error) {
    ElMessage.error(error?.message || '复制失败，请手动复制');
  }
};

const copyDiagramAsImage = async () => {
  if (copyingImage.value) return;
  const svgElement = diagramContainer.value?.querySelector('svg');
  if (!svgElement) {
    ElMessage.warning('请先等待时序图渲染完成');
    return;
  }

  copyingImage.value = true;

  try {
    const supportError = getClipboardImageSupportError();
    if (supportError) {
      const blob = await convertSvgToPngBlob(svgElement);
      downloadBlob(blob, 'sequence-diagram.png');
      ElMessage.warning(`${supportError}，已改为下载图片`);
      return;
    }

    const blob = await convertSvgToPngBlob(svgElement);
    await navigator.clipboard.write([new ClipboardItem({ 'image/png': blob })]);
    ElMessage.success('时序图图片已复制');
  } catch (error) {
    console.error('复制图片失败', error);
    ElMessage.error(error?.message || '复制图片失败，请稍后重试');
  } finally {
    copyingImage.value = false;
  }
};

const getClipboardImageSupportError = () => {
  if (!window.isSecureContext) {
    return '当前页面不是安全上下文（需要 HTTPS 或 localhost）';
  }
  if (!navigator.clipboard || typeof navigator.clipboard.write !== 'function') {
    return '浏览器不支持复制图片（navigator.clipboard.write 不可用）';
  }
  if (typeof ClipboardItem === 'undefined') {
    return '浏览器不支持复制图片（ClipboardItem 不可用）';
  }
  return '';
};

const legacyCopyText = (text) => {
  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.setAttribute('readonly', 'readonly');
  textarea.style.position = 'fixed';
  textarea.style.left = '-9999px';
  textarea.style.top = '-9999px';
  document.body.appendChild(textarea);
  textarea.select();
  const ok = document.execCommand('copy');
  document.body.removeChild(textarea);
  if (!ok) {
    throw new Error('复制失败，请手动复制');
  }
};

const downloadBlob = (blob, filename) => {
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

const convertSvgToPngBlob = (svgElement) =>
  new Promise((resolve, reject) => {
    const serializer = new XMLSerializer();
    const svgString = serializer.serializeToString(svgElement);
    const svgBlob = new Blob([svgString], {
      type: 'image/svg+xml;charset=utf-8',
    });
    const url = URL.createObjectURL(svgBlob);
    const image = new Image();
    image.onload = () => {
      const scale = window.devicePixelRatio || 2;
      const viewBox = svgElement.viewBox?.baseVal;
      const width = (viewBox?.width || svgElement.clientWidth || 1200) * scale;
      const height =
        (viewBox?.height || svgElement.clientHeight || 600) * scale;
      const canvas = document.createElement('canvas');
      canvas.width = width;
      canvas.height = height;
      const ctx = canvas.getContext('2d');
      if (!ctx) {
        URL.revokeObjectURL(url);
        reject(new Error('Canvas 初始化失败'));
        return;
      }
      ctx.fillStyle = '#ffffff';
      ctx.fillRect(0, 0, width, height);
      ctx.drawImage(image, 0, 0, width, height);
      canvas.toBlob(
        (blob) => {
          if (!blob) {
            reject(new Error('转换图片失败'));
            return;
          }
          resolve(blob);
        },
        'image/png',
        1,
      );
      URL.revokeObjectURL(url);
    };
    image.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('无法渲染 SVG，请刷新后重试'));
    };
    image.src = url;
  });

watch(
  () => props.diagram,
  () => {
    renderDiagram();
  },
  { immediate: true },
);

onMounted(() => {
  initializeMermaid();
  renderDiagram();
});
</script>

<style scoped lang="scss">
.sequence-viewer {
  background: $background-primary;
  border-radius: $border-radius-2xl;
  border: 1px solid $border-light;
  box-shadow: $shadow-lg;
  padding: $spacing-2xl $spacing-3xl;
  display: flex;
  flex-direction: column;
  gap: $spacing-xl;
}

.viewer-header {
  display: flex;
  justify-content: space-between;
  gap: $spacing-lg;
  flex-wrap: wrap;

  h2 {
    margin: 0;
    font-size: $font-size-2xl;
    font-weight: $font-weight-semibold;
  }

  p {
    margin: $spacing-xs 0 0;
    color: $secondary-color;
    font-size: $font-size-sm;
  }

  .header-actions {
    display: flex;
    gap: $spacing-sm;
    align-items: center;
  }
}

.viewer-body {
  min-height: 360px;
  border: 1px dashed rgba(37, 99, 235, 0.3);
  border-radius: $border-radius-xl;
  padding: $spacing-2xl;
  background: rgba(248, 250, 252, 0.8);
  position: relative;
}

.viewer-alert {
  margin-bottom: $spacing-md;
}

.diagram-container {
  width: 100%;
  overflow: auto;

  svg {
    width: 100%;
    height: auto;
  }
}

.mermaid-code {
  background: #0f172a;
  color: #e2e8f0;
  border-radius: $border-radius-xl;
  padding: $spacing-lg;
  font-family: 'Fira Code', 'JetBrains Mono', monospace;

  .code-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: $spacing-sm;
    font-weight: $font-weight-medium;
  }

  pre {
    margin: 0;
    white-space: pre-wrap;
    word-break: break-word;
    font-size: 0.875rem;
    line-height: 1.6;
  }
}

@media (max-width: $breakpoint-md) {
  .viewer-header {
    flex-direction: column;
    align-items: flex-start;

    .header-actions {
      flex-wrap: wrap;
      width: 100%;
    }
  }
}
</style>
