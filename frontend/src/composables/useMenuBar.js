import { inject, onBeforeUnmount } from 'vue';

export const MENU_BAR_KEY = Symbol('MENU_BAR');

export function useMenuBar() {
  const api = inject(MENU_BAR_KEY, null);
  if (!api) {
    throw new Error('useMenuBar must be used within a MenuBar provider');
  }

  // 页面卸载时默认清空，避免切路由残留
  onBeforeUnmount(() => {
    api.clear();
  });

  return api;
}
