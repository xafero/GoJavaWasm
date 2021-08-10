package org.xafero.gojava.wasm.lib.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;

class BodyReader {
	private final HttpResponse<?> _message;
	private final FetchResponse _parent;
	private final InputStream _task;
	private boolean[] _done;

	public BodyReader(FetchResponse parent, HttpResponse<?> message) {
		_parent = parent;
		_message = message;
		_task = (InputStream) _message.body();
		_done = new boolean[] { false };
	}

	public Promise<Byte[]> read() {
		Function<Byte[], Object> func = (a) -> {
			return wrap(a);
		};
		return new Promise<Byte[]>(_parent, _task, func);
	}

	private Object wrap(Byte[] arg) {
		var bytes = ArrayUtils.toPrimitive(arg);
		return new ReadResult(bytes, _done);
	}

	public void cancel() throws IOException {
		_task.close();
	}
}
