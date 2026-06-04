document.addEventListener("DOMContentLoaded", async () => {
  const app = document.querySelector("[data-gallery-app]");
  if (!app) return;

  const grid = app.querySelector("[data-grid]");
  const albumsNode = app.querySelector("[data-albums]");
  const loadMore = app.querySelector("[data-load-more]");
  const empty = app.querySelector("[data-empty]");
  const toolbar = app.querySelector(".gallery-toolbar");
  let chips = [...app.querySelectorAll("[data-filter]")];
  const batchSize = 36;
  let allPhotos = [];
  let albums = [];
  let visiblePhotos = [];
  let rendered = 0;
  let activeCategory = "all";
  let activeAlbum = "all";
  let lightboxIndex = 0;
  let slideshow = null;

  const lightbox = document.querySelector("[data-lightbox]");
  const lightboxImage = document.querySelector("[data-lightbox-image]");
  const lightboxCaption = document.querySelector("[data-lightbox-caption]");

  try {
    const response = await fetch("gallery.json", { cache: "no-store" });
    const data = await response.json();
    albums = data.albums || [];
    renderCategoryFilters();
    allPhotos = albums.flatMap((album) => (album.photos || []).map((photo, index) => ({
      ...photo,
      albumId: album.id,
      albumTitle: album.title,
      category: album.category || "podroze",
      index
    })));
    renderAlbums();
    applyFilters();
  } catch (error) {
    empty.hidden = false;
    empty.textContent = "Nie udało się wczytać gallery.json. Uruchom generator galerii.";
  }

  function renderCategoryFilters() {
    const categories = [...new Set(albums.map((album) => album.category || "pozostale"))].sort((a, b) => a.localeCompare(b, "pl"));
    categories.forEach((category) => {
      const button = document.createElement("button");
      button.className = "chip";
      button.type = "button";
      button.dataset.filter = category;
      button.textContent = category.replace(/-/g, " ").replace(/^./, (letter) => letter.toLocaleUpperCase("pl"));
      toolbar.append(button);
    });
    chips = [...app.querySelectorAll("[data-filter]")];
    chips.forEach((chip) => {
      chip.addEventListener("click", () => {
        activeCategory = chip.dataset.filter;
        activeAlbum = "all";
        chips.forEach((item) => item.classList.toggle("is-active", item === chip));
        renderAlbums();
        applyFilters();
      });
    });
  }

  function renderAlbums() {
    albumsNode.innerHTML = "";
    const activeAlbums = albums.filter((album) => activeCategory === "all" || album.category === activeCategory);
    const all = albumButton({ id: "all", title: "Wszystkie albumy", photos: activeAlbums.flatMap((album) => album.photos || []) });
    albumsNode.append(all);
    activeAlbums.forEach((album) => albumsNode.append(albumButton(album)));
  }

  function albumButton(album) {
    const button = document.createElement("button");
    button.className = `album-card${activeAlbum === album.id ? " is-active" : ""}`;
    button.type = "button";
    button.dataset.album = album.id;
    button.innerHTML = `<strong>${escapeHtml(album.title)}</strong><span>${(album.photos || []).length} zdjęć</span>`;
    button.addEventListener("click", () => {
      activeAlbum = album.id;
      renderAlbums();
      applyFilters();
    });
    return button;
  }

  function applyFilters() {
    visiblePhotos = allPhotos.filter((photo) => {
      const categoryOk = activeCategory === "all" || photo.category === activeCategory;
      const albumOk = activeAlbum === "all" || photo.albumId === activeAlbum;
      return categoryOk && albumOk;
    });
    rendered = 0;
    grid.innerHTML = "";
    empty.hidden = visiblePhotos.length > 0;
    renderBatch();
  }

  function renderBatch() {
    const fragment = document.createDocumentFragment();
    visiblePhotos.slice(rendered, rendered + batchSize).forEach((photo, offset) => {
      const index = rendered + offset;
      const button = document.createElement("button");
      button.className = "photo-card";
      button.type = "button";
      button.innerHTML = `<img src="${photo.thumbnail}" alt="${escapeHtml(photo.alt || photo.albumTitle)}" loading="lazy" decoding="async">`;
      button.addEventListener("click", () => openLightbox(index));
      fragment.append(button);
    });
    grid.append(fragment);
    rendered += batchSize;
    loadMore.hidden = rendered >= visiblePhotos.length;
  }

  loadMore.addEventListener("click", renderBatch);

  function openLightbox(index) {
    lightboxIndex = index;
    updateLightbox();
    lightbox.hidden = false;
    document.body.style.overflow = "hidden";
  }

  function closeLightbox() {
    lightbox.hidden = true;
    document.body.style.overflow = "";
    stopSlideshow();
  }

  function stepLightbox(direction) {
    if (!visiblePhotos.length) return;
    lightboxIndex = (lightboxIndex + direction + visiblePhotos.length) % visiblePhotos.length;
    updateLightbox();
  }

  function updateLightbox() {
    const photo = visiblePhotos[lightboxIndex];
    if (!photo) return;
    lightboxImage.src = photo.src;
    lightboxImage.alt = photo.alt || photo.albumTitle;
    lightboxCaption.textContent = `${photo.albumTitle} · ${lightboxIndex + 1} / ${visiblePhotos.length}`;
  }

  function stopSlideshow() {
    if (slideshow) clearInterval(slideshow);
    slideshow = null;
    document.querySelector("[data-lightbox-play]").textContent = "Pokaz slajdów";
  }

  document.querySelector("[data-lightbox-close]")?.addEventListener("click", closeLightbox);
  document.querySelector("[data-lightbox-prev]")?.addEventListener("click", () => stepLightbox(-1));
  document.querySelector("[data-lightbox-next]")?.addEventListener("click", () => stepLightbox(1));
  document.querySelector("[data-lightbox-play]")?.addEventListener("click", (event) => {
    if (slideshow) {
      stopSlideshow();
    } else {
      event.currentTarget.textContent = "Zatrzymaj";
      slideshow = setInterval(() => stepLightbox(1), 3500);
    }
  });

  document.addEventListener("keydown", (event) => {
    if (lightbox.hidden) return;
    if (event.key === "Escape") closeLightbox();
    if (event.key === "ArrowRight") stepLightbox(1);
    if (event.key === "ArrowLeft") stepLightbox(-1);
  });

  let touchStartX = 0;
  lightbox.addEventListener("touchstart", (event) => { touchStartX = event.changedTouches[0].clientX; }, { passive: true });
  lightbox.addEventListener("touchend", (event) => {
    const diff = event.changedTouches[0].clientX - touchStartX;
    if (Math.abs(diff) > 45) stepLightbox(diff > 0 ? -1 : 1);
  }, { passive: true });

  function escapeHtml(value) {
    return String(value).replace(/[&<>"']/g, (char) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[char]));
  }
});
