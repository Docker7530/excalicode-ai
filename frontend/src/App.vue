<template>
  <ElConfigProvider :locale="zhCn">
    <RouterView />
  </ElConfigProvider>
</template>

<script setup>
import zhCn from 'element-plus/es/locale/lang/zh-cn';
</script>
