#!/bin/bash

# Set vars
export GOROOT="$(realpath go-root)"
export GOPATH="$(realpath go-path)"

# Set path
export PATH="$GOROOT/bin:$PATH"
export PATH="$GOPATH/bin:$PATH"

git clone https://github.com/golang/go.git $GOROOT
pushd $GOROOT
git checkout "go$(sed -n -E 's/^go (.*)/\1/p' ../XrayCore/go.mod)"
cd src
./make.bash
popd

./buildXrayCore.sh $1
./buildXrayHelper.sh $1
