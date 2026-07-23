# Build the app with zero local dependencies beyond Docker.
# The cirruslabs image ships JDK 17 + Android SDK 34; the Gradle wrapper
# supplies Gradle itself. Gradle caches land in .gradle-docker/ so repeat
# builds are fast.

ANDROID_IMAGE := ghcr.io/cirruslabs/android-sdk:34
# MSYS_NO_PATHCONV stops Git Bash on Windows from rewriting /work into a
# C:/... path; it's ignored everywhere else.
DOCKER_RUN := MSYS_NO_PATHCONV=1 docker run --rm \
	-v "$(CURDIR)":/work \
	-w /work \
	-e GRADLE_USER_HOME=/work/.gradle-docker \
	$(ANDROID_IMAGE)

REPO := bubthegreat/abcs

# Use adb from PATH when present; otherwise fall back to the default
# Android SDK location (where Android Studio puts platform-tools).
ADB := $(shell command -v adb 2>/dev/null || echo "$(LOCALAPPDATA)/Android/Sdk/platform-tools/adb.exe")

.PHONY: apk release-apk test clean install install-latest help

help:
	@echo "make apk          - build debug APK via Docker (app/build/outputs/apk/debug/)"
	@echo "make release-apk  - build unsigned release APK via Docker"
	@echo "make test         - run unit tests via Docker"
	@echo "make install                        - build in Docker, install via host adb over USB"
	@echo "make install-latest                 - download newest GitHub release APK, install via USB (no build)"
	@echo "make pair DEVICE=ip:port CODE=nnnnnn - one-time wireless adb pairing (in Docker)"
	@echo "make install-wifi DEVICE=ip:port    - build + install over wireless adb (in Docker)"
	@echo "make clean        - gradle clean via Docker"

apk:
	$(DOCKER_RUN) ./gradlew --no-daemon assembleDebug
	@echo "APK: app/build/outputs/apk/debug/app-debug.apk"

release-apk:
	$(DOCKER_RUN) ./gradlew --no-daemon assembleRelease
	@echo "APK: app/build/outputs/apk/release/app-release-unsigned.apk"

test:
	$(DOCKER_RUN) ./gradlew --no-daemon testDebugUnitTest

# Fully containerized install over wireless adb — no host adb needed.
# One-time: pair the tablet (Developer options -> Wireless debugging ->
# Pair device with pairing code), then connect+install every time after.
#
#   make pair DEVICE=192.168.1.42:37123 CODE=123456
#   make install DEVICE=192.168.1.42:41234
#
# (Pairing port and connect port are different; both are shown on the
# tablet's Wireless debugging screen.)
pair:
	@test -n "$(DEVICE)" -a -n "$(CODE)" || { echo "Usage: make pair DEVICE=<ip>:<pair-port> CODE=<6-digit-code>"; exit 1; }
	$(DOCKER_RUN) sh -c "adb pair $(DEVICE) $(CODE)"

# Default install: USB cable via host adb (Docker Desktop on Windows
# cannot pass USB through, so this is the one host-adb step).
install: apk
	"$(ADB)" install -r app/build/outputs/apk/debug/app-debug.apk
	@echo "Installed. Find 'Let's Learn' in the app drawer."

# Skip building entirely: grab the newest CI release APK and install it.
install-latest:
	curl -s https://api.github.com/repos/$(REPO)/releases/latest \
		| grep -o '"browser_download_url": *"[^"]*\.apk"' \
		| grep -o 'https[^"]*' \
		| head -1 \
		| xargs curl -L -o .latest.apk
	"$(ADB)" install -r .latest.apk
	@echo "Installed latest release. Find 'Let's Learn' in the app drawer."

# Fully containerized alternative over Wireless debugging.
install-wifi: apk
	@test -n "$(DEVICE)" || { echo "Usage: make install-wifi DEVICE=<ip>:<port>  (tablet's Wireless debugging address)"; exit 1; }
	$(DOCKER_RUN) sh -c "adb connect $(DEVICE) && adb -s $(DEVICE) install -r app/build/outputs/apk/debug/app-debug.apk"
	@echo "Installed. Find 'Let's Learn' in the app drawer."

clean:
	$(DOCKER_RUN) ./gradlew --no-daemon clean
