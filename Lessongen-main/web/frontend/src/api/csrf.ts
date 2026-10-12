export function csrfHeaders(): Record<string, string> {
  const value = document.cookie.split("; ")
    .find(item => item.startsWith("XSRF-TOKEN="))
    ?.slice("XSRF-TOKEN=".length);
  return value ? { "X-XSRF-TOKEN": decodeURIComponent(value) } : {};
}
