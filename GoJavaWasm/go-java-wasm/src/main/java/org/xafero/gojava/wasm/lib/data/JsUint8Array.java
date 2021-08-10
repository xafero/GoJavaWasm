package org.xafero.gojava.wasm.lib.data;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

public class JsUint8Array extends ArrayList<Byte> {

	private static final long serialVersionUID = -8944605071733197570L;

	public JsUint8Array() {
	}

	public JsUint8Array(byte[] span) {
		super(Arrays.asList(ArrayUtils.toObject(span)));
	}

	public int getByteLength() {
		return this.size();
	}

	@Override
	public String toString() {
		return toString(false);
	}

	private String toString(boolean showContent) {
		var bld = new StringBuilder();
		bld.append("Uint8Array" + "(" + getByteLength() + ") [");
		if (showContent)
			bld.append(String.join(", ", this.stream().map(f -> f.toString()).toArray(String[]::new)));
		bld.append("]");
		return bld.toString();
	}
}
