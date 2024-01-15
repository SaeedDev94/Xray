#!/bin/bash

DEST="../app/libs"

prepare_go() {
  go install golang.org/x/mobile/cmd/gomobile@latest
  go mod download
}

build_android() {
  rm -f "$DEST/*.jar"
  rm -f "$DEST/*.aar"
  gomobile init
  gomobile bind -o "$DEST/libXray.aar" -target android -androidapi 29
}

cd libXray
prepare_go
build_android
