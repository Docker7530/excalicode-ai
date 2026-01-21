<template>
  <ElConfigProvider :locale="zhCn">
    <AppHeader
      v-if="showHeader"
      :show-home-button="showHomeButton"
      show-github-button
      :menu-items="menuItems"
      :active-menu-id="activeMenuId"
    />

    <RouterView />
  </ElConfigProvider>
</template>

<script setup>
import { computed, provide, ref } from 'vue';
import zhCn from 'element-plus/es/locale/lang/zh-cn';
import AppHeader from '@/components/AppHeader.vue';
import { MENU_BAR_KEY } from '@/composables/useMenuBar';

const route = useRoute();

const menuItems = ref([]);
const activeMenuId = ref('');

const setMenuItems = (items = []) => {
  menuItems.value = Array.isArray(items) ? items : [];
};

const setActiveMenuId = (id = '') => {
  activeMenuId.value = id || '';
};

const clearMenu = () => {
  menuItems.value = [];
  activeMenuId.value = '';
};

provide(MENU_BAR_KEY, {
  setMenuItems,
  setActiveMenuId,
  clear: clearMenu,
});

const showHeader = computed(() => route.path !== '/login');
const showHomeButton = computed(() => route.path !== '/');
</script>
