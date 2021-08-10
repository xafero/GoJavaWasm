package org.xafero.gojava.wasm.lib.data;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;

public class JsDate {
	private final DateTime _current;

	public JsDate() {
		_current = DateTime.now();
	}

	public int getTimeZoneOffset() {
		var utc = DateTime.now(DateTimeZone.UTC);
		var minutes = Minutes.minutesBetween(utc, _current).getMinutes();
		return minutes;
	}

	@Override
	public String toString() {
		return "JsDate";
	}
}
