package org.xafero.gojava.wasm.lib.internal;

import org.xafero.gojava.wasm.lib.Error;
import org.xafero.gojava.wasm.lib.funcs.MyInstanceFunc;
import org.xafero.gojava.wasm.lib.funcs.MyStaticFunc;

public class Errors {

	public static Error newEnoSys() {
		var err = new Error("not implemented");
		err.setCode("ENOSYS");
		return err;
	}

	public static Object execute(MyInstanceFunc call, Object instance, Object[] args) {
		try {
			return call.apply(instance, args);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}

	public static Object execute(MyStaticFunc call, Object... args) {
		try {
			return call.apply(args);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
}
