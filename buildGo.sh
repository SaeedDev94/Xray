#!/bin/bash

# Tools version
SDK_VERSION=$(awk -F ' ' '/compileSdk/ {print $3}' app/build.gradle.kts)
NDK_VERSION=$(awk -F '"' '/ndkVersion/ {print $2}' app/build.gradle.kts)
GO_VERSION=$(awk -F ' ' '/^go / {print $2}' XrayCore/go.mod)

# Set vars
export GOPATH="$(realpath go-path)"
export GOTOOLCHAIN="go$GO_VERSION"

# Set path
export PATH="$GOPATH/bin:$PATH"

# Setup SDK & NDK
sdkmanager "platform-tools" "platforms;android-$SDK_VERSION.0" "build-tools;$SDK_VERSION.0.0"
sdkmanager --install "ndk;$NDK_VERSION" --channel=3

# Accept licenses
yes | sdkmanager --licenses

./buildXrayCore.sh $1
./buildXrayHelper.sh $1
