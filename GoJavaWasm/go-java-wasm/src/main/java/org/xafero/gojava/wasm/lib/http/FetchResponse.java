package org.xafero.gojava.wasm.lib.http;

import java.net.http.HttpResponse;

class FetchResponse implements AutoCloseable {
	private final HttpResponse<?> _message;

	private Headers headers;
	private FetchBody body;
	private FetchBuffer arrayBuffer;
	private int status;

	public FetchResponse(HttpResponse<?> message) {
		_message = message;
		headers = new Headers(message.headers());
		body = new FetchBody(this, message);
		arrayBuffer = new FetchBuffer();
		status = (int) _message.statusCode();
	}

	public Headers getHeaders() {
		return headers;
	}

	public void setHeaders(Headers headers) {
		this.headers = headers;
	}

	public FetchBody getBody() {
		return body;
	}

	public void setBody(FetchBody body) {
		this.body = body;
	}

	public FetchBuffer getArrayBuffer() {
		return arrayBuffer;
	}

	public void setArrayBuffer(FetchBuffer arrayBuffer) {
		this.arrayBuffer = arrayBuffer;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public HttpResponse<?> get_message() {
		return _message;
	}

	@Override
	public String toString() {
		return "FetchResponse";
	}

	@Override
	public void close() throws Exception {
		if (_message instanceof AutoCloseable)
			((AutoCloseable) _message).close();
	}
}
