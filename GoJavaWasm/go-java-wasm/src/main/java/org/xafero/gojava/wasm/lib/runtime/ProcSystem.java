package org.xafero.gojava.wasm.lib.runtime;

import java.nio.file.Paths;

public class ProcSystem {

	public static long getNanoTime() {
		var nano = System.nanoTime();
		return nano;
	}

	public static String cwd() {
		var path = Paths.get("");
		var abs = path.toAbsolutePath();
		var dir = abs.toString();
		return dir;
	}

	public long getPid() {
		return ProcessHandle.current().pid();
	}

	@Override
	public String toString() {
		return "ProcSystem";
	}
}
