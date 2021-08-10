package org.xafero.gojava.wasm.lib.runtime;

public class EventData {

	private double id;
	private Object _this;
	private Object args;
	private Object result;

	public double getId() {
		return id;
	}

	public void setId(double id) {
		this.id = id;
	}

	public Object getThis() {
		return _this;
	}

	public void setThis(Object _this) {
		this._this = _this;
	}

	public Object getArgs() {
		return args;
	}

	public void setArgs(Object args) {
		this.args = args;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

	@Override
	public String toString() {
		return "EventData";
	}
}
