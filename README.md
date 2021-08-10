# GoJavaWasm
A Java project for running Go(lang)'s WebAssembly code

## Compile your Go
* cd Examples/Go/Hello
* GOOS=js GOARCH=wasm go build -o main.wasm

## Run it in Java
* java -jar wasm-exec.jar ./main.wasm

## Acknowledgements
* Google for the language
* Go's community in general
* Yuto Kawamura for Wasmtime
