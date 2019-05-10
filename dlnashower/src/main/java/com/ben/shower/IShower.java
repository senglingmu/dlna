package com.ben.shower;

/**
 * Created by YangBin on 2019/4/2
 */
public interface IShower extends IDevice {
    int STATE_UNKNOWN = 0;
    int STATE_STOP = 1;
    int STATE_TRANSITIONING = 2;
    int STATE_PLAYING = 3;
    int STATE_PAUSE = 4;

    String getId();

    String getName();

    void renderImage(String path);

    void stopShowImage();

    void playVideo(String path);

    void playAudio(String path);

    public interface ICallback extends IDevice.ICallback {
        void onQueryState(int errCode, String errDes, int state);

        void onQueryMediaInfo(int errCode, String errDes, String uri, String metaData);

        void onQueryPosition(int errCode, String errDes, int duration, int position);

        void onQueryVolume(int errCode, String errDes, int vol, int minVol, int maxVol);

        void onSetVolume(int errCode, String errDes, int vol);

        void onSetPath(int errCode, String errDes, String uri, String metaData);

        void onStop(int errCode, String errDes);

        void onPlay(int errCode, String errDes);

        void onPause(int errCode, String errDes);

        void onSeek(int errCode, String errDes, int position);

        void onCompleted(int delayMillis);

        void onEventState(int state);

        void onEventURI(String currUrl, String transUrl);

        void onEventCurrentURI(String currUrl);

        void onEventPlaySpeed(int speed);

        void onEventDuration(int duration);

        void onEventPosition(int position);

        void onEventVolume(int vol);
    }
}
