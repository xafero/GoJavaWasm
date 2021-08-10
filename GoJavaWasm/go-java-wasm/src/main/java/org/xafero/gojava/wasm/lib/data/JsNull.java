package org.xafero.gojava.wasm.lib.data;

public class JsNull {
	private JsNull() {
	}

	public static final JsNull S = new JsNull();

	@Override
	public String toString() {
		return "JsNull";
	}
}
