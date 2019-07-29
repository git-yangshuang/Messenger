package com.github.messenger.thread;

/**
 * Created by
 * yangshuang on 2019/7/29.
 */
public abstract class LoopThread extends Thread {

    private boolean looping = false;

    @Override
    public synchronized void start() {
        this.looping = true;
        super.start();
    }

    @Override
    public void run() {
        super.run();
        while (looping) {
            this.loop();
        }
        this.out();
    }

    private synchronized void pleaseWait() throws InterruptedException {
        this.wait();
    }

    protected abstract void loop();

    protected abstract void out();

    public void stopSelf(){
        this.looping = false;
    }
}
