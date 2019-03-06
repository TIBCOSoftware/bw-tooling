/*Copyright © 2018. TIBCO Software Inc. All Rights Reserved.*/

package com.tibco.bw.prometheus.monitor.util;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class Utils {

	public static boolean isPCF() {
		return System.getenv("VCAP_APPLICATION") != null;
	}
	
	public static String convertTimeToString(final long time) {
		DecimalFormat format2 = new DecimalFormat("00");
		DecimalFormat format3 = new DecimalFormat("000");
		GregorianCalendar cal = new GregorianCalendar();
		cal.setTimeInMillis(time);
		StringBuffer buf = new StringBuffer(80);
		buf.append(cal.get(Calendar.YEAR));
		buf.append('-');
		buf.append(format2.format(cal.get(Calendar.MONTH) + 1));
		buf.append('-');
		buf.append(format2.format(cal.get(Calendar.DAY_OF_MONTH)));
		buf.append(' ');
		buf.append(format2.format(cal.get(Calendar.HOUR_OF_DAY)));
		buf.append(':');
		buf.append(format2.format(cal.get(Calendar.MINUTE)));
		buf.append(':');
		buf.append(format2.format(cal.get(Calendar.SECOND)));
		buf.append(':');
		buf.append(format3.format(cal.get(Calendar.MILLISECOND)));
		return buf.toString();
	}

}
