import App from '@/App.vue';
import router from '@/router';
import '@/styles/main.scss';
import * as ElementPlusIconsVue from '@element-plus/icons-vue';
import 'element-plus/theme-chalk/src/loading.scss';
import 'element-plus/theme-chalk/src/message-box.scss';
import 'element-plus/theme-chalk/src/message.scss';
import 'element-plus/theme-chalk/src/notification.scss';

const plugins = [router];

/**
 * 全局注册 Element Plus 图标，保持组件可直接按名称使用
 * @param {import("vue").App} appInstance Vue 应用实例
 */
const registerElementPlusIcons = (appInstance) => {
  Object.entries(ElementPlusIconsVue).forEach(([name, component]) => {
    appInstance.component(name, component);
  });
};

/**
 * 当 Vue 启动失败时兜底渲染用户提示，避免白屏
 * @param {string} message 需要展示的错误提示
 */
const mountFallback = (message) => {
  document.body.innerHTML = `
    <div style="display:flex;align-items:center;justify-content:center;min-height:100vh;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#f5f6fa;color:#2c3e50;text-align:center;padding:24px;">
      <div>
        <h1 style="font-size:24px;margin-bottom:12px;">应用启动失败</h1>
        <p style="margin-bottom:24px;">${message}</p>
        <button style="padding:10px 20px;border:none;border-radius:8px;background:#409eff;color:#fff;cursor:pointer;" onclick="window.location.reload()">刷新页面</button>
      </div>
    </div>
  `;
};

/**
 * 应用启动主流程：实例化、插件注册、全局组件注册与挂载
 */
const bootstrap = () => {
  try {
    const app = createApp(App);

    plugins.forEach((plugin) => app.use(plugin));
    registerElementPlusIcons(app);

    app.mount('#app');
  } catch (error) {
    console.error('应用启动失败', error);
    mountFallback(error?.message || '请稍后重试');
  }
};

bootstrap();
