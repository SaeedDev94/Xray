#!/bin/bash

# Update repo
apt-get update
apt-get install -y ca-certificates
echo "deb https://deb.debian.org/debian forky main" > /etc/apt/sources.list.d/forky.list
apt-get update || apt-get update
apt-get dist-upgrade -y

# Define dirs
HOME_DIR="/home/vagrant"
BUILD_DIR="$HOME_DIR/build"
REPO_DIR="$BUILD_DIR/io.github.saeeddev94.xray"

# Clone repo
apt-get install -y git
git clone https://github.com/SaeedDev94/Xray.git $REPO_DIR
cd $REPO_DIR
git checkout "$RELEASE_TAG"
git submodule update --init --recursive

# Tools version
SDK_VERSION=$(awk -F ' ' '/compileSdk/ {print $3}' app/build.gradle.kts)
JAVA_VERSION=$(awk -F '_' '/JVM/ {print $2}' app/build.gradle.kts)

# Set vars
export ANDROID_HOME="/opt/android-sdk"
export JAVA_HOME="/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64"

# Set path
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/build-tools/$SDK_VERSION.0.0:$PATH"
export PATH="$JAVA_HOME/bin:$PATH"

# Install Tools
apt-get install -y openjdk-$JAVA_VERSION-jdk-headless sdkmanager golang-go gcc libc-dev

# Build dependencies
./buildGo.sh $NATIVE_ARCH

# Setup gradle
./gradlew clean
./gradlew --version

# Build app
echo "$KS_FILE" > /tmp/xray_base64.txt
base64 -d /tmp/xray_base64.txt > /tmp/xray.jks
./gradlew -PabiId=$ABI_ID -PabiTarget=$ABI_TARGET assembleRelease
rm /tmp/xray_base64.txt /tmp/xray.jks

# Build name
VERSION_CODE=$(cat app/versionCode.txt)
((VERSION_CODE += ABI_ID))
BUILD_NAME="Xray-$RELEASE_TAG-$VERSION_CODE.apk"
mv "app/build/outputs/apk/release/app-$ABI_TARGET-release.apk" "$DIST_DIR/$BUILD_NAME"
