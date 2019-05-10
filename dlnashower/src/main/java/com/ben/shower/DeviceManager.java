package com.ben.shower;

/**
 * Created by YangBin on 2019/4/2
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.ben.shower.dlna.CyberLinkDeviceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhangbo
 */
public abstract class DeviceManager {
    public static final int DEVICE_IMAGE = 0x01;
    public static final int DEVICE_AUDIO = 0x02;
    public static final int DEVICE_VIDEO = 0x04;

    public static DeviceManager create(Context context, Listener listener) {
        return new CyberLinkDeviceManager(context, listener);
    }

    private final HashMap<String, IDevice> mDevMap = new HashMap<>();
    private final List<String> mDevList = new ArrayList<String>();
    private final Listener mListener;

    private final TaskPipe mTaskPipe;
    private Context mContext;
    private boolean mbValid;

    private String mSelectID;
    private IDevice mCurrDev;

    public interface Listener {
        int BG_DEV = 0;

        void onBusy(boolean busy, int group, String reason);

        void onDeviceAdd(IDevice device);

        void onDeviceUpdate(IDevice device);

        void onDeviceRemove(IDevice device);

        void onDeviceSelected(IDevice device);

        IDevice selectDevice(IDevice oldDevice, IDevice device);

        void initDevice(IDevice device);
    }

    protected DeviceManager(Context context, Listener listener) {
        mContext = context.getApplicationContext();
        mListener = listener;
        mTaskPipe = new TaskPipe();
    }

    public final TaskPipe getTaskPipe() {
        return mTaskPipe;
    }

    protected abstract void onStart();

    public final boolean start() {
        onStart();
        if (isNetworkValid())
            onEnvChanged(true);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        mContext.registerReceiver(mNetStateReceiver, filter);
        return true;
    }

    //检查网络状态
    private boolean isNetworkValid() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (null != cm) {
            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (null != networkInfo && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                if (NetworkInfo.State.CONNECTED.equals(networkInfo.getState()))
                    return true;
            }
        }
        return false;
    }

    private BroadcastReceiver mNetStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                if (isNetworkValid())
                    onEnvChanged(true);
                else
                    onEnvChanged(false);
            }
        }
    };

    /**
     * 环境（网络）准备好
     *
     * @param bValid
     */
    protected void onEnvChanged(boolean bValid) {
        if (mbValid == bValid)
            return;
        mbValid = bValid;
        if (bValid) {
            clearDevices();
            refreshDevices();
        } else
            refreshStop();
    }

    public final IDevice getDevice(String id) {
        synchronized (mDevMap) {
            return mDevMap.get(id);
        }
    }

    public final IDevice getCurrent() {
        return mCurrDev;
    }

    public final String getSelectID() {
        return mSelectID;
    }

    public void destroy() {
        try {
            mContext.unregisterReceiver(mNetStateReceiver);
        } catch (IllegalArgumentException e) {
        }
        refreshStop();
        getTaskPipe().quit();
        doSelectDevice(null);
        synchronized (mDevMap) {
            mCurrDev = null;
            mDevList.clear();
            for (Map.Entry<String, IDevice> entry : mDevMap.entrySet()) {
                IDevice dev = entry.getValue();
                if (null != dev)
                    dev.onDestroy();
            }
            mDevMap.clear();
        }
    }

    public void refreshDevices() {
        notifyBusy(true);
    }

    public void refreshStop() {
        notifyBusy(false);
    }

    protected final void notifyBusy(boolean busy) {
        mListener.onBusy(busy, Listener.BG_DEV, "search");
    }

    public List<IDevice> getList(List<IDevice> list) {
        synchronized (mDevMap) {
            final int count = mDevList.size();
            if (null == list)
                list = new ArrayList<>(count);
            else
                list.clear();
            for (int i = 0; i < count; i++) {
                IDevice dev = mDevMap.get(mDevList.get(i));
                if (null != dev)
                    list.add(dev);
            }
            return list;
        }
    }

    public boolean selectDevice(String id) {
        IDevice device;
        synchronized (mDevMap) {
            device = mDevMap.get(id);
        }
        if (!doSelectDevice(device))
            return false;
        mSelectID = id;
        return true;
    }

    private boolean doSelectDevice(IDevice target) {
        IDevice curr = mCurrDev;
        if (target == curr)
            return true;
        if (null != target)
            mListener.initDevice(target);
        IDevice device = mListener.selectDevice(curr, target);
        if (device != target) {
            //恢复
            device = mListener.selectDevice(device, curr);
        }
        if (curr != device) {
            synchronized (mDevMap) {
                mCurrDev = device;
            }
            mListener.onDeviceSelected(device);
        }
        return device == target;
    }

    public boolean addDevice(IDevice dev) {
        return onDeviceAdded(dev);
    }

    public boolean removeDevice(String id) {
        return onDeviceRemoved(id);
    }

    /**
     * subclass called this
     *
     * @param dev
     * @return
     */
    protected boolean onDeviceAdded(IDevice dev) {
        final String id = dev.getId();
        if (!dev.onCreate())
            return false;
        IDevice device;
        synchronized (mDevMap) {
            device = mDevMap.get(id);
            mDevMap.put(id, dev);
            if (null == device)
                mDevList.add(id);
        }
        if (null == device) {
            mListener.onDeviceAdd(dev);
            // auto select the last one
            if (id.equals(mSelectID))
                doSelectDevice(dev);
        } else
            mListener.onDeviceUpdate(dev);
        return true;
    }

    /**
     * subclass called this
     *
     * @param id
     * @return
     */
    protected boolean onDeviceRemoved(String id) {
        IDevice dev;
        synchronized (mDevMap) {
            dev = mDevMap.remove(id);
            if (dev == null)
                return false;
            mDevList.remove(id);
        }
        mListener.onDeviceRemove(dev);

        if (null != mSelectID && mSelectID.equals(id))
            doSelectDevice(null);
        dev.onDestroy();
        return true;
    }

    protected void onSearchEnd() {
        notifyBusy(false);
    }

    protected void clearDevices() {
        synchronized (mDevMap) {
            mDevList.clear();
            for (Map.Entry<String, IDevice> entry : mDevMap.entrySet()) {
                IDevice dev = entry.getValue();
                if (null != dev)
                    dev.onDestroy();
            }
            mDevMap.clear();
        }
        doSelectDevice(null);
        mListener.onDeviceRemove(null);
    }
}
