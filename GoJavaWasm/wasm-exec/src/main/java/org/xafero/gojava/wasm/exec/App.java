package org.xafero.gojava.wasm.exec;

import java.io.File;

import org.slf4j.LoggerFactory;
import org.xafero.gojava.wasm.lib.Go;

import io.github.kawamuray.wasmtime.Module;

public class App {

	public static void main(String[] args) throws Exception {
		if (args == null || args.length < 1) {
			System.out.println("usage: " + "wasm-exec" + " [wasm binary] [arguments]");
			return;
		}
		var wasmFile = new File(args[0]).getAbsolutePath();
		var logger = LoggerFactory.getLogger(Go.class);
		try (var go = new Go(logger)) {
			go.prepare();
			go.addDefaultImports();
			go.create(e -> Module.fromFile(e, wasmFile));
			go.importObject();
			go.instantiate();
			go.load();
			go.run();
		}
	}
}
