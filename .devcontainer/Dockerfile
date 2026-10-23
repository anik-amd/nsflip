FROM mcr.microsoft.com/devcontainers/base:ubuntu-22.04

# Builds/tests run inside Codespaces — the Android SDK lives here, never locally.
ARG JDK_VERSION=17
ENV JAVA_HOME=/usr/lib/jvm/java-${JDK_VERSION}-openjdk-amd64
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH="${JAVA_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

# JDK
RUN apt-get update \
    && apt-get install -y --no-install-recommends openjdk-${JDK_VERSION}-jdk unzip wget \
    && rm -rf /var/lib/apt/lists/*

# Android cmdline-tools + SDK pieces needed to build (minimal footprint)
ARG CMDLINE_TOOLS_VERSION=11076708
ARG PLATFORM=35
ARG BUILD_TOOLS=35.0.0

RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && wget -q "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip" -O /tmp/clt.zip \
    && unzip -q /tmp/clt.zip -d ${ANDROID_HOME}/cmdline-tools \
    && mv ${ANDROID_HOME}/cmdline-tools/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /tmp/clt.zip \
    && yes | sdkmanager --licenses > /dev/null \
    && sdkmanager "platforms;android-${PLATFORM}" "build-tools;${BUILD_TOOLS}" "platform-tools" > /dev/null

# vscode user from the base image; /opt/android-sdk must be writable by them
RUN chown -R vscode:vscode ${ANDROID_HOME} 2>/dev/null || true

USER vscode
