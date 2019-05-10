package com.ben.shower.dlna;

import com.ben.shower.IDevice;
import com.ben.shower.IShower;
import com.ben.shower.TaskPipe;

import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.Argument;
import org.cybergarage.upnp.ArgumentList;
import org.cybergarage.upnp.ControlPoint;
import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.UPnP;
import org.cybergarage.upnp.event.EventListener;
import org.cybergarage.xml.Attribute;
import org.cybergarage.xml.Node;
import org.cybergarage.xml.Parser;
import org.cybergarage.xml.ParserException;
import org.cybergarage.xml.XML;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/**
 * Created by YangBin on 2019/4/2
 */
public class CyberLinkShower implements IShower, IKeyValueSet<ArgumentList, String, String>, EventListener {
    private static final int DLNA_INSTANCE_ID = 0;
    static final String TYPE_AVTransport = DLNAUtil.fullServiceType(DLNAUtil.SERVICENAME_AVTransport);
    static final String TYPE_RenderingControl = DLNAUtil.fullServiceType(DLNAUtil.SERVICENAME_RenderingControl);

    public static final String ENUM_STATE_STOPED = "STOPPED";
    public static final String ENUM_STATE_PLAYING = "PLAYING";
    public static final String ENUM_STATE_TRANSITIONING = "TRANSITIONING";
    public static final String ENUM_STATE_PAUSED = "PAUSED_PLAYBACK";
    public static final String ENUM_STATE_NOMEDIA = "NO_MEDIA_PRESENT";

    private static final int TASK_CONNECT = 1;
    private static final int TASK_RENDER = 2;
    private static final int TASK_AUDIO = 3;

    private final Device mDevice;
    private String mLocalName;
    private IDevice.ICallback mCallback;

    private Service mAvTransport;
    private Service mRenderingControl;

    private final ControlPoint mCP;
    private final TaskPipe mTaskPipe;

    CyberLinkShower(ControlPoint cp, Device dev, TaskPipe pipe) {
        mCP = cp;
        mDevice = dev;
        mTaskPipe = pipe;
    }

    @Override
    public Object getDevice() {
        return mDevice;
    }

    @Override
    public String getManufacture() {
        return mDevice.getManufacture();
    }

    @Override
    public String getURL() {
        return mDevice.getLocation();
    }

    @Override
    public String getId() {
        return mDevice.getUDN();
    }

    @Override
    public void setName(String name) {
        mLocalName = name;
    }

    @Override
    public String getName() {
        if (null != mLocalName)
            return mLocalName;
        return mDevice.getFriendlyName();
    }

    @Override
    public boolean isConnected() {
        return null != mCallback;
    }

    @Override
    public boolean connect(final IDevice.ICallback cb) {
        if (null == cb)
            return false;
        if (null != mCallback) {
            cb.onConnected(this);
            return true;
        }
        mAvTransport = mDevice.getService(TYPE_AVTransport);
        mRenderingControl = mDevice.getService(TYPE_RenderingControl);
        if (null == mAvTransport)
            return false;

        final Service avTransport = mAvTransport;
        final Service renderingControl = mRenderingControl;
        final IDevice device = this;
        final EventListener eventListener = this;
        Runnable task = new Runnable() {
            @Override
            public void run() {
                mCP.addEventListener(eventListener);
                do {
                    if (!mCP.subscribe(avTransport))
                        break;
                    if (null != renderingControl) {
                        if (!mCP.subscribe(renderingControl))
                            break;
                    }
                    mCallback = cb;
                    cb.onConnected(device);
                    return;
                } while (false);
                doDisconnect();
            }
        };

        return mTaskPipe.doTask(TASK_CONNECT, task);
    }

    @Override
    public void disconnect() {
        if (null == mCallback)
            return;
        mCallback.onDisconnected(this);
        mCallback = null;
        doDisconnect();
    }

    private void doDisconnect() {
        final Service avTransport = mAvTransport;
        final Service renderingControl = mRenderingControl;
        final EventListener eventListener = this;
        Runnable task = new Runnable() {
            @Override
            public void run() {
                mCP.removeEventListener(eventListener);
                if (null != avTransport && avTransport.isSubscribed()) {
                    mCP.unsubscribe(avTransport);
                }

                if (null != renderingControl && renderingControl.isSubscribed()) {
                    mCP.unsubscribe(renderingControl);
                }
            }
        };
        mTaskPipe.doTask(TASK_CONNECT, task);
        mAvTransport = null;
        mRenderingControl = null;
    }

    @Override
    public boolean onCreate() {
        if (null == mDevice.getService(TYPE_AVTransport))
            return false;
        return true;
    }

    @Override
    public void onDestroy() {
        disconnect();
    }
//结果和通知
    @Override
    public void eventNotifyReceived(String uuid, long seq, String _name, String _value) {
        if (!(mCallback instanceof ICallback))
            return;
        ICallback callback = (ICallback) mCallback;
        // check uuid
        do {
            if (null != mAvTransport && mAvTransport.getSID().equals(uuid))
                break;
            if (null != mRenderingControl && mRenderingControl.getSID().equals(uuid))
                break;
            return;
        } while (false);
System.err.println("_value::"+_value);
        _value = XML.unescapeXMLChars(_value);
        if (!_value.startsWith("<"))
            return;
        Node root;
        try {
            InputStream inp = new ByteArrayInputStream(_value.getBytes("UTF-8"));
            Parser parser = UPnP.getXMLParser();
            root = parser.parse(inp);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        } catch (ParserException e) {
            e.printStackTrace();
            return;
        }
        if (null == root)
            return;

        root = root.getNode(DLNAUtil.VAR_INSTANCEID);
        if (null == root)
            return;

        String name;
        String value;
        Attribute attr = root.getAttribute(DLNAUtil.ATTR_VALUE);
        if (null == attr)
            value = root.getValue();
        else
            value = attr.getValue();

        final int count = root.getNNodes();
        if (0 == count)
            return;

        for (int i = 0; i < count; i++) {
            Node node = root.getNode(i);
            name = node.getName();
            attr = node.getAttribute(DLNAUtil.ATTR_VALUE);
            if (null == attr)
                value = node.getValue();
            else
                value = attr.getValue();
            if (name.equals(DLNAUtil.VAR_STATE)) {
                int state = matchState(value);
                callback.onEventState(state);
            } else if (name.equals(DLNAUtil.VAR_TRANSPORT_URI)) {
                callback.onEventCurrentURI(value);
            } else if (name.equals(DLNAUtil.VAR_PLAY_SPEED)) {
                int speed = DLNAUtil.parseInt(value, 1);
                callback.onEventPlaySpeed(speed);
            } else if (name.equals(DLNAUtil.VAR_TRACK_DURATION)) {
                int tt = TimeInfo.stringToSeconds(value);
                callback.onEventDuration(tt);
            } else if (name.equals(DLNAUtil.VAR_TRACK_POSITION)) {
                int tt = TimeInfo.stringToSeconds(value);
                callback.onEventPosition(tt);
            } else if (name.equals(DLNAUtil.VAR_VOLUME)) {
                int level = Integer.valueOf(value);
                callback.onEventVolume(level);
            }
        }
    }

    @Override
    public ArgumentList putKeyValue(ArgumentList target, String key, String value) {
        Argument arg = target.getArgument(key);
        if (null == arg)
            return target;
        arg.setValue(value);
        return target;
    }

    @Override
    public String getKeyValue(ArgumentList target, String key) {
        Argument arg = target.getArgument(key);
        if (null == arg)
            return "";
        return arg.getValue();
    }

    private static int matchState(String value) {
        if (value != null) {
            if (value.equals(ENUM_STATE_PLAYING))
                return STATE_PLAYING;
            else if (value.equals(ENUM_STATE_PAUSED))
                return STATE_PAUSE;
            else if (value.equals(ENUM_STATE_TRANSITIONING))
                return STATE_TRANSITIONING;
            else if (value.equals(ENUM_STATE_STOPED))
                return STATE_STOP;
        }
        return STATE_UNKNOWN;
    }

    @Override
    public void renderImage(final String url) {
        if (!(mCallback instanceof ICallback))
            return;
        Service service = mAvTransport;System.err.println("renderImage:"+url);
        if (null == service)
            return;
        final Action action = service
                .getAction(DLNAUtil.ACTION_SetAVTransportURI);System.err.println("renderImage:"+action);
        if (null == action)
            return;

        ArgumentList inParams = action.getArgumentList();
        DLNAUtil.buildSetAVTransportURIParams(this, inParams, DLNA_INSTANCE_ID,
                url, null);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                boolean bRet = action.postControlAction();System.err.println("postControlAction:"+bRet);
                ICallback callback = (ICallback) mCallback;
                if (null == callback)
                    return;
                if (bRet) {
                    callback.onSetPath(0, null, url, null);
                } else {
                    callback.onSetPath(action.getStatus().getCode(),
                            action.getStatus().getDescription(), url, null);
                }

            }
        };

        mTaskPipe.doTask(TASK_RENDER, task);
    }

    @Override
    public void stopShowImage() {
        if (!(mCallback instanceof ICallback))
            return;
        Service service = mAvTransport;
        final Action action = service.getAction(DLNAUtil.ACTION_Stop);
        if (null == action)
            return;
        Runnable task = new Runnable() {
            @Override
            public void run() {
                boolean bRet = action.postControlAction();
                System.err.println("postControlAction:"+bRet);
                ICallback callback = (ICallback) mCallback;
                if (null == callback)
                    return;
                if (bRet) {
                    callback.onStop(0, "");
                } else {
                    callback.onStop(action.getStatus().getCode(), action.getStatus().getDescription());
                }
            }
        };

        mTaskPipe.doTask(TASK_RENDER, task);
    }

    @Override
    public void playVideo(final String url) {
        if (!(mCallback instanceof ICallback))
            return;
        Service service = mAvTransport;
        if (null == service)
            return;
        final Action action = service
                .getAction(DLNAUtil.ACTION_SetAVTransportURI);
        if (null == action)
            return;

        ArgumentList inParams = action.getArgumentList();
        DLNAUtil.buildSetAVTransportURIParams(this, inParams, DLNA_INSTANCE_ID,
                url, null);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                boolean bRet = action.postControlAction();
                ICallback callback = (ICallback) mCallback;
                if (null == callback)
                    return;
                if (bRet) {
                    callback.onSetPath(0, null, url, null);
                } else {
                    callback.onSetPath(action.getStatus().getCode(),
                            action.getStatus().getDescription(), url, null);
                }

            }
        };

        mTaskPipe.doTask(TASK_RENDER, task);
    }

    @Override
    public void playAudio(final String url) {
        if (!(mCallback instanceof ICallback))
            return;
        Service service = mAvTransport;
        if (null == service)
            return;
        final Action action = service
                .getAction(DLNAUtil.ACTION_SetAVTransportURI);
        if (null == action)
            return;

        ArgumentList inParams = action.getArgumentList();
        DLNAUtil.buildSetAVTransportURIParams(this, inParams, DLNA_INSTANCE_ID,
                url, null);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                boolean bRet = action.postControlAction();
                ICallback callback = (ICallback) mCallback;
                if (null == callback)
                    return;
                if (bRet) {
                    callback.onSetPath(0, null, url, null);
                } else {
                    callback.onSetPath(action.getStatus().getCode(),
                            action.getStatus().getDescription(), url, null);
                }

            }
        };

        mTaskPipe.doTask(TASK_AUDIO, task);
    }
}
