import { createApp } from "vue";
import { createPinia } from "pinia";
import "./styles/main.css";
import App from "./App.vue";
import router from "./router";

function installRandomUuidFallback(): void {
  const webCrypto = globalThis.crypto;

  if (!webCrypto || typeof webCrypto.randomUUID === "function") {
    return;
  }

  Object.defineProperty(webCrypto, "randomUUID", {
    configurable: true,
    value: (): string =>
      "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(
        /[xy]/g,
        (character) => {
          const randomValue = Math.floor(Math.random() * 16);
          const value =
            character === "x"
              ? randomValue
              : (randomValue & 0x3) | 0x8;

          return value.toString(16);
        },
      ),
  });
}

installRandomUuidFallback();

createApp(App).use(createPinia()).use(router).mount("#app");
