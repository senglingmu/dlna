package com.ben.shower;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.lang.ref.WeakReference;

/**
 * 用于执行异步任务
 */
public class TaskPipe {
    private final String mThreadName;
    private Handler mHandler;

    public TaskPipe() {
        this("TaskThread");
    }

    public TaskPipe(String name) {
        mThreadName = name;
    }

    public final Handler getHandler() {
        if (null == mHandler) {
            HandlerThread thread = new HandlerThread(mThreadName);
            thread.start();
            mHandler = new Handler(thread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.obj instanceof Runnable) {
                        Runnable task = (Runnable) msg.obj;
                        msg.obj = null;
                        task.run();
                    }
                }
            };
        }
        return mHandler;
    }

    public void quit() {
        if (null != mHandler) {
            mHandler.getLooper().quit();
            mHandler = null;
        }
    }

    public void cancelTask(int id) {
        if (null != mHandler)
            mHandler.removeMessages(id);
    }

    public void clear() {
        if (null != mHandler)
            mHandler.removeCallbacksAndMessages(null);
    }

    public boolean doTask(int id, Runnable task) {
        return doTaskDelay(id, task, 0);
    }

    public boolean doTaskDelay(int id, Runnable task, long delayMillis) {
        if (null == task)
            return false;
        Handler h = getHandler();
        if (id != 0)
            h.removeMessages(id);
        Message msg = h.obtainMessage(id, task);
        return h.sendMessageDelayed(msg, delayMillis);
    }

    public boolean hasTask(int id) {
        if (null != mHandler)
            return mHandler.hasMessages(id);
        return false;
    }

    /**
     * terminable task
     *
     * @author zhangbo
     */
    public static class Task implements Runnable {
        private final WeakReference<Object> mWR;
        private final Runnable mTask;

        public Task(Object token, Runnable task) {
            mWR = new WeakReference<>(token);
            mTask = task;
        }

        @Override
        public void run() {
            if (mWR.get() != null)
                mTask.run();
        }
    }
}
