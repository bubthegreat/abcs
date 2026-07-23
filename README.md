# Let's Learn

A kids' learning app for Android tablets, built for ages 2–9. Letters
(phonics order), decodable words and phrases, vocabulary and grammar
through 2nd grade, a math ladder from counting to algebra (with endless
generated arithmetic and times tables to 20×20), colors and shapes,
stylus handwriting with story writing, and a parent-controlled star
reward system with per-kid profiles.

## Requirements

- **Docker** — the only build dependency; the Android SDK and JDK live in
  the build image.
- **adb** (Android platform-tools) on your PATH — only needed to install
  onto a device from your machine.

## Build

```sh
make apk          # debug APK -> app/build/outputs/apk/debug/app-debug.apk
make test         # unit tests
make release-apk  # unsigned release APK
make clean
```

## Install onto a tablet (USB, default)

1. On the tablet: Settings → About → tap **Build number** 7 times, then
   Developer options → enable **USB debugging**.
2. Plug it in with a data cable and accept the "Allow USB debugging"
   prompt (check *Always allow*).
3. Run:

```sh
make install
```

Builds in Docker, installs via adb on your machine (the one step that
needs host platform-tools — Docker Desktop on Windows can't pass USB
through). Existing progress on the tablet is preserved.

### Optional: fully containerized install over WiFi

No host adb at all; tablet and computer on the same WiFi.

1. Tablet: Developer options → enable **Wireless debugging**.
2. One-time pairing (Wireless debugging → *Pair device with pairing
   code* shows an IP:PORT and 6-digit code):

```sh
make pair DEVICE=192.168.1.42:37123 CODE=123456
```

3. Install using the IP:PORT from the main Wireless debugging screen
   (different port; changes after reboots):

```sh
make install-wifi DEVICE=192.168.1.42:41234
```

## Releases

Every push to `master` runs tests, builds the APK, and publishes an
auto-incrementing GitHub release (`v1.0.<n>`) with the installable APK
attached — grab the latest from the Releases page and sideload it if
you don't want to build anything.
