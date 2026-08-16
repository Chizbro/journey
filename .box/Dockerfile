# The toolchain this repo needs, on an image that exists for x86_64.
#
# Not FROM box/base, and not by preference: Google ships AAPT2 — Android's
# resource compiler — as an x86_64-only Linux binary. On an ARM Mac the build
# gets all the way to compiling resources and then dies inside Rosetta,
# looking for a dynamic linker that isn't there. box/base exists only for the
# machine box runs on, so the Blueprint brings its own base and box appends
# its plumbing to it. See platform in box.toml.
#
# Tools only. No repo code is copied in — the clone happens inside the Box,
# from the Forge, which is the only way code reaches a Box at all.
FROM eclipse-temurin:17-jdk

USER root

# curl, unzip and git are box's plumbing prerequisites and the SDK
# installer's; the JDK is the image's own, and matches jvmTarget 17.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl unzip \
    && rm -rf /var/lib/apt/lists/*

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
    && chmod -R a+rX "$ANDROID_HOME"

# What this Box can and can't do, so nobody wastes an afternoon finding out:
# it compiles, runs unit tests, and lints. It cannot run the app. The Android
# emulator needs KVM, and Docker Desktop on Apple Silicon has none to give.
# An APK is the artifact; a running screen isn't available here.
