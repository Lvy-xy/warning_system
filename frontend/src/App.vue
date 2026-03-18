<template>
  <router-view v-if="isLoginPage" />

  <el-container v-else class="app-shell">
    <el-header class="app-header">
      <div class="header-left">
        <div class="brand">水肥智能预警调控系统</div>
        <el-menu mode="horizontal" :default-active="activePath" router>
          <el-menu-item index="/strategy">策略配置</el-menu-item>
          <el-menu-item index="/console">控制台</el-menu-item>
        </el-menu>
      </div>
      <div class="header-right">
        <span class="username">{{ username || "管理员" }}</span>
        <el-button type="danger" plain size="small" @click="handleLogout">
          退出登录
        </el-button>
      </div>
    </el-header>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from "vue";
import { ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import { getUsername, logoutSession } from "./utils/auth";

const router = useRouter();
const route = useRoute();
const activePath = computed(() => route.path);
const isLoginPage = computed(() => route.path === "/login");
const username = computed(() => getUsername());

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm("确认退出当前账号吗？", "提示", {
      type: "warning",
      confirmButtonText: "退出",
      cancelButtonText: "取消",
    });
  } catch (e) {
    return;
  }
  logoutSession();
  await router.replace("/login");
};
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
  margin-right: 24px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.username {
  color: #606266;
  font-size: 14px;
}

.app-main {
  padding: 0;
  background: #f5f7fa;
}
</style>
