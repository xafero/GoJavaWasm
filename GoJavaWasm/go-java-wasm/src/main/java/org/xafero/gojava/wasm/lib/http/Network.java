package org.xafero.gojava.wasm.lib.http;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.xafero.gojava.wasm.lib.data.JsUint8Array;

public class Network {

	public static Object fetch(Object[] args) {
		if (args.length == 2)
			return fetch((String) args[0], (Map<?, ?>) args[1]);

		throw new NotImplementedException("Fetch");
	}

	private static Object fetch(String url, Map<?, ?> dict) {
		var method = (String) dict.get("method");
		var client = HttpClient.newHttpClient();
		switch (method.toUpperCase()) {
		case "GET":
			var get = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
			return new Promise<HttpResponse<?>>(client, get, Network::wrap);
		case "POST":
			var headers = (Headers) dict.get("headers");
			var body = (JsUint8Array) dict.get("body");
			var rawBytes = ArrayUtils.toPrimitive(body.toArray(new Byte[body.size()]));
			var data = HttpRequest.BodyPublishers.ofByteArray(rawBytes);
			var post = HttpRequest.newBuilder().uri(URI.create(url)).POST(data);
			addHeaders(post, headers);
			return new Promise<HttpResponse<?>>(client, post.build(), Network::wrap);
		}
		throw new NotImplementedException("Fetch" + ": " + method + " on '" + url + "'!");
	}

	private static void addHeaders(Builder data, Headers headers) {
		for (var header : headers.entrySet())
			data.setHeader(header.getKey(), header.getValue());
	}

	private static Object wrap(HttpResponse<?> arg) {
		return new FetchResponse(arg);
	}
}
