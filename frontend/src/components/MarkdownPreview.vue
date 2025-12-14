<template>
  <div ref="containerRef" class="markdown-preview"></div>
</template>

<script setup>
import Vditor from 'vditor';
import 'vditor/dist/index.css';
import { nextTick, onMounted, ref, watch } from 'vue';

const props = defineProps({
  content: {
    type: String,
    default: '',
  },
});

const containerRef = ref(null);

const render = async () => {
  await nextTick();
  if (!containerRef.value) return;

  const markdown = props.content || '';
  Vditor.preview(containerRef.value, markdown, {
    mode: 'light',
  });
};

onMounted(() => {
  render();
});

watch(
  () => props.content,
  () => {
    render();
  },
);
</script>

<style scoped lang="scss">
.markdown-preview {
  :deep(p) {
    margin: 0.55em 0;
  }

  :deep(ul),
  :deep(ol) {
    margin: 0.55em 0;
    padding-left: 1.3em;
  }

  :deep(a) {
    color: #2563eb;
    text-decoration: none;
  }

  :deep(a:hover) {
    text-decoration: underline;
  }
}
</style>
