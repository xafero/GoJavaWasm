package org.xafero.gojava.wasm.lib.http;

import java.net.http.HttpHeaders;
import java.util.HashMap;

import org.xafero.gojava.wasm.lib.data.JsIterator;

public class Headers extends HashMap<String, String> {

	private static final long serialVersionUID = -223995638057685838L;

	public Headers() {
	}

	public Headers(HttpHeaders responseHeaders) {
		for (var header : responseHeaders.map().entrySet())
			this.put(header.getKey(), String.join(" ", header.getValue()).trim());
	}

	public JsIterator entries() {
		return new JsIterator(this.entrySet().iterator());
	}

	public void append(String key, String value) {
		this.put(key, value);
	}

	@Override
	public String toString() {
		return "Headers";
	}
}
