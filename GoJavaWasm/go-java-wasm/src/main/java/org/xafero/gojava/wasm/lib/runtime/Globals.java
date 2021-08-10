package org.xafero.gojava.wasm.lib.runtime;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.xafero.gojava.wasm.lib.data.JsArray;
import org.xafero.gojava.wasm.lib.data.JsDate;
import org.xafero.gojava.wasm.lib.data.JsObject;
import org.xafero.gojava.wasm.lib.data.JsUint8Array;
import org.xafero.gojava.wasm.lib.funcs.MyStaticFunc;
import org.xafero.gojava.wasm.lib.http.AbortController;
import org.xafero.gojava.wasm.lib.http.Headers;
import org.xafero.gojava.wasm.lib.http.Network;
import org.xafero.gojava.wasm.lib.internal.Crypto;

public class Globals {
	private final Map<String, Object> _values;
	private final ProcSystem _proc = new ProcSystem();
	private final FileSystem _fs = new FileSystem();
	private final Crypto _crypto = new Crypto();

	public Globals() {
		_values = new HashMap<String, Object>();
		putFunc(_values, "Object", Globals::createObject);
		putFunc(_values, "Array", Globals::createArray);
		putFunc(_values, "Uint8Array", Globals::createByteArray);
		putFunc(_values, "Date", Globals::createDate);
		putFunc(_values, "AbortController", Globals::createAbortController);
		putFunc(_values, "Headers", Globals::createHeaders);
		_values.put("process", _proc);
		_values.put("fs", _fs);
		_values.put("crypto", _crypto);
		putFunc(_values, "fetch", Network::fetch);
	}

	private static Object createObject(Object[] o) {
		return new JsObject();
	}

	private static Object createArray(Object[] o) {
		return new JsArray();
	}

	private static Object createByteArray(Object[] o) {
		return new JsUint8Array();
	}

	private static Object createDate(Object[] o) {
		return new JsDate();
	}

	private static Object createAbortController(Object[] o) {
		return new AbortController();
	}

	private static Object createHeaders(Object[] o) {
		return new Headers();
	}

	public Object get(String key) {
		Object value;
		if ((value = _values.get(key)) != null)
			return value;

		throw new NotImplementedException("Globals" + ": " + key);
	}

	@Override
	public String toString() {
		return "Globals";
	}

	private static void putFunc(Map<String, Object> map, String key, MyStaticFunc func) {
		map.put(key, func);
	}
}
