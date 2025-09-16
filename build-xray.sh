#!/bin/bash

# Update repo
apt-get update
apt-get install -y ca-certificates
echo "deb https://deb.debian.org/debian bookworm-backports main" > /etc/apt/sources.list.d/backports.list
apt-get update || apt-get update
apt-get dist-upgrade -y

# Gradle version
GRADLE_URL=$(grep distributionUrl gradle/wrapper/gradle-wrapper.properties | \
  cut -d '=' -f 2 | \
  sed 's#\\##g')
GRADLE_ARCHIVE=$(basename $GRADLE_URL)
GRADLE_VERSION=$(echo "$GRADLE_ARCHIVE" | sed -E 's/gradle-([0-9.]+)-bin\.zip/\1/')

# Tools version
ANDROID_PLATFORM_VERSION="android-35"
ANDROID_SDK_VERSION="35.0.0"
JAVA_VERSION="17"

# Install Tools
apt-get install -t bookworm-backports -y golang-go
apt-get install -y git openjdk-$JAVA_VERSION-jdk-headless sdkmanager wget unzip gcc libc-dev
sdkmanager "platform-tools" "platforms;$ANDROID_PLATFORM_VERSION" "build-tools;$ANDROID_SDK_VERSION"

# Define dirs
HOME_DIR="/home/vagrant"
BUILD_DIR="$HOME_DIR/build"
REPO_DIR="$BUILD_DIR/io.github.saeeddev94.xray"
GRADLE_DIR="$BUILD_DIR/gradle"

# Set vars
export JAVA_HOME="/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64"
export ANDROID_HOME="/opt/android-sdk"

# Set path
export PATH="$JAVA_HOME/bin:$PATH"
export PATH="$GRADLE_DIR/$GRADLE_VERSION/bin:$PATH"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/build-tools/$ANDROID_SDK_VERSION:$PATH"

# Download gradle
mkdir -p $GRADLE_DIR
pushd $GRADLE_DIR
wget "$GRADLE_URL"
unzip "$GRADLE_ARCHIVE"
rm "$GRADLE_ARCHIVE"
mv * "$GRADLE_VERSION"
popd

# Clone repo
git clone https://github.com/SaeedDev94/Xray.git $REPO_DIR
cd $REPO_DIR
git checkout "$RELEASE_TAG"
git submodule update --init --recursive

# Clean task
rm gradle/wrapper/gradle-wrapper.jar
gradle clean

# Build dependencies
./buildGo.sh $NATIVE_ARCH

# Build app
echo "$KS_FILE" > /tmp/xray_base64.txt
base64 -d /tmp/xray_base64.txt > /tmp/xray.jks
gradle -PabiId=$ABI_ID -PabiTarget=$ABI_TARGET assembleRelease
rm /tmp/xray_base64.txt /tmp/xray.jks

# Build name
VERSION_CODE=$(cat app/versionCode.txt)
((VERSION_CODE += ABI_ID))
BUILD_NAME="Xray-$RELEASE_TAG-$VERSION_CODE.apk"
mv "app/build/outputs/apk/release/app-$ABI_TARGET-release.apk" "$DIST_DIR/$BUILD_NAME"
