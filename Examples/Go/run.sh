#!/bin/sh
echo " ::: Run natively..."
go run main.go
echo " ::: Compiling to WASM..."
GOOS=js GOARCH=wasm go build -o main.wasm
echo " ::: Run with java..."
java -jar ../../../GoJavaWasm/wasm-exec/target/wasm-exec-1.0-SNAPSHOT.jar ./main.wasm
echo " ::: Done."
