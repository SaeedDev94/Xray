#!/bin/bash

# Update repo
apt-get update
apt-get install -y ca-certificates
echo "deb https://deb.debian.org/debian bookworm-backports main" > /etc/apt/sources.list.d/backports.list
apt-get update || apt-get update
apt-get dist-upgrade -y

# Tools version
ANDROID_PLATFORM_VERSION="android-35"
ANDROID_SDK_VERSION="35.0.0"
ANDROID_NDK_VERSION="28.2.13676358"
JAVA_VERSION="17"
GRADLE_VERSION="8.14.3"

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

# Set vars
export JAVA_HOME="/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64"
export ANDROID_HOME="/opt/android-sdk"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/$ANDROID_NDK_VERSION"

# Set path
export PATH="$JAVA_HOME/bin:$PATH"
export PATH="$GRADLE_DIR/$GRADLE_VERSION/bin:$PATH"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/build-tools/$ANDROID_SDK_VERSION:$PATH"

# Download gradle
mkdir -p $GRADLE_DIR
pushd $GRADLE_DIR
GRADLE_ARCHIVE="gradle-$GRADLE_VERSION-bin.zip"
wget "https://services.gradle.org/distributions/$GRADLE_ARCHIVE"
unzip "$GRADLE_ARCHIVE"
rm "$GRADLE_ARCHIVE"
mv * "$GRADLE_VERSION"
popd

# Clone repo
git clone https://github.com/SaeedDev94/Xray.git $REPO_DIR
cd $REPO_DIR
git checkout "$RELEASE_TAG"
git submodule update --init --recursive

# Build dependencies
./buildGo.sh $NATIVE_ARCH

# Build app
gradle -PabiId=$ABI_ID -PabiTarget=$ABI_TARGET assembleRelease

# Sign app
VERSION_CODE=$(cat app/versionCode.txt)
((VERSION_CODE += ABI_ID))
BUILD_NAME="Xray-$RELEASE_TAG-$VERSION_CODE.apk"
cd app/build/outputs/apk/release
echo "$KS_FILE" > /tmp/xray_base64.txt
base64 -d /tmp/xray_base64.txt > /tmp/xray.jks
zipalign -p -f -v 16 "app-$ABI_TARGET-release-unsigned.apk" "$BUILD_NAME"
apksigner sign --ks /tmp/xray.jks --ks-pass "pass:$KS_PASSWORD" --ks-key-alias "$KEY_ALIAS" --key-pass "pass:$KEY_PASSWORD" "$BUILD_NAME"
rm /tmp/xray_base64.txt /tmp/xray.jks

# Move app to dist dir
mv "$BUILD_NAME" "$DIST_DIR"
