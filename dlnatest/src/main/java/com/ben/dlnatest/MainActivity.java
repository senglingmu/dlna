package com.ben.dlnatest;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.ben.shower.DeviceManager;
import com.ben.shower.IDevice;
import com.ben.shower.IShower;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private DeviceManager mDeviceManager;
    private DeviceAdapter mAdapterDevice;
    private String TAG = "MainActivity";
    private IShower mShower;
    //https://tx2play1.douyucdn.cn/288016rlols5_4000p.xs
    //http://n1.itc.cn/img8/wb/recom/2016/06/21/146649478474101659.JPEG
    private String test = "https://tx2play1.douyucdn.cn/276200rvoQ82EAno_900.xs";
    private EditText editText;

    private void play(final int state){
        if (null == mShower)
            return;
        new Thread(){
            @Override
            public void run() {
                super.run();
                String str = editText.getText().toString();
                if (str == null || str.isEmpty()||(!str.contains("http")))
                    str = test;
                if (null == mShower)
                    return;
                if (state == 0)
                    mShower.renderImage(str);
                else if (state == 1)
                    mShower.playAudio(str);
                else
                    mShower.playVideo(str);
            }
        }.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.exit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null != mShower)
                    mShower.stopShowImage();
                finish();
            }
        });
        editText = findViewById(R.id.et_conent);
        findViewById(R.id.bt_play_a).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(1);
            }
        });
        findViewById(R.id.bt_play_i).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(0);
            }
        });
        findViewById(R.id.bt_play_v).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play(2);
            }
        });



        ListView lv = findViewById(R.id.lv_device);
        mAdapterDevice = new DeviceAdapter(this);
        lv.setAdapter(mAdapterDevice);
        //TODO: 1.创建设备管理器
        mDeviceManager = DeviceManager.create(this, new DeviceManager.Listener() {
            private Handler mMainHandler;
            private AlertDialog mBusyBox;

            private void runMainTask(Runnable task) {
                if (null == mMainHandler)
                    mMainHandler = new Handler(Looper.getMainLooper());
                mMainHandler.post(task);
            }

            @Override
            public void onBusy(boolean busy, int group, final String reason) {
                if (busy) {//显示loading
                    runMainTask(new Runnable() {
                        @Override
                        public void run() {
                            if (null == mBusyBox) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setMessage(reason);
                                mBusyBox = builder.create();
                            } else
                                mBusyBox.setMessage(reason);
                            mBusyBox.show();
                        }
                    });
                } else if (null != mBusyBox) {//关闭loading
                    runMainTask(new Runnable() {
                        @Override
                        public void run() {
                            mBusyBox.hide();
                        }
                    });
                }
            }

            @Override
            public void onDeviceAdd(final IDevice device) {//有新设备
                System.err.println("onDeviceAdd:"+device.getName());
                runMainTask(new Runnable() {
                    @Override
                    public void run() {
                        mAdapterDevice.add(device);
                    }
                });
            }

            @Override
            public void onDeviceUpdate(final IDevice device) {//有设备改变
                System.err.println("onDeviceUpdate:"+device.getName());
                runMainTask(new Runnable() {
                    @Override
                    public void run() {
                        mAdapterDevice.update(device);
                    }
                });
            }

            @Override
            public void onDeviceRemove(IDevice device) {//有设备关闭
                final String id = null == device ? null : device.getId();
                System.err.println("onDeviceRemove:"+(null == device ? "All" : device.getName()));
                runMainTask(new Runnable() {
                    @Override
                    public void run() {
                        mAdapterDevice.remove(id);
                    }
                });
            }

            @Override
            public void onDeviceSelected(IDevice device) {
                if (null == device)
                    return;
                final String id = device.getId();
                runMainTask(new Runnable() {
                    @Override
                    public void run() {
                        mAdapterDevice.select(id);
                    }
                });
            }

            @Override
            public IDevice selectDevice(IDevice oldDevice, IDevice device) {
                //TODO: 3.选择设备，即连接设备
                if(null != oldDevice)
                    oldDevice.disconnect();
                if (null != device)
                    device.connect(mShowerCallback);
                return device;
            }

            @Override
            public void initDevice(IDevice device) {
            }
        });
        //TODO: 2.开始扫描监听设备
        mDeviceManager.start();
    }

    @Override
    public void onDestroy() {
        //TODO: 销毁
        new Thread(){//一定要在子线程
            @Override
            public void run() {
                super.run();
                Log.e(TAG, "run: " + (null == mDeviceManager));
                if (null != mDeviceManager)
                    mDeviceManager.destroy();
            }
        }.start();
        super.onDestroy();
    }

    //设备连接回调
    private IShower.ICallback mShowerCallback = new IShower.ICallback() {

        @Override
        public void onConnected(IDevice dev) {
            //TODO: 4.设备连接成功后
            IShower shower = (IShower) dev;
            Log.e(TAG, "onConnected: ");
            mShower = shower;
        }

        @Override
        public void onDisconnected(IDevice dev) {
            //TODO: 5.设备断开
        }

        @Override
        public void onQueryState(int errCode, String errDes, int state) {
            toast("onQueryState:" + errDes);
        }

        @Override
        public void onQueryMediaInfo(int errCode, String errDes, String uri, String metaData) {
            toast("onQueryMediaInfo:" + errDes + ",uri:"+uri);
        }

        @Override
        public void onQueryPosition(int errCode, String errDes, int duration, int position) {
            toast("onQueryPosition:" + errDes + ",duration:" +duration);
        }

        @Override
        public void onQueryVolume(int errCode, String errDes, int vol, int minVol, int maxVol) {
            toast("onQueryVolume:" + errDes +",vol:"+ vol + ",minVol:" + minVol +",maxVol:"+ maxVol);
        }

        @Override
        public void onSetVolume(int errCode, String errDes, int vol) {
            toast("onSetVolume:" + errDes + ",vol:" + vol);
        }

        @Override
        public void onSetPath(int errCode, String errDes, String uri, String metaData) {
            toast("onSetPath:" + errDes + ",uri:" + uri);
        }

        @Override
        public void onStop(int errCode, String errDes) {
            toast("onStop:" + errDes);
        }

        @Override
        public void onPlay(int errCode, String errDes) {
            toast("onPlay:" + errDes);
        }

        @Override
        public void onPause(int errCode, String errDes) {
            toast("onPause:" + errDes);
        }

        @Override
        public void onSeek(int errCode, String errDes, int position) {
            toast("onSeek:" + errDes + ",position:" +position);
        }

        @Override
        public void onCompleted(int delayMillis) {
            toast("onCompleted:" + delayMillis);
        }

        @Override
        public void onEventState(int state) {
            toast("onEventState:" + state);
        }

        @Override
        public void onEventURI(String currUrl, String transUrl) {
            toast("onEventURI:currUrl:" + currUrl + ",currUrl:" + transUrl);
        }

        @Override
        public void onEventCurrentURI(String currUrl) {
            toast("onEventCurrentURI:" + currUrl);
        }

        @Override
        public void onEventPlaySpeed(int speed) {
            toast("onEventPlaySpeed:" + speed);
        }

        @Override
        public void onEventDuration(int duration) {
            toast("onEventDuration:" + duration);
        }

        @Override
        public void onEventPosition(int position) {
            toast("onEventPosition:" + position);
        }

        @Override
        public void onEventVolume(int vol) {
            toast("onEventVolume:" + vol);
        }
    };

    private void toast(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, str, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class DeviceAdapter extends BaseAdapter {
        private Context mContext;
        private ArrayList<IDevice> mList = new ArrayList<>();
        private String mCurrID;

        DeviceAdapter(Context context) {
            mContext = context;
        }

        public void add(IDevice device) {
            mList.add(device);
            notifyDataSetChanged();
        }

        public void update(IDevice device) {
            String id = device.getId();
            for (int i = 0, n = mList.size(); i < n; i++) {
                if (id.equals(mList.get(i).getId())) {
                    mList.set(i, device);
                    notifyDataSetChanged();
                    break;
                }
            }
        }

        public void remove(String id) {
            if (null == id) {
                mList.clear();
                notifyDataSetChanged();
            } else {
                for (int i = 0, n = mList.size(); i < n; i++) {
                    if (id.equals(mList.get(i).getId())) {
                        mList.remove(i);
                        notifyDataSetChanged();
                        break;
                    }
                }
            }
        }

        public void select(String id) {
            mCurrID = id;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Object getItem(int position) {
            return mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
        }

        @Override
        public View getView(final int position, View convertView, final ViewGroup parent) {
            CheckBox ctv = (CheckBox) convertView;
            if (null == convertView) {
                ctv = new CheckBox(mContext);
                convertView = ctv;
                ctv.setTextSize(32);
                ctv.setPadding(10,10,10, 10);
                ctv.setGravity(Gravity.CENTER_VERTICAL);
            }

            final IDevice device = (IDevice) getItem(position);
            String id = device.getId();
            ctv.setChecked(id.equals(mCurrID));
            ctv.setText(device.getName());
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e(TAG, ": convertView ");
                    mDeviceManager.selectDevice(device.getId());
                }
            });
            return convertView;
        }
    }
}
