package org.xafero.gojava.wasm.lib.http;

import java.net.http.HttpResponse;

public class FetchBody {
	private final HttpResponse<?> _message;
	private final FetchResponse _parent;

	public FetchBody(FetchResponse parent, HttpResponse<?> message) {
		_parent = parent;
		_message = message;
	}

	public BodyReader getReader() {
		return new BodyReader(_parent, _message);
	}
}
