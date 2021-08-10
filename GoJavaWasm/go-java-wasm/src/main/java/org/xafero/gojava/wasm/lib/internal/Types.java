package org.xafero.gojava.wasm.lib.internal;

public class Types {

	public static boolean isNumber(Object obj) {
		if (obj == null)
			return false;

		String typeName = obj.getClass().getName();
		switch (typeName) {
		case "java.lang.Byte":
		case "byte":
		case "java.lang.Short":
		case "short":
		case "java.lang.Integer":
		case "int":
		case "java.lang.Long":
		case "long":
		case "java.math.BigInteger":
		case "java.lang.Float":
		case "float":
		case "java.lang.Double":
		case "double":
			return true;
		default:
			return false;
		}
	}
}
