# Daniel Hryciuk Photography

Statyczne portfolio fotograficzne dla domeny `dhryciuk.eu`, przygotowane pod GitHub Pages, lekką bramkę hasła, duże galerie i automatyczne generowanie miniaturek.

Hasło dostępu do strony: `FIGA`. To jest lekkie zabezpieczenie w `localStorage`, nie ochrona bankowa.

## Aplikacja Android

Folder `android-uploader/` zawiera natywną aplikację **DH Uploader** do dodawania zdjęć z telefonu bez użycia komputera. Aplikacja pozwala wybrać istniejący album lub utworzyć nowy, generuje zoptymalizowane zdjęcia i miniatury WebP, aktualizuje `gallery.json` i publikuje wszystko jednym commitem.

Gotowy APK do instalacji po publikacji strony: `https://dhryciuk.eu/assets/apk/dh-uploader-1.0.0-debug.apk`.

Instrukcja konfiguracji, tworzenia ograniczonego tokenu GitHub oraz instalacji znajduje się w [`android-uploader/README.md`](android-uploader/README.md).

## Struktura

```text
/
├── index.html
├── gallery.html
├── about.html
├── contact.html
├── login.html
├── css/
├── js/
├── images/
├── galleries/
├── assets/
├── gallery.json
├── generate-gallery.py
├── sitemap.xml
├── robots.txt
└── README.md
```

## Dodawanie zdjęć

1. Umieść zdjęcia w folderach źródłowych na komputerze.
2. Uruchom generator:

```powershell
python -m pip install pillow
python generate-gallery.py --source D:\ --source I:\ --clean
```

Generator:

- wyszukuje foldery zawierające obsługiwane pliki graficzne,
- tworzy zoptymalizowane zdjęcia WebP,
- tworzy miniatury,
- zapisuje dane w `gallery.json`,
- układa albumy w katalogach `galleries/przyroda`, `galleries/figa`, `galleries/motoryzacja`, `galleries/podroze`.

Kategorie są dobierane po nazwie ścieżki. Folder bez rozpoznanej kategorii trafia domyślnie do `Podróże`.

## Galerie i wydajność

Frontend nie ładuje wszystkich zdjęć naraz. `gallery.html` pobiera `gallery.json`, pokazuje albumy i renderuje zdjęcia partiami po 36 miniatur. Obrazy mają `loading="lazy"`, a pełny plik zdjęcia ładuje się dopiero po otwarciu lightboxa.

Lightbox obsługuje:

- pełny ekran,
- klawiaturę: `Esc`, strzałki lewo/prawo,
- gest przesunięcia na telefonie,
- pokaz slajdów.

## Kontakt

Wersja GitHub Pages używa FormSubmit w `contact.html`:

```html
https://formsubmit.co/daniel@dhryciuk.eu
```

Po pierwszym wysłaniu FormSubmit zwykle wymaga potwierdzenia adresu e-mail.

Wersja dla hostingu ViperHost znajduje się w:

```text
assets/php/contact.php
```

Na hostingu PHP zmień `action` formularza w `contact.html` na:

```html
action="/assets/php/contact.php"
```

## Publikacja na GitHub Pages

1. Utwórz nowe repozytorium na GitHubie, np. `dhryciuk-eu`.
2. Wgraj wszystkie pliki z tego katalogu.
3. Wejdź w `Settings → Pages`.
4. Wybierz `Deploy from a branch`.
5. Branch: `main`, folder: `/root`.
6. Zapisz ustawienia.
7. W sekcji `Custom domain` wpisz `dhryciuk.eu`.
8. Zostaw plik `CNAME` w repozytorium, zawiera już `dhryciuk.eu`.
9. Po propagacji DNS włącz `Enforce HTTPS`.

## DNS w ViperHost

W panelu ViperHost ustaw rekordy dla domeny głównej:

```text
A     @     185.199.108.153
A     @     185.199.109.153
A     @     185.199.110.153
A     @     185.199.111.153
CNAME www   danielhryciuk87-cpu.github.io.
```

Jeśli ViperHost wymaga pełnej nazwy hosta, użyj:

```text
CNAME www.dhryciuk.eu   danielhryciuk87-cpu.github.io.
```

Po zmianie DNS propagacja może potrwać od kilku minut do 24 godzin. SSL w GitHub Pages pojawi się dopiero po poprawnym wskazaniu domeny.

## Automatyczne aktualizacje

Najprostszy proces:

```powershell
python generate-gallery.py --source D:\ --source I:\ --clean
git add .
git commit -m "Update galleries"
git push
```

GitHub Pages automatycznie opublikuje nową wersję po pushu.

## Uruchomienie lokalne

```powershell
node assets/scripts/local-server.mjs
```

Adres podglądu:

```text
http://127.0.0.1:4175
```

## SEO

Projekt zawiera:

- `sitemap.xml`,
- `robots.txt`,
- canonical URL,
- OpenGraph,
- `schema.org` typu `Photographer`,
- favicon SVG,
- semantyczny HTML.

## Rozwiązywanie problemów

Jeśli galeria jest pusta, sprawdź czy istnieje `gallery.json` i czy generator znalazł foldery ze zdjęciami.

Jeśli zdjęcia nie otwierają się po publikacji, sprawdź wielkość liter w nazwach plików. GitHub Pages rozróżnia wielkość liter.

Jeśli domena nie działa, sprawdź rekordy DNS w ViperHost i status domeny w `Settings → Pages`.

Jeśli HTTPS nie chce się włączyć, usuń i ponownie wpisz domenę w GitHub Pages po poprawnej propagacji DNS.
