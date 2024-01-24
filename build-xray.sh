#!/bin/bash

# Update repo
apt-get update
apt-get install -y ca-certificates
echo "deb https://deb.debian.org/debian bullseye-backports main" > /etc/apt/sources.list.d/backports.list
apt-get update || apt-get update

# Install tools
apt-get install -y git openjdk-17-jdk-headless sdkmanager
sdkmanager "platforms;android-34" "build-tools;34.0.0"
sdkmanager --install "ndk;26.1.10909125" --channel=3
apt-get install -t bullseye-backports -y golang-go
apt-get install -y gcc libc-dev

# mkdir sources dir
mkdir -p /home/vagrant/build/srclib


# Build go
git clone https://github.com/golang/go.git /home/vagrant/build/srclib/go
pushd /home/vagrant/build/srclib/go
git checkout go1.21.6
cd src
./make.bash
popd

# Set vars
export PATH="/home/vagrant/build/srclib/go/bin:$PATH"
export ANDROID_HOME="/opt/android-sdk"
export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/26.1.10909125"
export ANDROID_SDK_TOOLS="$ANDROID_HOME/build-tools/34.0.0"
export GOPATH="/home/vagrant/go"
export PATH="$PATH:$GOPATH/bin"

# Build app
git clone https://github.com/SaeedDev94/Xray.git /home/vagrant/build/io.github.saeeddev94.xray
cd /home/vagrant/build/io.github.saeeddev94.xray
git submodule update --init --recursive
git checkout "$RELEASE_TAG"
VERSION_CODE=$(cat app/versionCode.txt)
((VERSION_CODE += ABI_ID))
BUILD_NAME="Xray-$RELEASE_TAG-$VERSION_CODE.apk"
pushd libXray
go install golang.org/x/mobile/cmd/gomobile@v0.0.0-20240112133503-c713f31d574b
go mod download
gomobile init
gomobile bind -o "../app/libs/libXray.aar" -androidapi 29 -target "android/$NATIVE_ARCH" -ldflags="-buildid=" -trimpath
popd
./gradlew -PabiId=$ABI_ID -PabiTarget=$ABI_TARGET assembleRelease

# Sign app
cd app/build/outputs/apk/release
echo "$KS_FILE" > /xray_base64.txt
base64 -d /xray_base64.txt > /xray.jks
$ANDROID_SDK_TOOLS/zipalign -p -f -v 4 "app-$ABI_TARGET-release-unsigned.apk" "$BUILD_NAME"
$ANDROID_SDK_TOOLS/apksigner sign --ks /xray.jks --ks-pass "pass:$KS_PASSWORD" --ks-key-alias "$KEY_ALIAS" --key-pass "pass:$KEY_PASSWORD" "$BUILD_NAME"

# Move app to dist dir
mv "$BUILD_NAME" "$DIST_DIR"
