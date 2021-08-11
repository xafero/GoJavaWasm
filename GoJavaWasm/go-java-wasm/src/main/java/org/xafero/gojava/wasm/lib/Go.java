package org.xafero.gojava.wasm.lib;

import static io.github.kawamuray.wasmtime.WasmValType.I32;

import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.slf4j.Logger;
import org.xafero.gojava.wasm.lib.data.Builtins;
import org.xafero.gojava.wasm.lib.data.JsNull;
import org.xafero.gojava.wasm.lib.data.JsUndefined;
import org.xafero.gojava.wasm.lib.funcs.MyAction;
import org.xafero.gojava.wasm.lib.funcs.MyDelegate;
import org.xafero.gojava.wasm.lib.funcs.MyStaticFunc;
import org.xafero.gojava.wasm.lib.internal.Buffers;
import org.xafero.gojava.wasm.lib.internal.Crypto;
import org.xafero.gojava.wasm.lib.internal.MemoryWrap;
import org.xafero.gojava.wasm.lib.internal.Reflect;
import org.xafero.gojava.wasm.lib.internal.Types;
import org.xafero.gojava.wasm.lib.runtime.EventData;
import org.xafero.gojava.wasm.lib.runtime.FileSystem;
import org.xafero.gojava.wasm.lib.runtime.Globals;
import org.xafero.gojava.wasm.lib.runtime.ProcSystem;

import io.github.kawamuray.wasmtime.Engine;
import io.github.kawamuray.wasmtime.Extern;
import io.github.kawamuray.wasmtime.Func;
import io.github.kawamuray.wasmtime.Linker;
import io.github.kawamuray.wasmtime.Module;
import io.github.kawamuray.wasmtime.Store;
import io.github.kawamuray.wasmtime.WasmFunctions;
import io.github.kawamuray.wasmtime.WasmFunctions.Consumer1;
import io.github.kawamuray.wasmtime.WasmValType;

public class Go implements AutoCloseable {

	private final FileSystem _fs = new FileSystem();

	private final Logger _log;
	private final Charset _encoding;
	private final Map<Integer, Object> _scheduledTimeouts;
	private final Map<Integer, Double> _goRefCounts;
	private final Map<Integer, Object> _values;
	private final Stack<Integer> _idPool;
	private final Map<Object, Integer> _ids;
	private final List<Triple<String, String, Func>> _funcs;

	private int _nextCallbackTimeoutId;
	private Store<Void> _store;
	private Linker _linker;
	private Module _module;
	private MemoryWrap _mem;
	private Boolean exited;
	private EventData pendingEvent;

	public Go(Logger log) {
		this._log = log;
		this._encoding = StandardCharsets.UTF_8;
		_scheduledTimeouts = new HashMap<Integer, Object>();
		_goRefCounts = new HashMap<Integer, Double>();
		_values = new HashMap<Integer, Object>();
		_idPool = new Stack<Integer>();
		_ids = new HashMap<Object, Integer>();
		_funcs = new ArrayList<Triple<String, String, Func>>();
		_nextCallbackTimeoutId = 1;
		pendingEvent = null;
		exited = null;
	}

	public Boolean getExited() {
		return exited;
	}

	public void setExited(Boolean exited) {
		this.exited = exited;
	}

	public EventData getPendingEvent() {
		return pendingEvent;
	}

	public void setPendingEvent(EventData pendingEvent) {
		this.pendingEvent = pendingEvent;
	}

	@Override
	public void close() throws Exception {
		_scheduledTimeouts.clear();
		_goRefCounts.clear();
		_values.clear();
		_idPool.clear();
		_ids.clear();
		_funcs.clear();
		_store.close();
		_linker.close();
		_module.close();
		_mem.close();
	}

	private void exit(int code) {
		if (code != 0)
			System.err.println("exit code: " + code);
	}

	private ByteBuffer getMemBuffer() {
		return this._mem.getSpan(_store);
	}

	private ByteBuffer getMemSlice(int position, int length) {
		var slice = getMemBuffer().duplicate().position(position).slice();
		slice.limit(length);
		return slice;
	}

	/**
	 * func clearTimeoutEvent(id int32)
	 * 
	 * @param sp
	 */
	private void clearTimeoutEvent(int sp) {
		sp >>= 0;
		var id = _mem.readInt32(_store, sp + 8);
		clearTimeout(_scheduledTimeouts.get(id));
		_scheduledTimeouts.remove(id);
		_log.trace("clearTimeoutEvent {}", id);
	}

	/**
	 * func getRandomData(r []byte)
	 * 
	 * @param sp
	 */
	private void getRandomData(int sp) {
		sp >>= 0;
		var buf = loadSlice(sp + 8);
		Crypto.getRandomValues(buf);
		_log.trace("getRandomData {}", buf.limit());
	}

	/**
	 * func finalizeRef(v ref)
	 * 
	 * @param sp
	 */
	private void finalizeRef(int sp) {
		sp >>= 0;
		var id = _mem.readInt32(_store, sp + 8);
		_goRefCounts.put(id, _goRefCounts.get(id) - 1);
		if (_goRefCounts.get(id) == 0) {
			var v = _values.get(id);
			_values.put(id, null);
			_ids.remove(v);
			_idPool.push(id);
		}
		_log.trace("finalizeRef {}", id);
	}

	/**
	 * func nanotime1() int64
	 * 
	 * @param sp
	 */
	private void nanoTime(int sp) {
		sp >>= 0;
		var longVal = ProcSystem.getNanoTime() * 1000;
		setInt64(sp + 8, longVal);
		_log.trace("nanoTime {}", longVal);
	}

	/**
	 * func walltime1() (sec int64, nsec int32)
	 * 
	 * @param sp
	 */
	private void wallTime(int sp) {
		sp >>= 0;
		var msec = System.currentTimeMillis();
		var mVal = msec / 1000;
		setInt64(sp + 8, mVal);
		var val = (msec % 1000) * 1000000;
		_mem.writeInt32(_store, sp + 16, (int) val);
		_log.trace("walltime {} {}", mVal, val);
	}

	/**
	 * func stringVal(value string) ref
	 * 
	 * @param sp
	 */
	private void stringVal(int sp) {
		sp >>= 0;
		var txt = loadString(sp + 8);
		storeValue(sp + 24, txt);
		_log.trace("stringVal '{}'", txt);
	}

	/**
	 * func valueInvoke(v ref, args []ref) (ref, bool)
	 * 
	 * @param sp
	 */
	private void valueInvoke(int sp) {
		sp >>= 0;
		try {
			var v = loadValue(sp + 8);
			var args = loadSliceOfValues(sp + 16);
			var result = Reflect.apply(v, JsUndefined.S, args);
			sp = getSp() >> 0;
			storeValue(sp + 40, result);
			_mem.writeByte(_store, sp + 48, (byte) 1);
			_log.trace("valueInvoke {} {} {}", v, args, result);
		} catch (Exception err) {
			storeValue(sp + 40, err);
			_mem.writeByte(_store, sp + 48, (byte) 0);
		}
	}

	/**
	 * func valueNew(v ref, args []ref) (ref, bool)
	 * 
	 * @param sp
	 */
	private void valueNew(int sp) {
		sp >>= 0;
		try {
			var v = loadValue(sp + 8);
			var args = loadSliceOfValues(sp + 16);
			var result = Reflect.construct(v, args);
			sp = getSp() >> 0;
			storeValue(sp + 40, result);
			_mem.writeByte(_store, sp + 48, (byte) 1);
			_log.trace("valueNew {} {} {}", v, args, result);
		} catch (Exception err) {
			storeValue(sp + 40, err);
			_mem.writeByte(_store, sp + 48, (byte) 0);
		}
	}

	/**
	 * func valueLength(v ref) int
	 * 
	 * @param sp
	 */
	private void valueLength(int sp) {
		sp >>= 0;
		var val = loadValue(sp + 8);
		int num;
		if (val instanceof Collection<?>)
			num = ((Collection<?>) val).size();
		else
			num = Array.getLength(val);
		setInt64(sp + 16, num);
		_log.trace("valueLength {} {}", val, num);
	}

	/**
	 * func valuePrepareString(v ref) (ref, int)
	 * 
	 * @param sp
	 */
	private void valuePrepareString(int sp) {
		sp >>= 0;
		var val = loadValue(sp + 8);
		var txt = (String) val;
		var str = _encoding.encode(txt).array();
		storeValue(sp + 16, str);
		setInt64(sp + 24, str.length);
	}

	private void setInt64(int addr, long v) {
		_mem.writeInt32(_store, addr + 0, (int) v);
		_mem.writeInt32(_store, addr + 4, (int) Math.floor(v / 4294967296.0));
	}

	private int getSp() {
		var getter = _linker.get(_store, "", "getsp").get().func();
		var res = getter.call(_store);
		return res[0].i32();
	}

	/**
	 * func valueLoadString(v ref, b []byte)
	 * 
	 * @param sp
	 */
	private void valueLoadString(int sp) {
		sp >>= 0;
		var str = (byte[]) loadValue(sp + 8);
		var bytes = loadSlice(sp + 16);
		Buffers.overwrite(bytes, str);
	}

	private long getInt64(int addr) {
		var low = _mem.readInt32(_store, addr + 0);
		var high = _mem.readInt32(_store, addr + 4);
		return low + high * 4294967296l;
	}

	private Object setTimeout(MyAction action, long interval) {
		throw new NotImplementedException("SetTimeout");
	}

	private Object loadValue(long addr) {
		var f = _mem.readDouble(_store, (int) addr);

		if ((f + "") == "0")
			return JsUndefined.S;

		if (!Double.isNaN(f))
			return f;

		var id = _mem.readInt32(_store, (int) addr);
		return _values.get(id);
	}

	private Object[] loadSliceOfValues(int addr) {
		var array = getInt64(addr + 0);
		var len = getInt64(addr + 8);
		var a = new Object[(int) len];
		for (var i = 0; i < len; i++)
			a[i] = loadValue(array + i * 8);
		return a;
	}

	/**
	 * func copyBytesToGo(dst []byte, src ref) (int, bool)
	 * 
	 * @param sp
	 */
	@SuppressWarnings("unchecked")
	private void copyBytesToGo(int sp) {
		sp >>= 0;
		var dst = loadSlice(sp + 8);
		var src = loadValue(sp + 32);
		if (!(src instanceof byte[] || src instanceof Collection<?>)) {
			_mem.writeByte(_store, sp + 48, (byte) 0);
			return;
		}
		var srcArray = (Collection<Byte>) src;
		var interm = ArrayUtils.toPrimitive(srcArray.toArray(new Byte[srcArray.size()]));
		var toCopy = ArrayUtils.subarray(interm, 0, dst.limit());
		Buffers.overwrite(dst, toCopy);
		setInt64(sp + 40, toCopy.length);
		_mem.writeByte(_store, sp + 48, (byte) 1);
		_log.trace("copyBytesToGo {} {} {}", srcArray.size(), dst.limit(), toCopy.length);
	}

	/**
	 * func copyBytesToJS(dst ref, src []byte) (int, bool)
	 * 
	 * @param sp
	 */
	@SuppressWarnings("unchecked")
	private void copyBytesToJs(int sp) {
		sp >>= 0;
		var dst = loadValue(sp + 8);
		var src = loadSlice(sp + 16);
		if (!(dst instanceof byte[] || dst instanceof Collection<?>)) {
			_mem.writeByte(_store, sp + 48, (byte) 0);
			return;
		}
		var dstArray = (Collection<Byte>) dst;
		var toCopy = ArrayUtils.subarray(Buffers.copy(src), 0, dstArray.size());
		Buffers.refill(dstArray, toCopy);
		setInt64(sp + 40, toCopy.length);
		_mem.writeByte(_store, sp + 48, (byte) 1);
		_log.trace("copyBytesToJS {} {} {}", src.limit(), dstArray.size(), toCopy.length);
	}

	private String loadString(int addr) {
		var saddr = getInt64(addr + 0);
		var len = getInt64(addr + 8);
		var slice = getMemSlice((int) saddr, (int) len);
		return _encoding.decode(slice).toString();
	}

	@SuppressWarnings("resource")
	public MyStaticFunc makeFuncWrapper(double id) {
		var go = this;

		MyStaticFunc funcWrap = (Object[] args) -> {
			var event = new EventData();
			event.setId(id);
			event.setThis(null);
			event.setArgs(args);
			go.pendingEvent = event;
			go.resume();
			return event.getResult();
		};

		return funcWrap;
	}

	/**
	 * func valueGet(v ref, p string) ref
	 * 
	 * @param sp
	 */
	private void valueGet(int sp) {
		sp >>= 0;
		var ref = loadValue(sp + 8);
		var str = loadString(sp + 16);
		var result = Reflect.get(ref, str);
		sp = getSp() >> 0;
		storeValue(sp + 32, result);
		_log.trace("valueGet {} {} {}", ref, str, result);
	}

	/**
	 * func valueSet(v ref, p string, x ref)
	 * 
	 * @param sp
	 */
	private void valueSet(int sp) {
		sp >>= 0;
		var obj = loadValue(sp + 8);
		var name = loadString(sp + 16);
		var val = loadValue(sp + 32);
		Reflect.set(obj, name, val);
		_log.trace("valueSet {} {} {}", obj, name, val);
	}

	/**
	 * func valueDelete(v ref, p string)
	 * 
	 * @param sp
	 */
	private void valueDelete(int sp) {
		sp >>= 0;
		var obj = loadValue(sp + 8);
		var name = loadString(sp + 16);
		Reflect.deleteProperty(obj, name);
		_log.trace("valueDelete {} {}", obj, name);
	}

	/**
	 * func valueIndex(v ref, i int) ref
	 * 
	 * @param sp
	 */
	private void valueIndex(int sp) {
		sp >>= 0;
		var obj = loadValue(sp + 8);
		var index = getInt64(sp + 16);
		var item = Reflect.get(obj, index);
		storeValue(sp + 24, item);
		_log.trace("valueIndex {} {} {}", obj, index, item);
	}

	private void storeValue(int addr, Object v) {
		int nanHead = 0x7FF80000;

		double vd;
		if (Types.isNumber(v) && (v + "") != "0" && (vd = Double.parseDouble(v + "")) != 0) {
			if (Double.isNaN(vd)) {
				_mem.writeInt32(_store, addr + 4, nanHead);
				_mem.writeInt32(_store, addr, 0);
				return;
			}
			_mem.writeDouble(_store, addr, vd);
			return;
		}

		if (Builtins.isJsUndefined(v)) {
			_mem.writeDouble(_store, addr, 0);
			return;
		}

		Integer id;
		if ((id = _ids.get(v)) == null) {
			if (_idPool.empty() || (id = _idPool.pop()) == null)
				id = _values.size();
			_values.put(id, v);
			_goRefCounts.put(id, (double) 0);
			_ids.put(v, id);
		}
		_goRefCounts.put(id, _goRefCounts.get(id) + 1);

		var typeFlag = 0;

		if (v instanceof MyDelegate) {
			typeFlag = 4;
		} else if (v instanceof String) {
			typeFlag = 2;
		} else if ((v instanceof JsNull) || (v instanceof Integer) || (v instanceof Boolean)) {
			typeFlag = 0;
		} else if (v instanceof Object) {
			typeFlag = 1;
		} else {
			throw new UnsupportedOperationException(typeFlag + " ?");
		}

		switch (v.getClass().getSimpleName()) {
		case "symbol":
			typeFlag = 3; // TODO
			break;
		}

		_mem.writeInt32(_store, addr + 4, nanHead | typeFlag);
		_mem.writeInt32(_store, addr, id);
		_log.trace("storeValue {} {} {}", typeFlag, id, addr);
	}

	/**
	 * valueSetIndex(v ref, i int, x ref)
	 * 
	 * @param sp
	 */
	private void valueSetIndex(int sp) {
		sp >>= 0;
		var obj = loadValue(sp + 8);
		var index = getInt64(sp + 16);
		var val = loadValue(sp + 24);
		Reflect.set(obj, index, val);
	}

	/**
	 * func valueCall(v ref, m string, args []ref) (ref, bool)
	 * 
	 * @param sp
	 */
	private void valueCall(int sp) {
		sp >>= 0;
		try {
			var v = loadValue(sp + 8);
			var name = loadString(sp + 16);
			var m = Reflect.get(v, name);
			var args = loadSliceOfValues(sp + 32);
			var result = Reflect.apply(m, v, args);
			sp = getSp() >> 0;
			storeValue(sp + 56, result);
			_mem.writeByte(_store, sp + 64, (byte) 1);
			_log.trace("valueCall {} {} {} {} {}", v, name, m, args, result);
		} catch (Exception err) {
			storeValue(sp + 56, err);
			_mem.writeByte(_store, sp + 64, (byte) 0);
		}
	}

	private ByteBuffer loadSlice(int addr) {
		var array = getInt64(addr + 0);
		var len = getInt64(addr + 8);
		var bytes = getMemSlice((int) array, (int) len);
		return bytes;
	}

	/**
	 * func wasmExit(code int32)
	 * 
	 * @param sp
	 */
	private void wasmExit(int sp) {
		sp >>= 0;
		var code = _mem.readInt32(_store, sp + 8);
		exited = true;
		// _instance = null;
		_values.clear();
		_goRefCounts.clear();
		_ids.clear();
		_idPool.clear();
		exit((int) code);
		_log.trace("wasmExit {}", code);
	}

	/**
	 * func wasmWrite(fd uintptr, p unsafe.Pointer, n int32)
	 * 
	 * @param sp
	 */
	private void wasmWrite(int sp) {
		sp >>= 0;
		var fd = getInt64(sp + 8);
		var p = getInt64(sp + 16);
		var n = _mem.readInt32(_store, sp + 24);
		var slice = getMemSlice((int) p, (int) n);
		var bytes = new byte[n];
		slice.get(bytes);
		_fs.writeSync(fd, bytes);
		_log.trace("wasmWrite {} {} {} {}", fd, p, n, bytes.length);
	}

	private void debug(int value) {
		System.out.println(value);
	}

	private void clearTimeout(Object o) {
		throw new NotImplementedException("ClearTimeout");
	}

	/**
	 * func resetMemoryDataView()
	 * 
	 * @param sp
	 */
	private void resetMemoryDataView(int sp) {
		sp >>= 0;
		_log.trace("resetMemoryDataView");
	}

	/**
	 * func valueInstanceOf(v ref, t ref) bool
	 * 
	 * @param sp
	 */
	private void valueInstanceOf(int sp) {
		sp >>= 0;
		var ref = loadValue(sp + 8);
		var tr = loadValue(sp + 16);
		_log.trace("valueInstanceOf {} {}", ref, tr);
	}

	/**
	 * func scheduleTimeoutEvent(delay int64) int32
	 * 
	 * @param sp
	 */
	private void scheduleTimeoutEvent(int sp) {
		sp >>= 0;
		var id = _nextCallbackTimeoutId;
		_nextCallbackTimeoutId++;
		_scheduledTimeouts.put(id, setTimeout(o -> {
			resume();
			while (_scheduledTimeouts.containsKey(id)) {
				// for some reason Go failed to register the timeout event, log and try again
				// (temporary workaround for https://github.com/golang/go/issues/28975)
				System.err.println("scheduleTimeoutEvent: missed timeout event");
				resume();
			}
		}, getInt64(sp + 8) + 1)); // setTimeout has been seen to fire up to 1 millisecond early

		_mem.writeInt32(_store, sp + 16, id);
		_log.trace("scheduleTimeoutEvent {}", id);
	}

	private void resume() {
		if (exited == true) {
			throw new UnsupportedOperationException("Go program has already exited");
		}
		var resume = _linker.get(_store, "", "resume").get().func();
		var fun = WasmFunctions.consumer(_store, resume);
		fun.accept();
		if (exited == true) {
		}
	}

	@SuppressWarnings("unchecked")
	public void run() {
		var env = new HashMap<String, Object>();
		var argv = new Object[] { "js" };

		/*
		 * if (_instance == null) { throw new
		 * UnsupportedOperationException("Go.run: WebAssembly.Instance expected"); }
		 */

		var global = new Globals();
		var _null = JsNull.S;

		// JS values that Go currently has references to, indexed by reference id
		Buffers.reset(_values, Arrays.asList(new Object[] { Double.NaN, 0, _null, true, false, global, this }));

		// number of references that Go has to a JS value, indexed by reference id
		Buffers.reset(_goRefCounts, Collections.nCopies(_values.size(), Double.POSITIVE_INFINITY));

		// mapping from JS values to reference ids
		Buffers.refill(_ids,
				Arrays.asList(
						new Pair[] { ImmutablePair.of(0, 1), ImmutablePair.of(_null, 2), ImmutablePair.of(true, 3),
								ImmutablePair.of(false, 4), ImmutablePair.of(global, 5), ImmutablePair.of(this, 6) }));

		// unused ids that have been garbage collected
		_idPool.clear();

		// whether the Go program has exited
		exited = false;

		// Pass command line arguments and environment variables to WebAssembly by
		// writing them to the linear memory
		final int[] offset = new int[] { 4096 };

		Function<String, Integer> strPtr = str -> {
			var bytes = _encoding.encode(str + "\0").array();
			var buff = getMemSlice(offset[0], bytes.length);
			buff.get(bytes);
			offset[0] = offset[0] + bytes.length;
			if (offset[0] % 8 != 0) {
				offset[0] = offset[0] + 8 - (offset[0] % 8);
			}
			var ptr = offset[0];
			return ptr;
		};

		var argc = argv.length;

		var argvPtrs = new ArrayList<Integer>();
		for (Object arg : argv)
			argvPtrs.add(strPtr.apply((String) arg));
		argvPtrs.add(0);

		var keys = env.keySet().stream().sorted().toArray();
		for (Object key : keys)
			argvPtrs.add(strPtr.apply(key + "=" + env.get(key)));
		argvPtrs.add(0);

		for (Integer ptr : argvPtrs) {
			_mem.writeInt32(_store, offset[0], ptr);
			_mem.writeInt32(_store, offset[0] + 4, 0);
			offset[0] = offset[0] + 8;
		}

		var localArgv = offset[0];

		var run = _linker.get(_store, "", "run").get().func();
		var fun = WasmFunctions.consumer(_store, run, I32, I32);
		fun.accept(argc, localArgv);

		if (exited == true) {
		}
	}

	private void defineMethod(String name, Consumer1<Integer> callback) {
		var module = "go";
		var func = WasmFunctions.wrap(_store, WasmValType.I32, callback);
		Triple<String, String, Func> triple = ImmutableTriple.of(module, name, func);
		_funcs.add(triple);
	}

	@Override
	public String toString() {
		return "Go";
	}

	public void prepare() {
		_store = Store.withoutData();
		_linker = new Linker(_store.engine());
	}

	public void addDefaultImports() {
		defineMethod("debug", i -> debug(i));
		defineMethod("runtime.resetMemoryDataView", i -> resetMemoryDataView(i));
		defineMethod("runtime.wasmExit", i -> wasmExit(i));
		defineMethod("runtime.wasmWrite", i -> wasmWrite(i));
		defineMethod("runtime.nanotime1", i -> nanoTime(i));
		defineMethod("runtime.walltime1", i -> wallTime(i));
		defineMethod("runtime.scheduleTimeoutEvent", i -> scheduleTimeoutEvent(i));
		defineMethod("runtime.clearTimeoutEvent", i -> clearTimeoutEvent(i));
		defineMethod("runtime.getRandomData", i -> getRandomData(i));
		defineMethod("syscall/js.finalizeRef", i -> finalizeRef(i));
		defineMethod("syscall/js.stringVal", i -> stringVal(i));
		defineMethod("syscall/js.valueGet", i -> valueGet(i));
		defineMethod("syscall/js.valueSet", i -> valueSet(i));
		defineMethod("syscall/js.valueDelete", i -> valueDelete(i));
		defineMethod("syscall/js.valueIndex", i -> valueIndex(i));
		defineMethod("syscall/js.valueSetIndex", i -> valueSetIndex(i));
		defineMethod("syscall/js.valueCall", i -> valueCall(i));
		defineMethod("syscall/js.valueInvoke", i -> valueInvoke(i));
		defineMethod("syscall/js.valueNew", i -> valueNew(i));
		defineMethod("syscall/js.valueLength", i -> valueLength(i));
		defineMethod("syscall/js.valuePrepareString", i -> valuePrepareString(i));
		defineMethod("syscall/js.valueLoadString", i -> valueLoadString(i));
		defineMethod("syscall/js.valueInstanceOf", i -> valueInstanceOf(i));
		defineMethod("syscall/js.copyBytesToGo", i -> copyBytesToGo(i));
		defineMethod("syscall/js.copyBytesToJS", i -> copyBytesToJs(i));
	}

	public void create(Function<Engine, Module> setup) {
		_module = setup.apply(_store.engine());
	}

	public void importObject() {
		for (Triple<String, String, Func> triple : _funcs) {
			var mod = triple.getLeft();
			var name = triple.getMiddle();
			var extern = Extern.fromFunc(triple.getRight());
			_linker.define(mod, name, extern);
		}
	}

	public void instantiate() {
		_linker.module(_store, "", _module);
	}

	public void load() {
		var mem = _linker.get(_store, "", "mem").get().memory();
		_mem = new MemoryWrap(mem);
	}
}
