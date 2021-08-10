package org.xafero.gojava.wasm.lib.data;

import java.util.Iterator;

public class JsIterator implements AutoCloseable {

	public class NextItem {
		private boolean done;
		private Object value;

		public boolean isDone() {
			return done;
		}

		public void setDone(boolean done) {
			this.done = done;
		}

		public Object getValue() {
			return value;
		}

		public void setValue(Object value) {
			this.value = value;
		}
	}

	private final Iterator<?> _enumerator;

	public JsIterator(Iterator<?> enumerator) {
		_enumerator = enumerator;
	}

	public NextItem next() {
		var isDone = !_enumerator.hasNext();
		var item = new NextItem();
		item.setDone(isDone);
		item.setValue(isDone ? JsUndefined.S : _enumerator.next());
		return item;
	}

	@Override
	public void close() throws Exception {
		if (_enumerator instanceof AutoCloseable)
			((AutoCloseable) _enumerator).close();
	}

	@Override
	public String toString() {
		return "JsIterator";
	}
}
