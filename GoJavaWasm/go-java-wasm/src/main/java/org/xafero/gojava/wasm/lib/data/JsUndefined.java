package org.xafero.gojava.wasm.lib.data;

public class JsUndefined {

	private JsUndefined() {
	}

	public static final JsUndefined S = new JsUndefined();

	@Override
	public String toString() {
		return "JsUndefined";
	}
}
