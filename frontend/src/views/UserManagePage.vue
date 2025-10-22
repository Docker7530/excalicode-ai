<template>
  <div class="user-manage-page">
    <AppHeader />

    <section class="hero-section">
      <div class="hero-content">
        <h1 class="page-title">
          <ElIcon class="title-icon"><User /></ElIcon>
          人员管理
        </h1>
        <p class="page-desc">
          管理系统登录账号，支持创建、更新、删除管理员与普通用户
        </p>
      </div>
      <ElButton
        type="primary"
        size="large"
        :icon="Plus"
        class="create-button"
        @click="openCreateDialog"
      >
        新增人员
      </ElButton>
    </section>

    <section v-loading="loading" class="table-section">
      <ElTable
        :data="users"
        border
        stripe
        empty-text="暂无人员数据"
        class="user-table"
      >
        <ElTableColumn prop="username" label="用户名" min-width="160" />
        <ElTableColumn label="角色" min-width="140">
          <template #default="{ row }">
            <ElTag :type="row.role === 'ADMIN' ? 'danger' : 'info'">
              {{ roleLabelMap[row.role] || row.role }}
            </ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn
          prop="createdTime"
          label="创建时间"
          min-width="200"
          :formatter="formatDate"
        />
        <ElTableColumn
          prop="updatedTime"
          label="更新时间"
          min-width="200"
          :formatter="formatDate"
        />
        <ElTableColumn label="操作" min-width="180" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <ElButton
                size="small"
                :icon="Edit"
                type="primary"
                text
                @click="openEditDialog(row)"
              >
                编辑
              </ElButton>
              <ElPopconfirm
                title="确定要删除该人员吗？"
                confirm-button-text="删除"
                cancel-button-text="取消"
                confirm-button-type="danger"
                @confirm="handleDelete(row)"
              >
                <template #reference>
                  <ElButton size="small" :icon="Delete" type="danger" text>
                    删除
                  </ElButton>
                </template>
              </ElPopconfirm>
            </div>
          </template>
        </ElTableColumn>
      </ElTable>
    </section>

    <ElDialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="480px"
      destroy-on-close
    >
      <ElForm
        ref="formRef"
        :model="formModel"
        :rules="formRules"
        label-width="96px"
        class="user-form"
      >
        <ElFormItem label="用户名" prop="username">
          <ElInput
            v-model.trim="formModel.username"
            placeholder="请输入用户名"
            maxlength="50"
            clearable
          />
        </ElFormItem>

        <ElFormItem
          :label="dialogMode === 'create' ? '登录密码' : '登录密码'"
          prop="password"
        >
          <ElInput
            v-model.trim="formModel.password"
            type="password"
            :placeholder="
              dialogMode === 'create' ? '请输入初始密码' : '不修改请留空'
            "
            show-password
            clearable
          />
        </ElFormItem>

        <ElFormItem label="角色" prop="role">
          <ElSelect v-model="formModel.role" placeholder="请选择角色">
            <ElOption label="管理员" value="ADMIN" />
            <ElOption label="普通用户" value="USER" />
          </ElSelect>
        </ElFormItem>
      </ElForm>

      <template #footer>
        <div class="dialog-footer">
          <ElButton @click="dialogVisible = false">取消</ElButton>
          <ElButton
            type="primary"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            确认
          </ElButton>
        </div>
      </template>
    </ElDialog>
  </div>
</template>

<script setup>
import { createUser, fetchUsers, removeUser, updateUser } from '@/api/user';
import AppHeader from '@/components/AppHeader.vue';
import { Delete, Edit, Plus, User } from '@element-plus/icons-vue';

const users = ref([]);
const loading = ref(false);
const submitLoading = ref(false);
const dialogVisible = ref(false);
const dialogMode = ref('create');

const roleLabelMap = {
  ADMIN: '管理员',
  USER: '普通用户',
};

const formRef = ref();
const formModel = reactive({
  id: null,
  username: '',
  password: '',
  role: 'USER',
});

const formRules = computed(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    {
      min: 3,
      max: 50,
      message: '用户名长度需在3-50个字符之间',
      trigger: 'blur',
    },
  ],
  password:
    dialogMode.value === 'create'
      ? [
          { required: true, message: '请输入密码', trigger: 'blur' },
          {
            min: 6,
            max: 64,
            message: '密码长度需在6-64个字符之间',
            trigger: 'blur',
          },
        ]
      : [
          {
            min: 6,
            max: 64,
            message: '密码长度需在6-64个字符之间',
            trigger: 'blur',
          },
        ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}));

const dialogTitle = computed(() =>
  dialogMode.value === 'create' ? '新增人员' : '编辑人员',
);

const resetForm = () => {
  formModel.id = null;
  formModel.username = '';
  formModel.password = '';
  formModel.role = 'USER';
};

const loadUsers = async () => {
  loading.value = true;
  try {
    const data = await fetchUsers();
    users.value = Array.isArray(data) ? data : [];
  } catch (error) {
    ElMessage.error(error.message || '加载人员数据失败');
  } finally {
    loading.value = false;
  }
};

const openCreateDialog = () => {
  dialogMode.value = 'create';
  resetForm();
  dialogVisible.value = true;
  nextTick(() => {
    formRef.value?.clearValidate();
  });
};

const openEditDialog = (row) => {
  dialogMode.value = 'edit';
  formModel.id = row.id;
  formModel.username = row.username;
  formModel.password = '';
  formModel.role = row.role;
  dialogVisible.value = true;
  nextTick(() => {
    formRef.value?.clearValidate();
  });
};

const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    submitLoading.value = true;
    await formRef.value.validate();

    if (dialogMode.value === 'create') {
      await createUser({
        username: formModel.username,
        password: formModel.password,
        role: formModel.role,
      });
      ElMessage.success('新增人员成功');
    } else {
      const payload = {
        username: formModel.username,
        role: formModel.role,
      };
      if (formModel.password) {
        payload.password = formModel.password;
      }
      await updateUser(formModel.id, payload);
      ElMessage.success('更新人员成功');
    }

    dialogVisible.value = false;
    await loadUsers();
  } catch (error) {
    if (error?.message) {
      ElMessage.error(error.message);
    }
  } finally {
    submitLoading.value = false;
  }
};

const handleDelete = async (row) => {
  try {
    await removeUser(row.id);
    ElMessage.success('删除人员成功');
    await loadUsers();
  } catch (error) {
    ElMessage.error(error.message || '删除人员失败');
  }
};

const formatDate = (_row, _column, cellValue) => {
  if (!cellValue) return '-';
  try {
    const date = new Date(cellValue);
    return new Intl.DateTimeFormat('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }).format(date);
  } catch {
    return cellValue;
  }
};

watchEffect(() => {
  if (!dialogVisible.value) {
    resetForm();
  }
});

onMounted(() => {
  loadUsers();
});
</script>

<style scoped lang="scss">
.user-manage-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #ffffff 0%, #f8fafc 40%, #eef2ff 100%);
  padding: 120px 24px 48px;

  @media (max-width: 768px) {
    padding: 100px 16px 32px;
  }
}

.hero-section {
  max-width: 960px;
  margin: 0 auto 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 24px;
  padding: 32px 40px;
  box-shadow: 0 24px 60px rgba(79, 70, 229, 0.12);

  @media (max-width: 768px) {
    flex-direction: column;
    align-items: flex-start;
    padding: 24px;
  }
}

.hero-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 2rem;
  font-weight: 700;
  color: #1e1b4b;
  margin: 0;
}

.title-icon {
  padding: 12px;
  border-radius: 16px;
  background: rgba(79, 70, 229, 0.12);
  color: #4f46e5;
  font-size: 24px;
}

.page-desc {
  margin: 0;
  font-size: 0.95rem;
  color: #4c566a;
}

.create-button {
  border-radius: 999px;
  padding: 0 28px;
  height: 48px;
  font-weight: 600;
  box-shadow: 0 18px 30px rgba(79, 70, 229, 0.18);

  @media (max-width: 768px) {
    width: 100%;
  }
}

.table-section {
  max-width: 960px;
  margin: 0 auto;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 20px;
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.12);
  padding: 24px;

  @media (max-width: 768px) {
    padding: 16px;
  }
}

.user-table {
  width: 100%;
}

.action-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

.user-form {
  padding: 12px 0;
}
</style>
