#! /bin/bash

MVN_VERSION=$(mvn -q \
    -Dexec.executable="echo" \
    -Dexec.args='${project.version}' \
    --non-recursive \
    org.codehaus.mojo:exec-maven-plugin:1.3.1:exec)
echo "$MVN_VERSION"

git config --global user.email $EMAIL
git config --global user.name $USER

git tag -a $MVN_VERSION -m '$MVN_VERSION tag'