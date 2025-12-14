<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1 class="login-title">EXCALICODE AI</h1>
        <p class="login-subtitle">AI 驱动的软件工程平台</p>
      </div>

      <ElForm
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <ElFormItem prop="username">
          <ElInput
            v-model="loginForm.username"
            placeholder="用户名"
            size="large"
            prefix-icon="User"
            clearable
          />
        </ElFormItem>

        <ElFormItem prop="password">
          <ElInput
            v-model="loginForm.password"
            type="password"
            placeholder="密码"
            size="large"
            prefix-icon="Lock"
            show-password
            clearable
          />
        </ElFormItem>

        <ElFormItem>
          <ElButton
            type="primary"
            size="large"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </ElButton>
        </ElFormItem>
      </ElForm>
    </div>
  </div>
</template>

<script setup>
import { login } from '@/api/auth';

const router = useRouter();
const loginFormRef = ref(null);
const loading = ref(false);

// 登录表单数据
const loginForm = reactive({
  username: '',
  password: '',
});

// 表单验证规则
const loginRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
};

/**
 * 处理登录
 */
const handleLogin = async () => {
  if (!loginFormRef.value) return;

  try {
    await loginFormRef.value.validate();

    loading.value = true;

    const { token, username, role } = await login(loginForm);

    // 存储 Token 和用户信息
    localStorage.setItem('token', token);
    localStorage.setItem('username', username);
    localStorage.setItem('role', role);

    ElMessage.success('登录成功!');

    // 跳转到首页
    router.push('/');
  } catch (error) {
    console.error('登录失败:', error);
    ElMessage.error(error.message || '登录失败,请检查用户名和密码');
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  padding: 20px;
}

.login-box {
  width: 100%;
  max-width: 420px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 48px 40px;
  animation: slideIn 0.4s ease-out;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-title {
  font-size: 32px;
  font-weight: 700;
  color: #2c3e50;
  margin: 0 0 12px 0;
  letter-spacing: -0.5px;
}

.login-subtitle {
  font-size: 15px;
  color: #6c757d;
  margin: 0;
  font-weight: 400;
}

.login-form {
  margin-top: 32px;
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  margin-top: 12px;
  background: linear-gradient(135deg, #409eff 0%, #67c23a 100%);
  border: none;

  &:hover {
    background: linear-gradient(135deg, #2f74d6 0%, #4ea226 100%);
    transform: translateY(-1px);
    box-shadow: 0 8px 16px rgba(64, 158, 255, 0.25);
  }

  &:active {
    transform: translateY(0);
  }
}

// 自定义 Element Plus 输入框样式
:deep(.el-input__wrapper) {
  padding: 12px 16px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e0e0e0 inset;

  &:hover {
    box-shadow: 0 0 0 1px #c0c0c0 inset;
  }

  &.is-focus {
    box-shadow: 0 0 0 1px #667eea inset;
  }
}

:deep(.el-input__inner) {
  font-size: 15px;
}
</style>
