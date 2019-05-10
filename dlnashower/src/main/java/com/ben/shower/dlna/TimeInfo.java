package com.ben.shower.dlna;

public class TimeInfo {
	private String mDuring;
	private int mDuringInt;
	private String mRealtime;
	private int mRealtimeInt;

	public boolean setTime(int during, int realtime) {
		int durVal = during < 0 ? 0 : during;
		int rtVal = realtime < 0 ? 0 : realtime;
		if (rtVal > durVal)
			return false;
		mDuringInt = durVal;
		mDuring = secondsToString(mDuringInt, 2);
		mRealtimeInt = rtVal;
		mRealtime = secondsToString(mRealtimeInt, 2);
		return true;
	}

	public boolean setTime(String during, String realtime) {
		int durVal = stringToSeconds(during);
		int rtVal = stringToSeconds(realtime);
		return setTime(durVal, rtVal);
	}

	public boolean update(String realtime) {
		int rtVal = stringToSeconds(realtime);
		if (rtVal > mDuringInt)
			return false;
		mRealtimeInt = rtVal;
		mRealtime = secondsToString(mRealtimeInt, 2);
		return true;
	}

	public int getDuringInt() {
		return mDuringInt;
	}

	public void setDuring(int during) {
		mDuringInt = during < 0 ? 0 : during;
		mDuring = secondsToString(mDuringInt, 2);
	}

	/**
	 * @return the during
	 */
	public String getDuring() {
		return mDuring;
	}

	/**
	 * @param during
	 *            the during to set
	 */
	public void setDuring(String during) {
		setDuring(stringToSeconds(during));
	}

	public int getRealtimeInt() {
		return mRealtimeInt;
	}

	public void setRealtime(int realtime) {
		mRealtimeInt = realtime < 0 ? 0 : realtime;
		mRealtime = secondsToString(mRealtimeInt, 2);
	}

	/**
	 * @return the realtime
	 */
	public String getRealtime() {
		return mRealtime;
	}

	/**
	 * @param realtime
	 *            the realtime to set
	 */
	public void setRealtime(String realtime) {
		setRealtime(stringToSeconds(realtime));
	}

	public static int stringToSeconds(String time) {
		if (null == time)
			return 0;

		final int len = time.length();
		int value = 0;
		for (int i = 0, base = 0, tmp = 0; i < len; i++) {
			char ch = time.charAt(i);
			if (ch == ':') {
				value *= 60;
				base = value;
				tmp = 0;
			} else if (Character.isDigit(ch)) {
				tmp = tmp * 10 + (ch - '0');
				value = base + tmp;
			} else
				break;
		}
		return value;
	}

	/**
	 * 
	 * @param seconds
	 * @param seg
	 *            0:unspcialed, 1:ss, 2:mm:ss 3:hh:mm:ss
	 * @return
	 */
	public static String secondsToString(int seconds, int seg) {
		if (seconds < 0) {
			switch (seg) {
			case 1:
				return "--";
			case 2:
				return "--:--";
			default:
				return "--:--:--";
			}
		}

		int s = seconds;
		if (1 == seg)
			return String.valueOf(s);

		s %= 60;
		seconds /= 60;
		int m = seconds;
		if (2 == seg)
			return String.format(java.util.Locale.US, "%1$d:%2$02d", m, s);

		m %= 60;
		seconds /= 60;
		int h = seconds;
		return String.format(java.util.Locale.US, "%1$02d:%2$02d:%3$02d", h, m,
				s);
	}
}
