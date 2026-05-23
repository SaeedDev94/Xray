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

# Tools version
SDK_VERSION=$(awk -F ' ' '/compileSdk/ {print $3}' app/build.gradle.kts)
JAVA_VERSION=$(awk -F '_' '/JVM/ {print $2}' app/build.gradle.kts)

# Set vars
BUILD_TOOLS="$SDK_VERSION.1.0"
export ANDROID_HOME="/opt/android-sdk"
export JAVA_HOME="/usr/lib/jvm/java-$JAVA_VERSION-openjdk-amd64"

# Set path
export PATH="$ANDROID_HOME/platform-tools:$PATH"
export PATH="$ANDROID_HOME/build-tools/$BUILD_TOOLS:$PATH"
export PATH="$JAVA_HOME/bin:$PATH"

# Install Tools
apt-get install -y git openjdk-$JAVA_VERSION-jdk-headless sdkmanager wget unzip gcc libc-dev golang-go

# Clone repo
git clone https://github.com/SaeedDev94/Xray.git $REPO_DIR
cd $REPO_DIR
git checkout "$RELEASE_TAG"
git submodule update --init --recursive

# Build dependencies
./buildGo.sh $NATIVE_ARCH

# Setup gradle
GRADLE_DIR="$BUILD_DIR/gradle"
GRADLE_URL=$(grep distributionUrl gradle/wrapper/gradle-wrapper.properties | \
  cut -d '=' -f 2 | \
  sed 's#\\##g')
GRADLE_ARCHIVE=$(basename $GRADLE_URL)
GRADLE_VERSION=$(echo "$GRADLE_ARCHIVE" | sed -E 's/gradle-([0-9.]+)-bin\.zip/\1/')
mkdir -p $GRADLE_DIR
pushd $GRADLE_DIR
wget "$GRADLE_URL"
unzip "$GRADLE_ARCHIVE"
rm "$GRADLE_ARCHIVE"
mv * "$GRADLE_VERSION"
popd
export PATH="$GRADLE_DIR/$GRADLE_VERSION/bin:$PATH"

# Clean task
rm gradle/wrapper/gradle-wrapper.jar
gradle clean

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
