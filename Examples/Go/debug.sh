#!/bin/sh
echo " ::: Run natively..."
go run main.go
echo " ::: Compiling to WASM..."
GOOS=js GOARCH=wasm go build -o main.wasm
echo " ::: Run with java..."
java \
   -Dorg.slf4j.simpleLogger.log.io.github.kawamuray.wasmtime.Func=info \
   -Dorg.slf4j.simpleLogger.log.io.github.kawamuray.wasmtime.NativeLibraryLoader=info \
   -Dorg.slf4j.simpleLogger.defaultLogLevel=trace \
   -Dorg.slf4j.simpleLogger.showThreadName=false \
   -Dorg.slf4j.simpleLogger.showLogName=false \
   -jar ../../../GoJavaWasm/wasm-exec/target/wasm-exec-1.0-SNAPSHOT.jar ./main.wasm
echo " ::: Done."
