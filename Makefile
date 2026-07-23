# Build the app with zero local dependencies beyond Docker.
# The cirruslabs image ships JDK 17 + Android SDK 34; the Gradle wrapper
# supplies Gradle itself. Gradle caches land in .gradle-docker/ so repeat
# builds are fast.

ANDROID_IMAGE := ghcr.io/cirruslabs/android-sdk:34
DOCKER_RUN := docker run --rm \
	-v "$(CURDIR)":/work \
	-w /work \
	-e GRADLE_USER_HOME=/work/.gradle-docker \
	$(ANDROID_IMAGE)

.PHONY: apk release-apk test clean install help

help:
	@echo "make apk          - build debug APK via Docker (app/build/outputs/apk/debug/)"
	@echo "make release-apk  - build unsigned release APK via Docker"
	@echo "make test         - run unit tests via Docker"
	@echo "make install      - build then install onto the connected device via adb"
	@echo "make clean        - gradle clean via Docker"

apk:
	$(DOCKER_RUN) ./gradlew --no-daemon assembleDebug
	@echo "APK: app/build/outputs/apk/debug/app-debug.apk"

release-apk:
	$(DOCKER_RUN) ./gradlew --no-daemon assembleRelease
	@echo "APK: app/build/outputs/apk/release/app-release-unsigned.apk"

test:
	$(DOCKER_RUN) ./gradlew --no-daemon testDebugUnitTest

# adb runs on the HOST (USB passthrough into Docker isn't worth the pain).
# Needs platform-tools on PATH and USB debugging enabled on the device.
install: apk
	adb install -r app/build/outputs/apk/debug/app-debug.apk
	@echo "Installed. Find 'Let's Learn' in the app drawer."

clean:
	$(DOCKER_RUN) ./gradlew --no-daemon clean
