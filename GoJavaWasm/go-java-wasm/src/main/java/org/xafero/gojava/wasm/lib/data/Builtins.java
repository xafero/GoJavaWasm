package org.xafero.gojava.wasm.lib.data;

public class Builtins {
	private static boolean isJsNull(Object obj) {
		return obj instanceof JsNull;
	}

	public static boolean isJsUndefined(Object obj) {
		return obj == null || obj instanceof JsUndefined;
	}

	public static boolean isUndefinedOrNull(Object obj) {
		return isJsNull(obj) || isJsUndefined(obj);
	}
}
