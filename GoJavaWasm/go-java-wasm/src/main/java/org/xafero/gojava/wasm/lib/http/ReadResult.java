package org.xafero.gojava.wasm.lib.http;

import org.xafero.gojava.wasm.lib.data.JsUint8Array;

public class ReadResult {
	private final byte[] _bytes;
	private final JsUint8Array _value;
	private final boolean[] _done;

	public ReadResult(byte[] bytes, boolean[] done) {
		_bytes = bytes;
		_value = new JsUint8Array(_bytes);
		_done = done;
	}

	public Object getValue() {
		var val = _value;
		setDone(true);
		return val;
	}

	public boolean getDone() {
		return _done[0];
	}

	public void setDone(boolean value) {
		_done[0] = value;
	}
}
