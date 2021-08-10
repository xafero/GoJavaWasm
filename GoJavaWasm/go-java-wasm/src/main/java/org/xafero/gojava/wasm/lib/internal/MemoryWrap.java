package org.xafero.gojava.wasm.lib.internal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import io.github.kawamuray.wasmtime.Memory;
import io.github.kawamuray.wasmtime.Store;

public class MemoryWrap implements AutoCloseable {

	private final Memory _internal;

	public MemoryWrap(Memory internal) {
		_internal = internal;
	}

	@Override
	public void close() throws Exception {
		_internal.close();
		_internal.dispose();
	}

	public ByteBuffer getSpan(Store<?> store) {
		return _internal.buffer(store);
	}

	private ByteBuffer getSlice(Store<?> store, int address) {
		return getSpan(store).slice().order(ByteOrder.LITTLE_ENDIAN).position(address);
	}

	public int readInt32(Store<?> store, int address) {
		return getSlice(store, address).asIntBuffer().get();
	}

	public void writeInt32(Store<?> store, int address, Integer value) {
		getSlice(store, address).asIntBuffer().put(value);
	}

	public double readDouble(Store<?> store, int address) {
		return getSlice(store, address).asDoubleBuffer().get();
	}

	public void writeDouble(Store<?> store, int address, double value) {
		getSlice(store, address).asDoubleBuffer().put(value);
	}

	public void writeByte(Store<?> store, int address, byte value) {
		getSlice(store, address).put(value);
	}
}
