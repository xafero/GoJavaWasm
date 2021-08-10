package org.xafero.gojava.wasm.lib.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.xafero.gojava.wasm.lib.data.Builtins;
import org.xafero.gojava.wasm.lib.funcs.MyInstanceFunc;
import org.xafero.gojava.wasm.lib.funcs.MyStaticFunc;
import org.xafero.gojava.wasm.lib.runtime.Globals;

import static org.xafero.gojava.wasm.lib.internal.Errors.execute;

public class Reflect {

	public static void deleteProperty(Object obj, String name) {
		throw new NotImplementedException("DeleteProperty");
	}

	public static Object get(Object obj, Object value) {
		if (obj instanceof Globals && value instanceof String)
			return ((Globals) obj).get((String) value);

		if (value instanceof String) {
			var name = (String) value;
			var type = obj.getClass();

			var property = findProperty(type, name);
			if (property != null)
				return getValue(property, obj);

			var method = findMethod(type, name);
			if (method != null)
				return wrap(method);

			var field = findField(type, name);
			if (field != null)
				return getValue(field, obj);

			throw new NotImplementedException(type + " " + name);
		}

		if (obj instanceof Object[]) {
			var coll = (Object[]) obj;
			var index = (long) value;
			var item = coll[(int) index];
			return item;
		}

		if (obj instanceof List<?>) {
			var coll = (List<?>) obj;
			var index = (long) value;
			var item = coll.get((int) index);
			return item;
		}

		if (obj instanceof AbstractMap.SimpleEntry<?, ?>) {
			var pair = (AbstractMap.SimpleEntry<?, ?>) obj;
			var index = (long) value;
			var item = index == 0 ? pair.getKey() : pair.getValue();
			return item;
		}

		throw new NotImplementedException("Get");
	}

	private static Object getValue(Field field, Object obj) {
		try {
			return field.get(obj);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static Object getValue(Pair<Method, Method> property, Object obj) {
		try {
			return property.getLeft().invoke(obj);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static Object wrap(Method method) {
		MyInstanceFunc func = (obj, args) -> {
			try {
				return method.invoke(obj, args);
			} catch (ReflectiveOperationException e) {
				throw new UnsupportedOperationException(e);
			}
		};

		return func;
	}

	public static Object apply(Object obj, Object instance, Object[] args) {
		if (obj instanceof MyInstanceFunc) {
			var func = (MyInstanceFunc) obj;
			var result = execute(func, instance, args);
			return result;
		}
		if (obj instanceof MyStaticFunc) {
			var staticFunc = (MyStaticFunc) obj;
			var result = execute(staticFunc, args);
			return result;
		}
		throw new NotImplementedException("Apply");
	}

	@SuppressWarnings("unchecked")
	public static Object construct(Object obj, Object[] args) {
		if (obj instanceof MyStaticFunc) {
			var of = (MyStaticFunc) obj;
			var res = execute(of);
			if (res instanceof Collection<?>) {
				var byteArray = (Collection<Byte>) res;
				for (var i = 0; i < (double) args[0]; i++)
					byteArray.add((byte) 0);
				return res;
			}
			if (args == null || args.length < 1) {
				var instance = execute(of);
				return instance;
			}
			throw new NotImplementedException(res + " ?");
		}
		throw new NotImplementedException("Construct");
	}

	@SuppressWarnings("unchecked")
	public static void set(Object obj, Object name, Object rawValue) {
		if (name instanceof String) {
			var myName = (String) name;
			if (obj instanceof Map<?, ?>) {
				var dict = (Map<String, Object>) obj;
				var key = myName;
				var val = rawValue;
				dict.put(key, val);
				return;
			}
			var type = obj.getClass();
			var prop = findProperty(type, myName);
			var value = Builtins.isUndefinedOrNull(rawValue) ? null : rawValue;
			invoke(prop.getRight(), obj, value);
			return;
		}
		throw new NotImplementedException("Set");
	}

	private static Object invoke(Method method, Object obj, Object... args) {
		try {
			return method.invoke(obj, args);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	private static Field findField(Class<?> type, String name) {
		return Arrays.asList(type.getFields()).stream()
				.filter(f -> f.getName().equalsIgnoreCase(StringUtils.stripStart(name, "_"))).findFirst().orElse(null);
	}

	private static Method findMethod(Class<?> type, String name) {
		return Arrays.asList(type.getMethods()).stream()
				.filter(m -> m.getName().equalsIgnoreCase(StringUtils.stripStart(name, "_"))).findFirst().orElse(null);
	}

	private static Pair<Method, Method> findProperty(Class<?> type, String rawName) {
		var name = StringUtils.stripStart(rawName, "_");
		var getter = findMethod(type, "get" + name);
		if (getter == null)
			getter = findMethod(type, "is" + name);
		var setter = findMethod(type, "set" + name);
		if (getter == null && setter == null)
			return null;
		return new ImmutablePair<Method, Method>(getter, setter);
	}
}
