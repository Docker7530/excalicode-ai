<template>
  <div class="app-header">
    <div
      ref="userPanelRef"
      class="user-panel"
      :class="{
        'with-home': showHomeButton,
        'home-visible': showHomeButton && isHomeExpanded,
      }"
      @mouseenter="handlePanelMouseEnter"
      @mouseleave="handlePanelMouseLeave"
      @focusin="handlePanelFocusIn"
      @focusout="handlePanelFocusOut"
    >
      <button
        v-if="showHomeButton"
        class="home-pill"
        type="button"
        @click="goHome"
      >
        <ElIcon class="pill-icon">
          <House />
        </ElIcon>
        <span class="pill-text">返回首页</span>
      </button>

      <ElDropdown trigger="click" @command="handleUserCommand">
        <div class="user-trigger">
          <ElIcon class="user-icon">
            <User />
          </ElIcon>
          <div class="user-info">
            <span class="username">{{ username }}</span>
            <span class="role">{{ roleLabel }}</span>
          </div>
          <ElIcon class="dropdown-indicator">
            <ArrowDown />
          </ElIcon>
        </div>
        <template #dropdown>
          <ElDropdownMenu>
            <ElDropdownItem v-if="isAdmin" command="user-manage">
              <ElIcon class="dropdown-item-icon">
                <Setting />
              </ElIcon>
              <span>人员管理</span>
            </ElDropdownItem>
            <ElDropdownItem divided command="logout">
              <ElIcon class="dropdown-item-icon">
                <SwitchButton />
              </ElIcon>
              <span>退出登录</span>
            </ElDropdownItem>
          </ElDropdownMenu>
        </template>
      </ElDropdown>
    </div>
  </div>
</template>

<script setup>
import {
  ArrowDown,
  House,
  Setting,
  SwitchButton,
  User,
} from '@element-plus/icons-vue';

const props = defineProps({
  showHomeButton: {
    type: Boolean,
    default: true,
  },
});

const { showHomeButton } = toRefs(props);

const router = useRouter();
const username = ref('');
const role = ref('');
const isHomeExpanded = ref(false);
const userPanelRef = ref(null);
const isAdmin = computed(() => role.value === 'ADMIN');

let introOpenTimer = null;
let introCloseTimer = null;

const refreshUserInfo = () => {
  username.value = localStorage.getItem('username') || '未登录';
  role.value = localStorage.getItem('role') || 'USER';
};

const clearIntroTimers = () => {
  if (introOpenTimer) {
    window.clearTimeout(introOpenTimer);
    introOpenTimer = null;
  }
  if (introCloseTimer) {
    window.clearTimeout(introCloseTimer);
    introCloseTimer = null;
  }
};

const expandHome = () => {
  if (!showHomeButton.value) return;
  clearIntroTimers();
  isHomeExpanded.value = true;
};

const collapseHome = (force = false) => {
  if (!showHomeButton.value) return;
  if (!force) {
    const activeElement = document.activeElement;
    if (activeElement && userPanelRef.value?.contains(activeElement)) {
      return;
    }
  }
  isHomeExpanded.value = false;
};

const handlePanelMouseEnter = () => {
  expandHome();
};

const handlePanelMouseLeave = () => {
  collapseHome();
};

const handlePanelFocusIn = () => {
  expandHome();
};

const handlePanelFocusOut = (event) => {
  if (
    !showHomeButton.value ||
    !userPanelRef.value ||
    userPanelRef.value.contains(event?.relatedTarget)
  ) {
    return;
  }
  collapseHome();
};

const scheduleIntroPeek = () => {
  if (!showHomeButton.value) return;
  clearIntroTimers();
  introOpenTimer = window.setTimeout(() => {
    expandHome();
    introCloseTimer = window.setTimeout(() => {
      collapseHome(true);
      introCloseTimer = null;
    }, 1600);
  }, 300);
};

onMounted(() => {
  refreshUserInfo();
  window.addEventListener('storage', refreshUserInfo);
  scheduleIntroPeek();
});

onBeforeUnmount(() => {
  window.removeEventListener('storage', refreshUserInfo);
  clearIntroTimers();
});

const roleLabel = computed(() =>
  role.value === 'ADMIN' ? '管理员' : '普通用户',
);

const goHome = () => {
  router.push('/');
};

const handleUserCommand = async (command) => {
  if (command === 'user-manage') {
    collapseHome(true);
    router.push('/user-manage');
    return;
  }

  if (command !== 'logout') return;

  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    });

    localStorage.removeItem('token');
    localStorage.removeItem('username');
    localStorage.removeItem('role');

    refreshUserInfo();
    ElMessage.success('已退出登录');

    router.push('/login');
  } catch {
    // 用户取消
  } finally {
    collapseHome(true);
  }
};
</script>

<style scoped lang="scss">
.app-header {
  position: fixed;
  top: 24px;
  left: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  pointer-events: none;
  z-index: 1200;

  @media (max-width: 768px) {
    top: 16px;
    left: 16px;
    right: 16px;
  }
}

.user-panel {
  pointer-events: auto;
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
  position: relative;
}

.user-panel.with-home {
  gap: 0;
}

.home-pill {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.95);
  border: 1px solid rgba(99, 102, 241, 0.25);
  color: #1e293b;
  font-weight: 600;
  font-size: 0.95rem;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
  cursor: pointer;
  transition:
    max-width 0.28s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.2s ease,
    transform 0.28s cubic-bezier(0.4, 0, 0.2, 1),
    margin-right 0.28s cubic-bezier(0.4, 0, 0.2, 1),
    box-shadow 0.28s ease;
  max-width: 0;
  opacity: 0;
  margin-right: 0;
  pointer-events: none;
  overflow: hidden;
  transform: translateX(12px);
  white-space: nowrap;
}

.home-pill:focus-visible {
  outline: none;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.25);
}

.user-panel.home-visible .home-pill {
  max-width: 160px;
  opacity: 1;
  margin-right: 12px;
  pointer-events: auto;
  transform: translateX(0);
}

.pill-icon {
  color: #6366f1;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  padding: 10px 18px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.94);
  border: 1px solid rgba(99, 102, 241, 0.2);
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.1);
  cursor: pointer;
  transition:
    transform 0.2s ease,
    box-shadow 0.2s ease,
    border-color 0.2s ease;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 12px 24px rgba(15, 23, 42, 0.14);
    border-color: rgba(99, 102, 241, 0.35);
  }
}

.user-icon {
  color: #6366f1;
}

.user-info {
  display: flex;
  flex-direction: column;
  line-height: 1.1;
}

.username {
  font-weight: 600;
  font-size: 0.95rem;
  color: #1e293b;
}

.role {
  font-size: 0.75rem;
  color: #64748b;
}

.dropdown-indicator {
  color: #94a3b8;
}

.dropdown-item-icon {
  margin-right: 6px;
}

@media (max-width: 576px) {
  .home-pill,
  .user-trigger {
    padding: 8px 14px;
    gap: 8px;
  }

  .user-info {
    display: none;
  }

  .home-pill {
    max-width: 40px;
    justify-content: center;
  }
}

@media (prefers-reduced-motion: reduce) {
  .home-pill {
    transition: none;
  }
}
</style>
