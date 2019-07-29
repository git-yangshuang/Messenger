package com.github.messenger.connect;

import android.os.Handler;
import android.os.Message;

import com.alibaba.fastjson.JSON;
import com.github.messenger.bean.MessageBean;
import com.github.messenger.thread.LoopThread;
import com.github.messenger.utils.StreamUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

public class SocketClient extends LoopThread {


    class MessageHandler extends Handler {
        private static final int WHAT_RESPONSE_DATA = 110;

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case WHAT_RESPONSE_DATA:
                    String response = (String) msg.obj;
                    MessageBean res = JSON.parseObject(response, MessageBean.class);
                    break;
            }
        }
    }

    private String host;
    private int port;
    private Socket socket;
    private LinkedList<MessageBean> messageQueue = new LinkedList<>();
    private LinkedList<String> heartKeys = new LinkedList<>();
    private WriteReadThread readThread, writeThread;
    private int socketState = -1;
    private Handler messageHandler;

    public SocketClient(String host, int port) {
        super();
        this.port = port;
        this.host = host;
        messageHandler = new MessageHandler();
    }

    public synchronized void sendMsg(MessageBean req) {
        messageQueue.offer(req);
    }

    private synchronized void pleaseWait() throws InterruptedException {
        this.wait();
    }

    @Override
    protected void loop() {
        try {
            if (socketState == -1) {
                if (socket != null && socket.isConnected()) {
                    socket.close();
                    socket = null;
                }
                socket = new Socket(host, port);
                socketState = 666;
            }
            if (socketState == 666) {
                readThread = new WriteReadThread(socket.getInputStream(), null);
                writeThread = new WriteReadThread(null, socket.getOutputStream());
                readThread.start();
                writeThread.start();
                socketState = 888;
            }
            if (socketState == 888) {
                pleaseWait();
            }

        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof SocketException) {
                this.stopSelf();
                this.release();
            }
        }
    }

    @Override
    protected void out() {
        this.release();
    }

    public void release() {
        try {
            if (socket != null && socket.isConnected()) {
                socket.close();
                socket = null;
            }
            socketState = -1;
            if (readThread != null && readThread.isAlive()) readThread.stopSelf();
            if (writeThread != null && writeThread.isAlive()) writeThread.stopSelf();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class WriteReadThread extends LoopThread {

        private InputStream inputStream;
        private OutputStream outputStream;

        public WriteReadThread(InputStream inputStream, OutputStream outputStream) {
            super();
            this.inputStream = inputStream;
            this.outputStream = outputStream;
        }

        @Override
        protected void loop() {
            if (inputStream != null) {
                readFromMobile();
            } else if (outputStream != null) {
                writeToMobile();
            } else {
                this.stopSelf();
                this.out();
            }
        }

        @Override
        protected void out() {
            try {
                if (socket != null && socket.isConnected()) {
                    socket.close();
                }
                socket = null;
                if (outputStream != null) {
                    outputStream.close();
                }
                outputStream = null;
                if (inputStream != null) {
                    inputStream.close();
                }
                inputStream = null;
                messageQueue.clear();
                heartKeys.clear();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void writeToMobile() {
            try {
                if (heartKeys.size() > 0) {
                    String key = heartKeys.poll();
                    outputStream.write(key.getBytes());
                    outputStream.write(StreamUtils.END.getBytes());
                    outputStream.flush();
                }
                if (messageQueue.size() > 0) {
                    MessageBean bean = messageQueue.poll();
                    String message = JSON.toJSONString(bean);
                    outputStream.write(message.getBytes());
                    outputStream.write(StreamUtils.END.getBytes());
                    outputStream.flush();
                    System.out.println("flush : " + message);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof SocketException) {
                    this.stopSelf();
                }
            }
        }

        private void readFromMobile() {
            try {
                String response = StreamUtils.input2String(inputStream);
                if (response == null || response.equals("")) return;
                System.out.println("response : " + response);
                if (response.contains("heart_check")) {
                    heartKeys.add(response);
                    return;
                }
                messageHandler.sendMessage(messageHandler.obtainMessage(MessageHandler.WHAT_RESPONSE_DATA, response));
            } catch (Exception e) {
                e.printStackTrace();
                if (e instanceof SocketException) {
                    this.stopSelf();
                }
            }
        }
    }
}
