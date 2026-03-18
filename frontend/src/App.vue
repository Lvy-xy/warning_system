<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="brand">水肥智能预警调控系统</div>
      <el-menu mode="horizontal" :default-active="activePath" router>
        <el-menu-item index="/strategy">策略配置</el-menu-item>
        <el-menu-item index="/console">控制台</el-menu-item>
      </el-menu>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from "vue";
import { useRoute } from "vue-router";

const route = useRoute();
const activePath = computed(() => route.path);
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
  padding: 0 24px;
}

.brand {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
}

.app-main {
  padding: 0;
  background: #f5f7fa;
}
</style>
