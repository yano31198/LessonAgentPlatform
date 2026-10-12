import { createApp } from "vue";
import { createPinia } from "pinia";
import axios from "axios";
import "./styles/main.css";
import App from "./App.vue";
import router from "./router";
import { useAuthStore } from "./stores/auth";

axios.defaults.withCredentials = true;
axios.defaults.xsrfCookieName = "XSRF-TOKEN";
axios.defaults.xsrfHeaderName = "X-XSRF-TOKEN";

const pinia = createPinia();
const auth = useAuthStore(pinia);

router.beforeEach(async (to) => {
  await auth.restore();
  if (to.meta.public) {
    if (auth.user && (to.name === "login" || to.name === "register")) return "/";
    return true;
  }
  if (!auth.user) {
    return { name: "login", query: { redirect: to.fullPath } };
  }
  return true;
});

createApp(App).use(pinia).use(router).mount("#app");
