package org.xafero.gojava.wasm.lib.http;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Function;

import org.xafero.gojava.wasm.lib.Error;
import org.xafero.gojava.wasm.lib.funcs.MyStaticFunc;

import static org.xafero.gojava.wasm.lib.internal.Errors.execute;

class Promise<T> implements AutoCloseable {
	private final Object _handle;
	private final Object _task;
	private final Function<T, Object> _wrapper;

	public Promise(Object handle, Object task, Function<T, Object> wrapper) {
		_handle = handle;
		_task = task;
		_wrapper = wrapper;
	}

	@SuppressWarnings("unchecked")
	public void then(MyStaticFunc onSuccess, MyStaticFunc onFailure) {
		try {
			var discard = HttpResponse.BodyHandlers.discarding();
			var result = ((HttpClient) _handle).send((HttpRequest) _task, discard);
			var wrap = _wrapper.apply((T) result);
			execute(onSuccess, wrap);
		} catch (Exception e) {
			var err = new Error(e.getMessage());
			err.setCode(e.getClass().getName().toUpperCase());
			execute(onFailure, err);
		}
	}

	@Override
	public void close() throws Exception {
		if (_handle instanceof AutoCloseable)
			((AutoCloseable) _handle).close();
		if (_task instanceof AutoCloseable)
			((AutoCloseable) _task).close();
	}

	@Override
	public String toString() {
		return "Promise";
	}
}
