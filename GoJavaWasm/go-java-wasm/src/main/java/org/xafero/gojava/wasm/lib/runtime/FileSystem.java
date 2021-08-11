package org.xafero.gojava.wasm.lib.runtime;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.xafero.gojava.wasm.lib.Error;
import org.xafero.gojava.wasm.lib.data.Builtins;
import org.xafero.gojava.wasm.lib.data.JsNull;
import org.xafero.gojava.wasm.lib.funcs.MyStaticFunc;
import org.xafero.gojava.wasm.lib.internal.Buffers;
import org.xafero.gojava.wasm.lib.internal.Errors;

import static org.xafero.gojava.wasm.lib.internal.Errors.execute;

public class FileSystem {
	public final FsConstants Constants = new FsConstants();

	private static final Charset encoding = StandardCharsets.UTF_8;

	private final Map<Integer, FileDescriptor> _fileDesc;
	private String _outputBuf = "";

	public FileSystem() {
		_fileDesc = new HashMap<Integer, FileDescriptor>();
	}

	public void stat(String path, MyStaticFunc call) {
		var fullPath = (new File(path)).getAbsolutePath();
		var fsInfo = new File(fullPath);
		execute(call, JsNull.S, new FsStats(fsInfo));
	}

	public void fstat(double fileId, MyStaticFunc call) {
		var fileDesc = _fileDesc.get((int) fileId);
		var path = fileDesc.getPath();
		stat(path, call);
	}

	public void mkdir(String path, Object mode, MyStaticFunc call) {
		(new File(path)).mkdir();
		execute(call, JsNull.S);
	}

	public int writeSync(double fd, byte[] buf) {
		var list = Arrays.asList(ArrayUtils.toObject(buf));
		return writeSync(fd, list);
	}

	public int writeSync(double fd, List<Byte> buf) {
		FileDescriptor fileDesc;
		if ((fileDesc = _fileDesc.get((int) fd)) != null) {
			try {
				var array = ArrayUtils.toPrimitive(buf.toArray(new Byte[buf.size()]));
				fileDesc.getHandle().write(array);
				fileDesc.flush();
				return buf.size();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		_outputBuf += encoding.decode(ByteBuffer.wrap(ArrayUtils.toPrimitive(buf.toArray(new Byte[buf.size()]))));
		var nl = _outputBuf.lastIndexOf('\n');
		if (nl != -1) {
			System.out.println(_outputBuf.substring(0, nl));
			_outputBuf = _outputBuf.substring(nl + 1);
		}
		return buf.size();
	}

	public void write(double fd, List<Byte> buf, int offset, double length, Object position, MyStaticFunc call)
			throws IOException {
		if (offset != 0 || ((int) length) != buf.size() || !Builtins.isUndefinedOrNull(position)) {
			execute(call, Errors.newEnoSys());
			return;
		}
		var n = writeSync(fd, buf);
		execute(call, JsNull.S, n, buf);
	}

	public void read(double fd, List<Byte> buf, int offset, double length, Object position, MyStaticFunc call)
			throws IOException {
		var fileDesc = _fileDesc.get((int) fd);
		var buff = ArrayUtils.toPrimitive(buf.toArray(new Byte[buf.size()]));
		if (position instanceof Double) {
			var posD = (Double) position;
			fileDesc.getHandle().seek(posD.longValue());
		}
		var n = fileDesc.getHandle().read(buff, offset, (int) length);
		Buffers.overwrite(buf, buff);
		var readBytes = Math.max(0, n);
		execute(call, JsNull.S, readBytes, buf);
	}

	public void open(String path, double flags, double mode, MyStaticFunc call)
			throws IOException, ReflectiveOperationException {
		var foundFlags = FsConstants.findFlags((int) flags);

		if (foundFlags.contains("O_CREAT")) {
			var created = new File(path);
			created.createNewFile();
			var createHandle = new FileDescriptor(foundFlags, new RandomAccessFile(created, "rw"), path);
			_fileDesc.put(createHandle.getId(), createHandle);
			execute(call, JsNull.S, createHandle.getId());
			return;
		}

		if ((int) flags == Constants.O_RDONLY) {
			var read = new File(path);
			var readHandle = new FileDescriptor(foundFlags, new RandomAccessFile(read, "r"), path);
			_fileDesc.put(readHandle.getId(), readHandle);
			execute(call, JsNull.S, readHandle.getId());
			return;
		}

		throw new NotImplementedException("Open");
	}

	public void close(double fileId, MyStaticFunc call) throws Exception {
		try (var fileDesc = _fileDesc.get((int) fileId)) {
			_fileDesc.remove(fileDesc.getId());
		}
		execute(call, JsNull.S);
	}

	public void fsync(double fileId, MyStaticFunc call) throws IOException {
		var fileDesc = _fileDesc.get((int) fileId);
		fileDesc.flush();
		execute(call, JsNull.S);
	}

	public void unlink(String path, MyStaticFunc call) {
		if (!(new File(path)).exists()) {
			execute(call, Errors.newEnoSys());
			return;
		}
		try {
			(new File(path)).delete();
		} catch (Exception e) {
			execute(call, new Error(e.getMessage()));
		}
	}

	public void rmdir(String path, MyStaticFunc call) {
		if (!(new File(path)).isDirectory()) {
			execute(call, Errors.newEnoSys());
			return;
		}
		try {
			FileUtils.deleteDirectory(new File(path));
		} catch (Exception e) {
			execute(call, new Error(e.getMessage()));
		}
	}

	@Override
	public String toString() {
		return "FileSystem";
	}

	private static int _nextFileId = 100;

	private class FileDescriptor implements AutoCloseable {
		private int id;
		private List<String> flags;
		private String path;
		private RandomAccessFile access;

		public FileDescriptor(List<String> flags, RandomAccessFile access, String path) {
			id = ++_nextFileId;
			this.flags = flags;
			this.access = access;
			this.path = path;
		}

		public void flush() throws IOException {
			access.getFD().sync();
		}

		public int getId() {
			return id;
		}

		@SuppressWarnings("unused")
		public List<String> getFlags() {
			return flags;
		}

		public RandomAccessFile getHandle() {
			return access;
		}

		public String getPath() {
			return path;
		}

		@Override
		public void close() throws Exception {
			flags.clear();
			flush();
			access.close();
		}
	}
}
