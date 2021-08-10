package org.xafero.gojava.wasm.lib;

public class Error extends Exception {
	
	private static final long serialVersionUID = -8810377399759512827L;

	private String code;

	public Error(String text) {
		super(text);
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}
}
