package org.xafero.gojava.wasm.lib.internal;

import java.util.Random;

public class Crypto {
	private static final Random random = new Random();

	public static byte[] getRandomValues(byte[] buffer) {
		random.nextBytes(buffer);
		return buffer;
	}

	@Override
	public String toString() {
		return "Crypto";
	}
}
