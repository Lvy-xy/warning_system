import { createRouter, createWebHistory } from "vue-router";
import StrategyConfig from "../views/StrategyConfig.vue";
import IrrigationConsole from "../views/IrrigationConsole.vue";
import Login from "../views/Login.vue";
import { isLoggedIn } from "../utils/auth";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/login" },
    { path: "/login", component: Login },
    { path: "/strategy", component: StrategyConfig, meta: { requiresAuth: true } },
    { path: "/console", component: IrrigationConsole, meta: { requiresAuth: true } },
  ],
});

router.beforeEach((to) => {
  const authed = isLoggedIn();

  if (to.path === "/login" && authed) {
    return "/strategy";
  }

  if (to.meta.requiresAuth && !authed) {
    return { path: "/login", query: { redirect: to.fullPath } };
  }

  return true;
});

export default router;
