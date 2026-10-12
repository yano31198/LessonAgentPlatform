import axios from "axios";
import { defineStore } from "pinia";
import { computed, ref } from "vue";

export interface AuthUser {
  id: string;
  username: string;
  displayName: string;
  role: "USER" | "ADMIN";
  lastLoginAt?: string | null;
}

export const useAuthStore = defineStore("auth", () => {
  const user = ref<AuthUser | null>(null);
  const initialized = ref(false);
  const loading = ref(false);

  async function prepareCsrf() {
    await axios.get("/api/auth/csrf");
  }

  async function restore() {
    if (initialized.value) return;
    try {
      await prepareCsrf();
      user.value = (await axios.get<AuthUser>("/api/auth/me")).data;
    } catch {
      user.value = null;
    } finally {
      initialized.value = true;
    }
  }

  async function login(username: string, password: string) {
    loading.value = true;
    try {
      user.value = (await axios.post<AuthUser>("/api/auth/login", { username, password })).data;
      await prepareCsrf();
    } finally {
      loading.value = false;
      initialized.value = true;
    }
  }

  async function register(username: string, password: string, displayName: string) {
    loading.value = true;
    try {
      await axios.post<AuthUser>("/api/auth/register", { username, password, displayName });
      await login(username, password);
    } finally {
      loading.value = false;
    }
  }

  async function logout() {
    try {
      await axios.post("/api/auth/logout");
    } finally {
      user.value = null;
      initialized.value = true;
    }
  }

  return {
    user,
    initialized,
    loading,
    isAuthenticated: computed(() => Boolean(user.value)),
    restore,
    login,
    register,
    logout,
  };
});
