package com.ben.shower.dlna;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.SystemClock;
import android.util.Log;

import com.ben.shower.DeviceManager;
import com.ben.shower.TaskPipe;

import org.cybergarage.net.HostInterface;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.DeviceList;
import org.cybergarage.upnp.device.DeviceChangeListener;
import org.cybergarage.upnp.device.Disposer;
import org.cybergarage.upnp.device.NotifyListener;
import org.cybergarage.upnp.device.SearchResponseListener;
import org.cybergarage.upnp.ssdp.SSDPPacket;

import java.lang.ref.WeakReference;

/**
 * Created by YangBin on 2019/4/2
 */
public class CyberLinkDeviceManager extends DeviceManager implements DeviceChangeListener, NotifyListener, SearchResponseListener {
    private static final String DMR_TYPE = DLNAUtil.fullDeviceType(DLNAUtil.TYPE_MediaRenderer, 1);
    private Context mContext;
    private MyControlPoint mCP;

    public CyberLinkDeviceManager(Context context, Listener listener) {
        super(context, listener);
        mContext = context.getApplicationContext();
        HostInterface.USE_ONLY_IPV4_ADDR = true;
        HostInterface.USE_LOOPBACK_ADDR = true;
        mCP = new MyControlPoint();
        mCP.addDeviceChangeListener(this);
        mCP.addNotifyListener(this);
        mCP.addSearchResponseListener(this);
        // auto renewsubcrible
        mCP.setNMPRMode(true);
    }

    @Override
    protected void onStart() {
        if (null != mCP)
            mCP.doStart();
    }

    @Override
    public void destroy() {
        if (null != mCP)
            mCP.stop();
        super.destroy();
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

    @Override
    public final void refreshDevices() {
        super.refreshDevices();
        getTaskPipe().doTask(0, new Runnable() {
            @Override
            public void run() {
                mCP.search();
            }
        });
    }

    @Override
    public final void refreshStop() {
        super.refreshStop();
        mCP.stopSearch();
    }

    private boolean addDevice(Device device, int stack) {
        final String udn = device.getUDN();System.err.println("udn:"+udn+" device:"+device.getModelDescription()+" "+device.getDeviceType());
        if (udn.length() <= 5)
            return false;
        boolean bAdded = false;
        if (device.getDeviceType().equals(DMR_TYPE)) {
            mCP.setAutoSearchInterval(device.getLeaseTime());
            onDeviceAdded(new CyberLinkShower(mCP, device, getTaskPipe()));
            bAdded = true;
        }

        if(stack > 0) {
            DeviceList subDevice = device.getDeviceList();
            for(int i = 0, n = subDevice.size(); i < n; i++) {
                if(addDevice(subDevice.getDevice(i), stack - 1))
                    bAdded = true;
            }
        }
        return bAdded;
    }

    private boolean removeDevice(Device device, int stack) {
        final String udn = device.getUDN();

        boolean bRemoved = onDeviceRemoved(udn);
        if(stack > 0) {
            DeviceList subDevice = device.getDeviceList();
            for(int i = 0, n = subDevice.size(); i < n; i++) {
                if(removeDevice(subDevice.getDevice(i), stack - 1))
                    bRemoved = true;
            }
        }
        return bRemoved;
    }

    @Override
    public void deviceAdded(Device dev) {System.err.println("deviceAdded:"+dev);
        addDevice(dev, 1);
    }

    @Override
    public void deviceRemoved(Device dev) {System.err.println("deviceRemoved:"+dev);
        removeDevice(dev, 1);
    }

    @Override
    public void deviceSearchResponseReceived(SSDPPacket ssdpPacket) {
        android.util.Log.w("CyberLink", "deviceSearchResponseReceived:"+ssdpPacket.isRootDevice()+"||"+ssdpPacket.getST()+","+ssdpPacket.getRemoteAddress());
        // onSearchEnd();
    }

    @Override
    public void deviceNotifyReceived(SSDPPacket ssdpPacket) {
        android.util.Log.w("CyberLink", "deviceNotifyReceived:"+ssdpPacket.getST()+","+ssdpPacket.getRemoteAddress());
        // onSearchEnd();
    }

    private class MyControlPoint extends ControlPoint {
        private final int MSEARCH_MAX = 5;// s
        private final int MSEARCH_INTERVAL = 30;// s
        private final int MSEARCH_MAN_INTERVAL = 2;// s
        private final int EXPIRECHECK_INTERVAL = 10;// s

        private final int ID_START = 1;
        private final int ID_SEARCH = 2;
        private final int ID_REFRESH = 3;
        private final int ID_SEARCH_END = 4;
        private final int ID_EXPIRED = 5;
        private TaskPipe mPipe;
        private final Runnable mRefreshTask;

        private Runnable mExpiredTask;
        private int mSearchInterval = MSEARCH_INTERVAL;
        private long mLastSearchInterval = 0;

        MyControlPoint() {
            setSearchMx(MSEARCH_MAX);
            setExpiredDeviceMonitoringInterval(EXPIRECHECK_INTERVAL);

            final WeakReference<ControlPoint> wr = new WeakReference<ControlPoint>(this);
            mRefreshTask = new Runnable() {
                @Override
                public void run() {
                    ControlPoint cp = wr.get();
                    if (cp != null)
                        cp.search();
                }
            };
            Log.e("yyy", "MyControlPoint: TaskPipe 被实例化");
            mPipe = new TaskPipe("DeviceSearch");
        }

        private final Runnable mSearchEndTask = new Runnable() {
            @Override
            public void run() {
                onSearchEnd();
            }
        };

        /**
         * @param interVal unit:seconds
         */
        void setAutoSearchInterval(int interVal) {
            if (interVal <= MSEARCH_MAX)
                return;
            if (interVal < mSearchInterval)
                mSearchInterval = interVal;
        }

        public void doStart() {
            mPipe.doTask(ID_START, new Runnable() {
                @Override
                public void run() {
                    start();
                }
            });
        }

        @Override
        public void search(String target, int mx) {
            final long currTime = SystemClock.elapsedRealtime();
            if (currTime < mLastSearchInterval + MSEARCH_MAN_INTERVAL * 1000)
                return;
            mLastSearchInterval = currTime;
            Log.e("yyy", "MyControlPoint: " + (null == mPipe));
            if (null == mPipe)
                mPipe = new TaskPipe("DeviceSearch");
            mPipe.cancelTask(ID_SEARCH);
            mPipe.cancelTask(ID_REFRESH);
            //handler.removeCallbacks(mSearchEndTask);
            //handler.removeCallbacks(mRefreshTask);
            mPipe.cancelTask(ID_EXPIRED);
            //handler.removeCallbacks(mExpiredTask);

            // check expire time manually.
            Disposer disposer = getDeviceDisposer();
            if (null != disposer) {
                Thread t = disposer.getThreadObject();
                if (null != t)
                    t.interrupt();
            }

            final int millisSearchMax = getSearchMx() * 1000;
            final long searchTime = currTime - (millisSearchMax >> 1);
            super.search(target, mx);

            final WeakReference<MyControlPoint> wr = new WeakReference<MyControlPoint>(this);
            mExpiredTask = new Runnable() {
                @Override
                public void run() {
                    MyControlPoint cp = wr.get();
                    if (cp == null)
                        return;
                    DeviceList list = cp.getDeviceList();
                    final int count = list.size();
                    for (int i = 0; i < count; i++) {
                        Device device = list.getDevice(i);
                        if (device.getTimeStamp() < searchTime) {
                            //not updated
                            System.err.println("timeout:"+device.getTimeStamp()+" "+searchTime);
                            cp.removeDevice(device);
                        }
                    }
                }
            };
            mPipe.doTaskDelay(ID_EXPIRED, mExpiredTask, millisSearchMax + millisSearchMax);
            //handler.postDelayed(mExpiredTask, millisSearchMax + millisSearchMax);
            mPipe.doTaskDelay(ID_REFRESH, mRefreshTask, mSearchInterval * 1000);
            //handler.postDelayed(mRefreshTask, mSearchInterval * 1000);

            mPipe.doTaskDelay(ID_SEARCH_END, mSearchEndTask, getSearchMx() * 1000);
            //handler.postDelayed(mSearchEndTask, getSearchMx() * 1000);
        }

        public void stopSearch() {
            mLastSearchInterval = 0;
            if(null != mPipe) {
                mPipe.getHandler().removeCallbacksAndMessages(null);
                mSearchEndTask.run();
            }
            /*if (null != mHandler) {
                mHandler.removeCallbacks(mSearchEndTask);
                mSearchEndTask.run();
                mHandler.removeCallbacks(mRefreshTask);
                if (null != mExpiredTask)
                    mHandler.removeCallbacks(mExpiredTask);
            }*/
        }

        @Override
        public boolean stop() {
            mLastSearchInterval = 0;
            stopSearch();
            if(null != mPipe) {
                mPipe.quit();
                mPipe = null;
            }
            /*if (null != mHandler) {
                mHandler.removeCallbacks(mSearchEndTask);
                mSearchEndTask.run();
                mHandler.removeCallbacks(mRefreshTask);
                if (null != mExpiredTask)
                    mHandler.removeCallbacks(mExpiredTask);
                mHandler.getLooper().quit();
                mHandler = null;
            }*/
            boolean ret = super.stop();
            DeviceList list = getDeviceList();
            final int count = list.size();
            for (int i = 0; i < count; i++)
                removeDevice(list.getDevice(i));
            mSearchInterval = MSEARCH_INTERVAL;
            return ret;
        }
    }
}
