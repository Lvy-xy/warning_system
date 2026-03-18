<template>
  <div class="login-page">
    <div class="login-bg"></div>
    <el-card class="login-card" shadow="never">
      <div class="logo-wrap">
        <div class="logo-dot"></div>
      </div>
      <h1 class="title">水肥智能预警调控软件</h1>
      <p class="subtitle">农业现场感知-决策-执行与自动控制平台</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入管理员账号"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            placeholder="请输入系统密码"
            :prefix-icon="Lock"
            show-password
            clearable
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <div class="form-extra">
          <el-checkbox v-model="form.rememberMe">保持登录状态</el-checkbox>
          <span class="tip-link">网络延迟说明</span>
        </div>

        <el-button
          type="primary"
          class="login-btn"
          :loading="loading"
          @click="handleLogin"
        >
          登 录 系 统
        </el-button>
      </el-form>

      <p class="account-tip">测试账号：admin / admin123</p>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { Lock, User } from "@element-plus/icons-vue";
import request from "@/utils/request";
import { getRememberedUsername, loginSession } from "@/utils/auth";

const router = useRouter();
const route = useRoute();
const formRef = ref(null);
const loading = ref(false);

const form = reactive({
  username: "",
  password: "",
  rememberMe: true,
});

const rules = {
  username: [{ required: true, message: "请输入管理员账号", trigger: "blur" }],
  password: [{ required: true, message: "请输入系统密码", trigger: "blur" }],
};

onMounted(() => {
  form.username = getRememberedUsername();
});

const handleLogin = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch (e) {
    return;
  }

  loading.value = true;
  try {
    const res = await request({
      url: "/auth/login",
      method: "post",
      data: {
        username: form.username,
        password: form.password,
      },
    });

    if (res?.success && res?.data?.token) {
      loginSession(res.data.token, res.data.username || form.username, form.rememberMe);
      ElMessage.success("登录成功，正在进入首页");
      const redirect = route.query.redirect || "/strategy";
      await router.replace(String(redirect));
      return;
    }

    ElMessage.error(res?.message || "账号或密码错误");
  } catch (e) {
    ElMessage.error("登录失败，请稍后重试");
  } finally {
    loading.value = false;
  }
};
</script>

<style scoped>
.login-page {
  position: relative;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #031427;
}

.login-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 82% 72%, rgba(30, 182, 120, 0.24), transparent 35%),
    linear-gradient(90deg, rgba(19, 101, 255, 0.18) 0, transparent 16%),
    repeating-linear-gradient(
      90deg,
      rgba(37, 102, 175, 0.06) 0,
      rgba(37, 102, 175, 0.06) 1px,
      transparent 1px,
      transparent 28px
    );
}

.login-card {
  position: relative;
  z-index: 1;
  width: 420px;
  border-radius: 14px;
  border: 1px solid rgba(0, 204, 255, 0.15);
  background: rgba(3, 17, 38, 0.78);
  box-shadow: 0 14px 40px rgba(0, 0, 0, 0.42);
}

.logo-wrap {
  display: flex;
  justify-content: center;
  margin-top: 6px;
}

.logo-dot {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: radial-gradient(circle, #35f3ff 0, #00a5ff 65%, rgba(0, 165, 255, 0.15) 66%);
  box-shadow: 0 0 16px rgba(0, 217, 255, 0.6);
}

.title {
  margin: 14px 0 4px;
  text-align: center;
  color: #00e1ff;
  font-size: 28px;
  letter-spacing: 1px;
}

.subtitle {
  margin: 0 0 20px;
  text-align: center;
  color: rgba(195, 224, 255, 0.62);
  font-size: 13px;
}

.login-form {
  margin-top: 6px;
}

.form-extra {
  margin: -2px 0 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: rgba(192, 216, 245, 0.78);
  font-size: 13px;
}

.tip-link {
  opacity: 0.75;
}

.login-btn {
  width: 100%;
  height: 42px;
  border: 0;
  background: linear-gradient(90deg, #1872ff 0, #00c7ff 100%);
}

.account-tip {
  margin: 14px 0 6px;
  text-align: center;
  color: rgba(157, 190, 224, 0.78);
  font-size: 12px;
}

:deep(.el-input__wrapper) {
  background: rgba(12, 31, 59, 0.86);
  box-shadow: inset 0 0 0 1px rgba(0, 195, 255, 0.11);
}

:deep(.el-input__inner) {
  color: #d7edff;
}

:deep(.el-checkbox__label) {
  color: rgba(192, 216, 245, 0.78);
}
</style>
