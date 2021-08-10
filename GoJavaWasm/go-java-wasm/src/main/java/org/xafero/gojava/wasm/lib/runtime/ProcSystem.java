package org.xafero.gojava.wasm.lib.runtime;

import java.nio.file.Paths;

public class ProcSystem {

	public static long getNanoTime() {
		var nano = System.nanoTime();
		return nano;
	}

	public static String cwd() {
		var dir = Paths.get("").toAbsolutePath().toString();
		return dir;
	}

	public long pid() {
		return ProcessHandle.current().pid();
	}

	@Override
	public String toString() {
		return "ProcSystem";
	}
}
