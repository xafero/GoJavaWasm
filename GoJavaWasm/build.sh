#!/bin/sh

echo " ::: Building library..."
cd go-java-wasm
mvn clean package install
cd ..

echo " ::: Building jar..."
cd wasm-exec
mvn clean package
cd ..

echo " ::: Done."
