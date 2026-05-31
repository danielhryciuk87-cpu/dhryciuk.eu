(() => {
  const PASSWORD = "FIGA";
  const KEY = "dhryciuk_access";
  const isLoginPage = location.pathname.endsWith("/login.html") || location.pathname.endsWith("login.html");
  const protectedPage = document.body?.dataset.protected === "true";

  const isAllowed = () => localStorage.getItem(KEY) === "granted";

  if (protectedPage && !isAllowed()) {
    const next = encodeURIComponent(location.pathname.split("/").pop() || "index.html");
    location.replace(`login.html?next=${next}`);
    return;
  }

  if (isLoginPage && isAllowed()) {
    location.replace(new URLSearchParams(location.search).get("next") || "index.html");
    return;
  }

  document.addEventListener("DOMContentLoaded", () => {
    const form = document.querySelector("[data-login-form]");
    const error = document.querySelector("[data-login-error]");

    if (form) {
      form.addEventListener("submit", (event) => {
        event.preventDefault();
        const value = new FormData(form).get("password");
        if (value === PASSWORD) {
          localStorage.setItem(KEY, "granted");
          location.replace(new URLSearchParams(location.search).get("next") || "index.html");
        } else if (error) {
          error.textContent = "Nieprawidłowe hasło.";
        }
      });
    }

    document.querySelectorAll("[data-logout]").forEach((button) => {
      button.addEventListener("click", () => {
        localStorage.removeItem(KEY);
        location.replace("login.html");
      });
    });
  });
})();
