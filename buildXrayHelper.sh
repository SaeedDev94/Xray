#!/bin/bash

TARGET="$1"
ARCHS=(arm arm64 386 amd64)
DEST="../app/src/main/assets"

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
  go mod download
}

build_android() {
  echo "Building XrayHelper for $TARGET"
  local OUTPUT="$DEST/xrayhelper"
  rm -f $OUTPUT
  CGO_ENABLED=0 GOOS=linux GOARCH=$TARGET \
    go build -v -o $OUTPUT \
    -ldflags "-s -w -buildid=" -buildvcs=false -trimpath ./main
}

check_target

pushd XrayHelper
prepare_go
build_android
popd
