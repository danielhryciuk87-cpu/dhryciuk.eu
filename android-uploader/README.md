# DH Uploader

Natywna aplikacja Android do publikowania zdjęć z telefonu bezpośrednio w galerii na `dhryciuk.eu`.

## Jak działa

1. Wybierasz istniejącą kategorię i album albo tworzysz nowy album.
2. Systemowy selektor zdjęć Androida pozwala wybrać do 50 zdjęć naraz.
3. Aplikacja tworzy lokalnie pliki WebP:
   - pełne zdjęcie: maksymalnie 2200 px, jakość 86,
   - miniatura: maksymalnie 520 px, jakość 72.
4. Zdjęcia, miniatury i zaktualizowany `gallery.json` są publikowane w jednym commicie.
5. GitHub Pages automatycznie odświeża stronę.

Zdjęcia trafiają do:

```text
galleries/<kategoria>/<album>/photos/
galleries/<kategoria>/<album>/thumbs/
```

## Konfiguracja tokenu GitHub

Utwórz fine-grained personal access token:

1. GitHub: `Settings > Developer settings > Personal access tokens > Fine-grained tokens`.
2. Wybierz tylko repozytorium `danielhryciuk87-cpu/dhryciuk.eu`.
3. Ustaw uprawnienie repozytorium `Contents: Read and write`.
4. W aplikacji otwórz ustawienia, wklej token i wybierz `Sprawdź i zapisz`.

Token nie znajduje się w kodzie ani w repozytorium. Aplikacja przechowuje go zaszyfrowanego przy użyciu Android Keystore. W razie utraty telefonu token należy odwołać na GitHubie.

## Budowanie i instalacja

Projekt wymaga Android Studio z JDK 17 i Android SDK 36.

Gotowy plik APK został też skopiowany do strony:

```text
assets/apk/dh-uploader-1.0.0-debug.apk
```

Po publikacji repozytorium będzie dostępny pod adresem:

```text
https://dhryciuk.eu/assets/apk/dh-uploader-1.0.0-debug.apk
```

1. Otwórz folder `android-uploader` w Android Studio.
2. Poczekaj na synchronizację Gradle.
3. Podłącz Pixel 7 przez USB z włączonym debugowaniem USB.
4. Uruchom konfigurację `app`.

Plik APK można też utworzyć poleceniem:

```powershell
.\gradlew.bat assembleDebug
```

Wynik:

```text
app\build\outputs\apk\debug\app-debug.apk
```

## Bezpieczeństwo

To narzędzie administracyjne. Nie publikuj APK razem z tokenem i nie udostępniaj tokenu innym osobom. Uprawnienie tokenu powinno być ograniczone do jednego repozytorium.
