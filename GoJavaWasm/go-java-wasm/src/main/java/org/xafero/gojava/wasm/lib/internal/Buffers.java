package org.xafero.gojava.wasm.lib.internal;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

public class Buffers {

	public static <T> void reset(Map<Integer, T> values, List<T> objects) {
		values.clear();
		for (var i = 0; i < objects.size(); i++)
			values.put(i, objects.get(i));
	}

	public static void refill(Map<Object, Integer> values, Collection<Pair<Object, Integer>> objects) {
		values.clear();
		for (var entry : objects)
			values.put(entry.getLeft(), entry.getRight());
	}

	public static void refill(byte[] dest, Collection<Byte> source) {
		var bytes = source.iterator();
		for (var i = 0; i < source.size(); i++) {
			var current = bytes.next();
			dest[i] = current;
		}
	}

	public static void refill(byte[] dest, byte[] source) {
		for (var i = 0; i < source.length; i++)
			dest[i] = source[i];
	}

	public static void refill(Collection<Byte> dest, byte[] source) {
		dest.clear();
		for (var t : source)
			dest.add(t);
	}

	public static void overwrite(List<Byte> dest, byte[] source) {
		for (var i = 0; i < source.length; i++)
			dest.set(i, source[i]);
	}
}
