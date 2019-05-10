package com.ben.shower;

public interface IDevice {
	String MAN_SELF = "Robelf";
	Object getDevice();

	String getManufacture();
	
	String getURL();

	String getId();

	void setName(String localName);

	String getName();

	boolean isConnected();

	/**
	 * connect this device
	 * 
	 * @param cb
	 *            callback
	 */
	boolean connect(ICallback cb);

	/**
	 * disconnect this player
	 */
	void disconnect();

	/**
	 * create
	 */
	boolean onCreate();
	/**
	 * removed
	 */
	void onDestroy();

	public interface ICallback {
		void onConnected(IDevice dev);

		void onDisconnected(IDevice dev);
	}
}
