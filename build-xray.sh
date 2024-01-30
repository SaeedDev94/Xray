#!/bin/bash

# Update repo
apt-get update
apt-get install -y ca-certificates
echo "deb https://deb.debian.org/debian bullseye-backports main" > /etc/apt/sources.list.d/backports.list
apt-get update || apt-get update
apt-get dist-upgrade -y

# Tools version
ANDROID_PLATFORM_VERSION="android-34"
ANDROID_SDK_VERSION="34.0.0"
ANDROID_NDK_VERSION="26.1.10909125"
JAVA_VERSION="17"
GRADLE_VERSION="8.2.1"
GO_VERSION="go1.21.6"
GO_MOBILE_VERSION="v0.0.0-20240112133503-c713f31d574b"

# Install Tools
apt-get install -t bullseye-backports -y golang-go
apt-get install -y git openjdk-$JAVA_VERSION-jdk-headless sdkmanager wget unzip gcc libc-dev
sdkmanager "platform-tools" "platforms;$ANDROID_PLATFORM_VERSION" "build-tools;$ANDROID_SDK_VERSION"
sdkmanager --install "ndk;$ANDROID_NDK_VERSION" --channel=3

# Create directories
mkdir -p /home/vagrant/gradle
mkdir -p /home/vagrant/build/srclib

# Download gradle
pushd /home/vagrant/gradle
GRADLE_ARCHIVE="gradle-$GRADLE_VERSION-bin.zip"
wget "https://services.gradle.org/distributions/$GRADLE_ARCHIVE"
unzip "$GRADLE_ARCHIVE"
rm "$GRADLE_ARCHIVE"
mv * "$GRADLE_VERSION"
popd

# Build go
git clone https://github.com/golang/go.git /home/vagrant/build/srclib/go
pushd /home/vagrant/build/srclib/go
git checkout "$GO_VERSION"
cd src
./make.bash
popd

# Set vars
export JAVA_HOME="/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64"
export ANDROID_HOME="/opt/android-sdk"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/$ANDROID_NDK_VERSION"
export GOPATH="/home/vagrant/build/srclib/go"

# Set path
export PATH="$JAVA_HOME/bin:$PATH"
export PATH="/home/vagrant/gradle/$GRADLE_VERSION/bin:$PATH"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/build-tools/$ANDROID_SDK_VERSION:$PATH"
export PATH="$GOPATH/bin:$PATH"

# Clone repo
git clone https://github.com/SaeedDev94/Xray.git /home/vagrant/build/io.github.saeeddev94.xray
cd /home/vagrant/build/io.github.saeeddev94.xray
git submodule update --init --recursive
git checkout "$RELEASE_TAG"

# Clean task
rm gradle/wrapper/gradle-wrapper.jar
cd app
gradle clean

# Build libXray
pushd ../libXray
go install golang.org/x/mobile/cmd/gomobile@$GO_MOBILE_VERSION
go mod download
gomobile init
gomobile bind -o "../app/libs/libXray.aar" -androidapi 29 -target "android/$NATIVE_ARCH" -ldflags="-buildid=" -trimpath
popd

# Build app
gradle -PabiId=$ABI_ID -PabiTarget=$ABI_TARGET assembleRelease

# Sign app
VERSION_CODE=$(cat versionCode.txt)
((VERSION_CODE += ABI_ID))
BUILD_NAME="Xray-$RELEASE_TAG-$VERSION_CODE.apk"
cd build/outputs/apk/release
echo "$KS_FILE" > /xray_base64.txt
base64 -d /xray_base64.txt > /xray.jks
zipalign -p -f -v 4 "app-$ABI_TARGET-release-unsigned.apk" "$BUILD_NAME"
apksigner sign --ks /xray.jks --ks-pass "pass:$KS_PASSWORD" --ks-key-alias "$KEY_ALIAS" --key-pass "pass:$KEY_PASSWORD" "$BUILD_NAME"

# Move app to dist dir
mv "$BUILD_NAME" "$DIST_DIR"
