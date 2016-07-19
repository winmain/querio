#!/bin/bash

function make-bintray-credentials {
  if [ -z "$BINTRAY_USER" ] || [ -z "$BINTRAY_API_KEY" ]; then
    echo "Env BINTRAY_USER and BINTRAY_API_KEY must be set"
    exit -1
  fi
  mkdir $HOME/.bintray/
  FILE=$HOME/.bintray/.credentials
  cat <<EOF >$FILE
realm = Bintray API Realm
host = api.bintray.com
user = $BINTRAY_USER
password = $BINTRAY_API_KEY
EOF
  echo "Created ~/.bintray/.credentials file for $BINTRAY_USER"
}

make-bintray-credentials
sbt publish
