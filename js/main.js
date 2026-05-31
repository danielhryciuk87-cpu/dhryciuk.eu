document.addEventListener("DOMContentLoaded", () => {
  const header = document.querySelector("[data-header]");
  const nav = document.querySelector("#site-nav");
  const toggle = document.querySelector(".nav-toggle");

  const setHeader = () => header?.classList.toggle("is-scrolled", window.scrollY > 16);
  setHeader();
  window.addEventListener("scroll", setHeader, { passive: true });

  toggle?.addEventListener("click", () => {
    const open = nav?.classList.toggle("is-open");
    toggle.setAttribute("aria-expanded", String(Boolean(open)));
  });

  document.querySelectorAll("[data-year]").forEach((node) => {
    node.textContent = String(new Date().getFullYear());
  });
});
