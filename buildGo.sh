#!/bin/bash

# Tools version
SDK_VERSION=$(awk -F ' ' '/compileSdk/ {print $3}' app/build.gradle.kts)
NDK_VERSION=$(awk -F '"' '/ndkVersion/ {print $2}' app/build.gradle.kts)

# Set vars
PLATFORM="android-$SDK_VERSION"
BUILD_TOOLS="$SDK_VERSION.1.0"
export GOROOT="$(realpath go-root)"
export GOPATH="$(realpath go-path)"

# Set path
export PATH="$GOROOT/bin:$PATH"
export PATH="$GOPATH/bin:$PATH"

# Setup SDK & NDK
sdkmanager "platform-tools" "platforms;$PLATFORM" "build-tools;$BUILD_TOOLS"
sdkmanager --install "ndk;$NDK_VERSION" --channel=3

git clone https://github.com/golang/go.git $GOROOT
pushd $GOROOT
git checkout "go$(sed -n -E 's/^go (.*)/\1/p' ../XrayCore/go.mod)"
cd src
./make.bash
popd

./buildXrayCore.sh $1
./buildXrayHelper.sh $1
