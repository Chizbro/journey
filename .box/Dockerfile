# The toolchain this repo needs and box's base image doesn't carry.
#
# Tools only. No repo code is copied in here — the clone happens inside the
# Box, from the Forge, which is the only way code reaches a Box at all.
FROM box/base

USER root

# app/build.gradle.kts asks for jvmTarget 17, which AGP 8.7 and Kotlin
# 2.0.21 are happy with.
RUN apt-get update \
    && apt-get install -y --no-install-recommends default-jdk-headless \
    && rm -rf /var/lib/apt/lists/*
ENV JAVA_HOME=/usr/lib/jvm/default-java

# Android command-line tools, then exactly the pieces app/build.gradle.kts
# names: compileSdk 35, so platform 35 and the matching build-tools.
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
RUN mkdir -p "$ANDROID_HOME/cmdline-tools" \
    && curl -fsSLo /tmp/tools.zip \
         https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
    && unzip -q /tmp/tools.zip -d "$ANDROID_HOME/cmdline-tools" \
    && mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" \
    && rm /tmp/tools.zip \
    && yes | sdkmanager --licenses > /dev/null \
    && sdkmanager --install "platform-tools" "platforms;android-35" "build-tools;35.0.0" > /dev/null \
    && chown -R agent "$ANDROID_HOME"

# What this Box can and can't do, so nobody wastes an afternoon finding out:
# it compiles, runs unit tests, and lints. It cannot run the app. The Android
# emulator needs KVM, and Docker Desktop on Apple Silicon has none to give.
# An APK is the artifact; a running screen isn't available here.
USER agent
