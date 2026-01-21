<template>
  <header class="app-header" role="banner">
    <div class="header-inner">
      <div class="left">
        <button
          v-if="showHomeButton"
          class="brand"
          type="button"
          :disabled="isHome"
          :aria-disabled="isHome"
          @click="goHome"
        >
          <span class="brand-mark" aria-hidden="true">EX</span>
          <span class="brand-name">EXCALICODE AI</span>

          <span v-if="!isHome" class="back-chip">
            <ElIcon class="back-chip-icon"><House /></ElIcon>
            <span class="back-chip-text">首页</span>
          </span>
        </button>

        <div v-else class="brand brand--static" aria-label="EXCALICODE AI">
          <span class="brand-mark" aria-hidden="true">EX</span>
          <span class="brand-name">EXCALICODE AI</span>
        </div>
      </div>

      <div class="right" aria-label="Header actions">
        <a
          v-if="showGithubButton && githubRepoUrl"
          class="icon-btn"
          :href="githubRepoUrl"
          target="_blank"
          rel="noopener noreferrer"
          aria-label="打开 GitHub 仓库"
          title="GitHub"
        >
          <svg
            class="github-icon"
            viewBox="0 0 24 24"
            width="20"
            height="20"
            aria-hidden="true"
            focusable="false"
          >
            <path
              fill="currentColor"
              d="M12 .5C5.7.5.6 5.6.6 11.9c0 5 3.3 9.3 7.9 10.8.6.1.8-.3.8-.6v-2c-3.2.7-3.9-1.4-3.9-1.4-.5-1.2-1.2-1.5-1.2-1.5-1-.7.1-.7.1-.7 1.1.1 1.7 1.1 1.7 1.1 1 .1 1.5.7 1.8 1.2.2.5.8.7 1.3.5.1-.7.4-1.2.7-1.4-2.6-.3-5.3-1.3-5.3-5.8 0-1.3.5-2.4 1.2-3.3-.1-.3-.5-1.5.1-3.1 0 0 1-.3 3.3 1.3.9-.3 1.9-.4 2.9-.4s2 .1 2.9.4C17.1 5 18.1 5.3 18.1 5.3c.6 1.6.2 2.8.1 3.1.8.9 1.2 2 1.2 3.3 0 4.5-2.7 5.5-5.3 5.8.4.3.8 1 .8 2v3c0 .3.2.7.8.6 4.6-1.5 7.9-5.8 7.9-10.8C23.4 5.6 18.3.5 12 .5z"
            />
          </svg>
        </a>

        <button
          class="user-action"
          type="button"
          :aria-label="isLoggedIn ? '退出登录' : '去登录'"
          @click="handleUserAction"
        >
          <span class="user-action__default" :title="displayName">
            {{ displayName }}
          </span>
          <span class="user-action__hover">{{ hoverLabel }}</span>
        </button>
      </div>
    </div>
  </header>
</template>

<script setup>
import { House } from '@element-plus/icons-vue';
import { PROJECT_LINKS } from '@/constants';

const props = defineProps({
  showHomeButton: {
    type: Boolean,
    default: true,
  },
  showGithubButton: {
    type: Boolean,
    default: true,
  },
});

const { showHomeButton, showGithubButton } = toRefs(props);

const router = useRouter();
const route = useRoute();

const username = ref('');
const githubRepoUrl = PROJECT_LINKS.GITHUB_REPO;

const refreshUserInfo = () => {
  username.value = localStorage.getItem('username') || '';
};

onMounted(() => {
  refreshUserInfo();
  window.addEventListener('storage', refreshUserInfo);
});

onBeforeUnmount(() => {
  window.removeEventListener('storage', refreshUserInfo);
});

const isHome = computed(() => route.path === '/');

const isLoggedIn = computed(() => Boolean(localStorage.getItem('token')));

const displayName = computed(() => username.value || '登录');

const hoverLabel = computed(() => (isLoggedIn.value ? '退出' : '登录'));

const goHome = () => {
  if (isHome.value) return;
  router.push('/');
};

const handleUserAction = () => {
  if (!isLoggedIn.value) {
    router.push('/login');
    return;
  }

  localStorage.removeItem('token');
  localStorage.removeItem('username');
  localStorage.removeItem('role');

  refreshUserInfo();
  ElMessage.success('已退出登录');

  router.push('/login');
};
</script>

<style scoped lang="scss">
.app-header {
  --hdr-h: 64px;
  --hdr-max: 1240px;
  --hdr-pad-x: 22px;

  position: fixed;
  top: 16px;
  left: 16px;
  right: 16px;
  z-index: 1200;
  pointer-events: none;

  @media (max-width: 768px) {
    top: 12px;
    left: 12px;
    right: 12px;
  }
}

.header-inner {
  pointer-events: auto;
  position: relative;
  height: var(--hdr-h);
  display: flex;
  align-items: center;
  justify-content: space-between;
  max-width: var(--hdr-max);
  margin: 0 auto;
  padding: 0 var(--hdr-pad-x);

  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.1);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.1);
  backdrop-filter: blur(18px) saturate(1.4);
  overflow: hidden;

  @supports not (backdrop-filter: blur(1px)) {
    background: rgba(255, 255, 255, 0.95);
  }

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    pointer-events: none;
    background:
      radial-gradient(
        620px 180px at 20% 0%,
        rgba(56, 189, 248, 0.22),
        transparent 60%
      ),
      radial-gradient(
        540px 180px at 80% 0%,
        rgba(34, 197, 94, 0.14),
        transparent 55%
      );
    opacity: 0.9;
  }
}

.left {
  display: flex;
  align-items: center;
  min-width: 0;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: none;
  background: transparent;
  padding: 8px 10px;
  border-radius: 14px;
  cursor: pointer;
  color: #0f172a;
  font-family:
    ui-sans-serif,
    system-ui,
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    'PingFang SC',
    'Microsoft YaHei',
    sans-serif;
  transition:
    transform 0.16s ease,
    background-color 0.16s ease,
    box-shadow 0.16s ease;

  &:hover {
    background: rgba(255, 255, 255, 0.65);
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
    transform: translateY(-1px);
  }

  &:active {
    transform: translateY(0);
  }

  &:disabled {
    cursor: default;
    box-shadow: none;
    transform: none;
    background: transparent;
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.22);
  }
}

.brand--static {
  cursor: default;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-weight: 800;
  letter-spacing: -0.04em;
  color: rgba(255, 255, 255, 0.96);
  background: linear-gradient(135deg, #0ea5e9, #22c55e);
  box-shadow:
    0 10px 18px rgba(14, 165, 233, 0.18),
    0 10px 18px rgba(34, 197, 94, 0.12);
}

.brand-name {
  font-weight: 750;
  letter-spacing: -0.02em;
  font-size: 0.98rem;
  white-space: nowrap;
}

.back-chip {
  margin-left: 8px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.72);
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.06);
  font-weight: 650;
  font-size: 0.88rem;
  color: #0f172a;
}

.back-chip-icon {
  color: #0ea5e9;
}

.right {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: rgba(15, 23, 42, 0.86);
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: rgba(255, 255, 255, 0.56);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.08);
  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease,
    border-color 0.16s ease,
    background-color 0.16s ease;

  &:hover {
    transform: translateY(-1px);
    background: rgba(255, 255, 255, 0.76);
    box-shadow: 0 18px 34px rgba(15, 23, 42, 0.12);
    border-color: rgba(14, 165, 233, 0.24);
  }

  &:active {
    transform: translateY(0);
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.2);
  }
}

.github-icon {
  display: block;
}

.user-action {
  height: 40px;
  padding: 0 14px;
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.1);
  background: rgba(255, 255, 255, 0.56);
  box-shadow: 0 12px 26px rgba(15, 23, 42, 0.08);
  cursor: pointer;
  position: relative;
  overflow: hidden;

  display: inline-flex;
  align-items: center;
  justify-content: center;

  transition:
    transform 0.16s ease,
    box-shadow 0.16s ease,
    border-color 0.16s ease,
    background-color 0.16s ease;

  &:hover {
    transform: translateY(-1px);
    background: rgba(255, 255, 255, 0.76);
    box-shadow: 0 18px 34px rgba(15, 23, 42, 0.12);
    border-color: rgba(244, 63, 94, 0.22);
  }

  &:active {
    transform: translateY(0);
  }

  &:focus-visible {
    outline: none;
    box-shadow: 0 0 0 3px rgba(14, 165, 233, 0.18);
  }
}

.user-action__default,
.user-action__hover {
  display: inline-flex;
  align-items: center;
  font-weight: 700;
  font-size: 0.95rem;
  letter-spacing: -0.01em;
  transition:
    opacity 0.14s ease,
    transform 0.14s ease;
}

.user-action__default {
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: rgba(15, 23, 42, 0.92);
}

.user-action__hover {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(190, 18, 60, 0.95);
  opacity: 0;
  transform: translateY(6px);
}

.user-action:hover .user-action__default {
  opacity: 0;
  transform: translateY(-6px);
}

.user-action:hover .user-action__hover {
  opacity: 1;
  transform: translateY(0);
}

@media (max-width: 768px) {
  .app-header {
    --hdr-pad-x: 14px;
  }

  .brand-name {
    display: none;
  }

  .back-chip {
    margin-left: 6px;
  }

  .user-name {
    max-width: 120px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .brand,
  .user-action,
  .icon-btn {
    transition: none;
  }
}
</style>
