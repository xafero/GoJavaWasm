package org.xafero.gojava.wasm.lib.internal;

import java.nio.ByteBuffer;
import java.util.Random;

public class Crypto {
	private static final Random random = new Random();

	public static ByteBuffer getRandomValues(ByteBuffer buffer) {
		var bytes = new byte[buffer.limit()];
		random.nextBytes(bytes);
		Buffers.overwrite(buffer, bytes);
		return buffer;
	}

	@Override
	public String toString() {
		return "Crypto";
	}
}
