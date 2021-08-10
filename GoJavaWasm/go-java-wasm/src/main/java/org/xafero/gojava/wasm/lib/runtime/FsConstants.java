package org.xafero.gojava.wasm.lib.runtime;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class FsConstants {

	public final int F_OK = 0;

	public final int O_RDONLY = 0;

	public final int O_WRONLY = 1;

	public final int O_RDWR = 2;

	public final int O_CREAT = 64;

	public final int O_EXCL = 128;

	public final int O_NOCTTY = 256;

	public final int O_TRUNC = 512;

	public final int O_APPEND = 1024;

	public final int O_NONBLOCK = 2048;

	public final int O_DSYNC = 4096;

	public final int O_DIRECT = 16384;

	public final int O_DIRECTORY = 65536;

	public final int O_NOFOLLOW = 131072;

	public final int O_NOATIME = 262144;

	public final int O_SYNC = 1052672;

	@Override
	public String toString() {
		return "FsConstants";
	}

	public static List<String> findFlags(int value) throws ReflectiveOperationException {
		var flags = new ArrayList<String>();
		for (var field : Fields) {
			if (!field.getName().startsWith("O_"))
				continue;
			var fieldVal = (int) field.get(Single);
			if ((fieldVal & value) == 0)
				continue;
			flags.add(field.getName());
		}
		return flags;
	}

	private static final FsConstants Single = new FsConstants();
	private static final Field[] Fields = FsConstants.class.getFields();
}
