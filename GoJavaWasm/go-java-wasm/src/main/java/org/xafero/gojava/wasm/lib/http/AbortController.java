package org.xafero.gojava.wasm.lib.http;

public class AbortController {
	private AbortSignal signal;

	public AbortController() {
		signal = new AbortSignal();
	}

	public AbortSignal getSignal() {
		return signal;
	}

	@Override
	public String toString() {
		return "AbortController";
	}
}
