<template>
  <div class="chatbi-page">
    <div class="chatbi-container">
      <div class="left-panel">
        <div class="panel-header">
          <div class="panel-title">ChatBI</div>
          <ElButton type="primary" size="small" @click="startNewSession">
            新对话
          </ElButton>
        </div>

        <ElScrollbar class="session-list">
          <div
            v-for="session in sessions"
            :key="session.id"
            class="session-item"
            :class="{ active: session.id === activeSessionId }"
            @click="openSession(session.id)"
          >
            <div class="session-title">{{ session.title }}</div>
            <div class="session-time">
              {{ formatTime(session.lastActiveTime) }}
            </div>
          </div>

          <div v-if="!sessions.length" class="session-empty">暂无会话</div>
        </ElScrollbar>
      </div>

      <div class="right-panel">
        <ElScrollbar ref="chatScrollbar" class="chat-area">
          <div
            v-for="(msg, idx) in messages"
            :key="idx"
            class="chat-message"
            :class="msg.role"
          >
            <div class="role">
              {{ msg.role === 'USER' ? '你' : 'ChatBI' }}
            </div>
            <div class="content">{{ msg.content }}</div>

            <div v-if="msg.result && msg.result.columns?.length" class="result">
              <ElTable
                :data="msg.tableRows"
                size="small"
                border
                style="width: 100%"
              >
                <ElTableColumn
                  v-for="col in msg.result.columns"
                  :key="col"
                  :prop="col"
                  :label="col"
                  min-width="140"
                  show-overflow-tooltip
                />
              </ElTable>
            </div>

            <ElCollapse v-if="isAdmin && msg.debug" class="debug">
              <ElCollapseItem title="调试信息" name="1">
                <div class="debug-block">
                  <div class="debug-title">Plan</div>
                  <pre class="debug-pre">{{ msg.debug.plan }}</pre>
                </div>
                <div class="debug-block">
                  <div class="debug-title">SQL</div>
                  <pre class="debug-pre">{{ msg.debug.sql }}</pre>
                </div>
              </ElCollapseItem>
            </ElCollapse>
          </div>

          <div v-if="loading" class="loading">ChatBI 思考中…</div>
        </ElScrollbar>

        <div class="composer">
          <ElInput
            v-model="question"
            type="textarea"
            :rows="3"
            placeholder="例如：帮我看下现在有几个人有任务呢？每个人有几个任务？"
            @keydown.enter.exact.prevent="send"
          />
          <div class="composer-actions">
            <ElButton type="primary" :loading="loading" @click="send">
              发送
            </ElButton>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import {
  askChatBi,
  getChatBiSessionDetail,
  listChatBiSessions,
} from '@/api/chatbi.js';

const sessions = ref([]);
const activeSessionId = ref(null);
const messages = ref([]);

const question = ref('');
const loading = ref(false);

const chatScrollbar = ref(null);

const role = ref(localStorage.getItem('role') || 'USER');
const isAdmin = computed(() => role.value === 'ADMIN');

const scrollToBottom = async () => {
  await nextTick();
  const wrap = chatScrollbar.value?.wrapRef;
  if (wrap) {
    wrap.scrollTop = wrap.scrollHeight;
  }
};

const formatTime = (value) => {
  if (!value) return '';
  return String(value).replace('T', ' ');
};

const hydrateMessage = (msg) => {
  const hydrated = {
    role: msg.role,
    content: msg.content,
    result: msg.result || null,
    debug: null,
    tableRows: [],
  };

  if (hydrated.result?.columns?.length && hydrated.result?.rows?.length) {
    hydrated.tableRows = hydrated.result.rows.map((row) => {
      const obj = {};
      hydrated.result.columns.forEach((col, idx) => {
        obj[col] = row?.[idx];
      });
      return obj;
    });
  }

  return hydrated;
};

const loadSessions = async () => {
  try {
    sessions.value = await listChatBiSessions();
  } catch (error) {
    ElMessage.error(error?.message || '加载会话失败');
  }
};

const openSession = async (sessionId) => {
  if (!sessionId) return;
  activeSessionId.value = sessionId;

  try {
    const detail = await getChatBiSessionDetail(sessionId);
    messages.value = (detail?.messages || []).map(hydrateMessage);
    await scrollToBottom();
  } catch (error) {
    ElMessage.error(error?.message || '加载会话详情失败');
  }
};

const startNewSession = () => {
  activeSessionId.value = null;
  messages.value = [];
  question.value = '';
};

const send = async () => {
  const text = question.value.trim();
  if (!text || loading.value) return;

  loading.value = true;
  try {
    messages.value.push({
      role: 'USER',
      content: text,
      result: null,
      tableRows: [],
    });
    question.value = '';
    await scrollToBottom();

    const payload = {
      sessionId: activeSessionId.value,
      question: text,
    };

    const res = await askChatBi(payload);

    if (res?.sessionId) {
      activeSessionId.value = res.sessionId;
    }

    const assistantMsg = {
      role: 'ASSISTANT',
      content: res?.answer || res?.clarifyingQuestion || '已完成',
      result: res?.result || null,
      debug: isAdmin.value
        ? {
            plan: res?.debugPlan || '',
            sql: res?.debugSql || '',
          }
        : null,
      tableRows: [],
    };

    if (
      assistantMsg.result?.columns?.length &&
      assistantMsg.result?.rows?.length
    ) {
      assistantMsg.tableRows = assistantMsg.result.rows.map((row) => {
        const obj = {};
        assistantMsg.result.columns.forEach((col, idx) => {
          obj[col] = row?.[idx];
        });
        return obj;
      });
    }

    messages.value.push(assistantMsg);
    await scrollToBottom();

    await loadSessions();
  } catch (error) {
    ElMessage.error(error?.message || '发送失败');
  } finally {
    loading.value = false;
  }
};

onMounted(async () => {
  role.value = localStorage.getItem('role') || 'USER';
  await loadSessions();
  if (sessions.value?.length) {
    await openSession(sessions.value[0].id);
  }
});
</script>

<style scoped lang="scss">
.chatbi-page {
  min-height: 100vh;
  background: #f6f7fb;
  padding-top: 96px;

  @media (max-width: 768px) {
    padding-top: 84px;
  }
}

.chatbi-container {
  display: flex;
  height: calc(100vh - 96px);

  @media (max-width: 768px) {
    height: calc(100vh - 84px);
  }
}

.left-panel {
  width: 280px;
  border-right: 1px solid #e5e7eb;
  background: #ffffff;
  display: flex;
  flex-direction: column;
}

.panel-header {
  padding: 16px;
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f1f5f9;
}

.panel-title {
  font-weight: 700;
  color: #0f172a;
}

.session-list {
  padding: 12px;
}

.session-item {
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  border: 1px solid transparent;
  margin-bottom: 10px;
  transition: all 0.15s ease;
}

.session-item:hover {
  background: #f8fafc;
}

.session-item.active {
  border-color: #c7d2fe;
  background: #eef2ff;
}

.session-title {
  font-size: 14px;
  color: #111827;
  line-height: 18px;
}

.session-time {
  margin-top: 6px;
  font-size: 12px;
  color: #64748b;
}

.session-empty {
  color: #94a3b8;
  font-size: 13px;
  padding: 10px;
}

.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-area {
  flex: 1;
  padding: 18px 22px;
}

.chat-message {
  background: #ffffff;
  border: 1px solid #eef2f7;
  border-radius: 14px;
  padding: 12px 14px;
  margin-bottom: 12px;
}

.chat-message.USER {
  border-color: #dbeafe;
}

.chat-message.ASSISTANT {
  border-color: #e9d5ff;
}

.role {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
}

.content {
  font-size: 14px;
  color: #0f172a;
  white-space: pre-wrap;
  line-height: 20px;
}

.result {
  margin-top: 12px;
}

.loading {
  color: #64748b;
  font-size: 13px;
  padding: 10px 0;
}

.composer {
  padding: 16px 22px;
  border-top: 1px solid #e5e7eb;
  background: #ffffff;
}

.composer-actions {
  margin-top: 10px;
  display: flex;
  justify-content: flex-end;
}

.debug {
  margin-top: 10px;
}

.debug-block {
  margin-bottom: 12px;
}

.debug-title {
  font-weight: 600;
  margin-bottom: 6px;
}

.debug-pre {
  background: #0b1020;
  color: #e5e7eb;
  padding: 12px;
  border-radius: 10px;
  overflow: auto;
  max-height: 240px;
}
</style>
