#!/bin/bash

TARGET="$1"
ARCHS=(arm arm64 386 amd64)
DEST="../app/libs"

is_in_array() {
  local value="$1"
  local array=("${@:2}")

  for item in "${array[@]}"; do
    if [[ "$item" == "$value" ]]; then
      return 0
    fi
  done

  return 1
}

check_target() {
  if ! is_in_array "$TARGET" "${ARCHS[@]}"; then
    echo "Not supported"
    exit 1
  fi
}

prepare_go() {
  echo "Install dependencies"
  cd XrayCore
  # rm go*
  # go mod init XrayCore
  # go mod tidy
  # go get golang.org/x/mobile
  go install golang.org/x/mobile/cmd/gomobile@v0.0.0-20240707233753-b765e5d5218f
  go mod download
}

build_android() {
  echo "Building XrayCore for $TARGET"
  rm -f "$DEST/XrayCore*"
  gomobile init
  gomobile bind -o "$DEST/XrayCore.aar" -androidapi 26 -target "android/$TARGET" -ldflags="-buildid=" -trimpath
}

refresh_dependencies() {
  echo "Gradle: refresh dependencies"
  cd ..
  ./gradlew --refresh-dependencies clean
}

check_target
prepare_go
build_android
refresh_dependencies
