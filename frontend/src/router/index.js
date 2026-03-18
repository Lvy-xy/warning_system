import { createRouter, createWebHistory } from "vue-router";
import StrategyConfig from "../views/StrategyConfig.vue";
import IrrigationConsole from "../views/IrrigationConsole.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/strategy" },
    { path: "/strategy", component: StrategyConfig },
    { path: "/console", component: IrrigationConsole },
  ],
});

export default router;
