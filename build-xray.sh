#!/bin/bash

# Update repo
apt-get update
apt-get install -y ca-certificates
echo "deb https://deb.debian.org/debian bookworm-backports main" > /etc/apt/sources.list.d/backports.list
apt-get update || apt-get update
apt-get dist-upgrade -y

# Tools version
ANDROID_PLATFORM_VERSION="android-34"
ANDROID_SDK_VERSION="34.0.0"
ANDROID_NDK_VERSION="26.2.11394342"
JAVA_VERSION="17"
GRADLE_VERSION="8.7"
GO_VERSION="go1.22.4"
GO_MOBILE_VERSION="v0.0.0-20240604190613-2782386b8afd"

# Install Tools
apt-get install -t bookworm-backports -y golang-go
apt-get install -y git openjdk-$JAVA_VERSION-jdk-headless sdkmanager wget unzip gcc libc-dev
sdkmanager "platform-tools" "platforms;$ANDROID_PLATFORM_VERSION" "build-tools;$ANDROID_SDK_VERSION"
sdkmanager --install "ndk;$ANDROID_NDK_VERSION" --channel=3

# Define dirs
HOME_DIR="/home/vagrant"
BUILD_DIR="$HOME_DIR/build"
REPO_DIR="$BUILD_DIR/io.github.saeeddev94.xray"
GRADLE_DIR="$BUILD_DIR/gradle"
SRC_DIR="$BUILD_DIR/srclib"
GO_ROOT_DIR="$SRC_DIR/go"
GO_PATH_DIR="$HOME_DIR/go"

# Create directories
mkdir -p $GRADLE_DIR
mkdir -p $SRC_DIR

# Download gradle
pushd $GRADLE_DIR
GRADLE_ARCHIVE="gradle-$GRADLE_VERSION-bin.zip"
wget "https://services.gradle.org/distributions/$GRADLE_ARCHIVE"
unzip "$GRADLE_ARCHIVE"
rm "$GRADLE_ARCHIVE"
mv * "$GRADLE_VERSION"
popd

# Build go
git clone https://github.com/golang/go.git $GO_ROOT_DIR
pushd $GO_ROOT_DIR
git checkout "$GO_VERSION"
cd src
./make.bash
popd

# Set vars
export JAVA_HOME="/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64"
export ANDROID_HOME="/opt/android-sdk"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/$ANDROID_NDK_VERSION"
export GOROOT="$GO_ROOT_DIR"
export GOPATH="$GO_PATH_DIR"

# Set path
export PATH="$JAVA_HOME/bin:$PATH"
export PATH="$GRADLE_DIR/$GRADLE_VERSION/bin:$PATH"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/build-tools/$ANDROID_SDK_VERSION:$PATH"
export PATH="$GOROOT/bin:$PATH"
export PATH="$GOPATH/bin:$PATH"

# Clone repo
git clone https://github.com/SaeedDev94/Xray.git $REPO_DIR
cd $REPO_DIR
git submodule update --init --recursive
git checkout "$RELEASE_TAG"

# Clean task
rm gradle/wrapper/gradle-wrapper.jar
cd app
gradle clean

# Build XrayCore
pushd ../XrayCore
go install golang.org/x/mobile/cmd/gomobile@$GO_MOBILE_VERSION
go mod download
gomobile init
gomobile bind -o "../app/libs/XrayCore.aar" -androidapi 26 -target "android/$NATIVE_ARCH" -ldflags="-buildid=" -trimpath
popd

# Build app
gradle -PabiId=$ABI_ID -PabiTarget=$ABI_TARGET assembleRelease

# Sign app
VERSION_CODE=$(cat versionCode.txt)
((VERSION_CODE += ABI_ID))
BUILD_NAME="Xray-$RELEASE_TAG-$VERSION_CODE.apk"
cd build/outputs/apk/release
echo "$KS_FILE" > /tmp/xray_base64.txt
base64 -d /tmp/xray_base64.txt > /tmp/xray.jks
zipalign -p -f -v 4 "app-$ABI_TARGET-release-unsigned.apk" "$BUILD_NAME"
apksigner sign --ks /tmp/xray.jks --ks-pass "pass:$KS_PASSWORD" --ks-key-alias "$KEY_ALIAS" --key-pass "pass:$KEY_PASSWORD" "$BUILD_NAME"
rm /tmp/xray_base64.txt /tmp/xray.jks

# Move app to dist dir
mv "$BUILD_NAME" "$DIST_DIR"
