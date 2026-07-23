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
	@echo "make pair DEVICE=ip:port CODE=nnnnnn - one-time wireless adb pairing (in Docker)"
	@echo "make install DEVICE=ip:port         - build + install over wireless adb (in Docker)"
	@echo "make install-usb                    - build in Docker, install via host adb over USB"
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

install: apk
	@test -n "$(DEVICE)" || { echo "Usage: make install DEVICE=<ip>:<port>  (tablet's Wireless debugging address)"; exit 1; }
	$(DOCKER_RUN) sh -c "adb connect $(DEVICE) && adb -s $(DEVICE) install -r app/build/outputs/apk/debug/app-debug.apk"
	@echo "Installed. Find 'Let's Learn' in the app drawer."

# Fallback for USB cable installs; the one target that uses host adb,
# because Docker Desktop on Windows cannot pass USB through.
install-usb: apk
	adb install -r app/build/outputs/apk/debug/app-debug.apk

clean:
	$(DOCKER_RUN) ./gradlew --no-daemon clean
