<template>
  <ElPopover
    trigger="hover"
    :width="width"
    :placement="placement"
    :offset="offset"
    :show-arrow="showArrow"
    effect="light"
    :popper-class="popperClassName"
  >
    <template #reference>
      <span class="description-anchor">
        <slot name="reference" :preview="previewText">
          <span
            class="description-anchor__text"
            :style="{ '--line-clamp': clampLines }"
          >
            {{ previewText }}
          </span>
        </slot>
      </span>
    </template>
    <div class="description-panel" :class="{ 'is-empty': !hasText }">
      <slot v-if="hasText" name="content" :text="normalizedText">
        <p>{{ normalizedText }}</p>
      </slot>
      <p v-else>暂无描述</p>
    </div>
  </ElPopover>
</template>

<script setup>
import { computed } from 'vue';

const props = defineProps({
  text: {
    type: String,
    default: '',
  },
  width: {
    type: Number,
    default: 360,
  },
  placement: {
    type: String,
    default: 'top-start',
  },
  maxPreviewLines: {
    type: Number,
    default: 2,
  },
  popperClass: {
    type: String,
    default: '',
  },
  offset: {
    type: Number,
    default: 6,
  },
  showArrow: {
    type: Boolean,
    default: true,
  },
});

const normalizedText = computed(() => props.text?.trim() ?? '');
const hasText = computed(() => Boolean(normalizedText.value));
const previewText = computed(() =>
  hasText.value ? normalizedText.value : '暂无描述',
);
const clampLines = computed(() =>
  props.maxPreviewLines > 0 ? props.maxPreviewLines : 2,
);
const popperClassName = computed(() =>
  ['task-description-popper', props.popperClass]
    .filter(Boolean)
    .join(' ')
    .trim(),
);
</script>

<style scoped lang="scss">
.description-anchor {
  display: block;
  cursor: pointer;
  color: #0f172a;
}

.description-anchor__text {
  display: -webkit-box;
  -webkit-line-clamp: var(--line-clamp, 2);
  -webkit-box-orient: vertical;
  word-break: break-word;
  overflow: hidden;
  font-size: 13px;
  line-height: 20px;
  color: inherit;
}

:deep(.task-description-popper) {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.15);
  padding: 14px 16px;
  color: #0f172a;
}

.description-panel {
  font-size: 14px;
  line-height: 1.6;
  max-height: 220px;
  overflow-y: auto;
  word-break: break-word;
}

.description-panel.is-empty {
  color: #94a3b8;
  text-align: center;
}

.description-panel::-webkit-scrollbar {
  width: 4px;
}

.description-panel::-webkit-scrollbar-thumb {
  background: rgba(148, 163, 184, 0.6);
  border-radius: 999px;
}
</style>
