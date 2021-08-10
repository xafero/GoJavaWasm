package org.xafero.gojava.wasm.lib.http;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;

public class BodyReader {
	private final HttpResponse<?> _message;
	private final FetchResponse _parent;
	private final byte[] _task;
	private boolean[] _done;

	public BodyReader(FetchResponse parent, HttpResponse<?> message) {
		_parent = parent;
		_message = message;
		_task = (byte[]) _message.body();
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
		// _task.close();
	}
}
