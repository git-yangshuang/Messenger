package com.github.messenger.connect;

import com.alibaba.fastjson.JSON;
import com.github.messenger.bean.MessageBean;
import com.github.messenger.thread.LoopThread;
import com.github.messenger.utils.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;


/**
 * Created by yangshuang
 * on 2019/7/29.
 */
public class SocketServerClient extends LoopThread {


    public interface OnReceivedMsgListener {
        void onReceivedMsg(MessageBean message);
    }


    /**
     * 消息队列
     */
    private Queue<MessageBean> messageQueue;
    /**
     * 需要回调的消息
     */
    private HashMap<String, MessageBean> requestBeansForResponse = new HashMap<>();
    /**
     * Socket
     */
    private Socket client;
    /**
     * ServerSocket
     */
    private ServerSocket server;
    /**
     * 端口号
     */
    private int port;
    /**
     * server error信息
     */
    private String serverError;
    /**
     * socket error信息
     */
    private String clientError;
    /**
     * 向外写入消息线程
     */
    private WriteReadThread writeThread;
    /**
     * 从外读取消息线程
     */
    private WriteReadThread readThread;
    /**
     * 随机数
     */
    private Random random = new Random();

    /**
     * 当前server 状态
     * <p>
     * -1 server未监听
     * 666 已连接
     * 888 数据传输中
     * 333 已断开
     */
    private int socketState = -1;
    /**
     * 上次心跳时间
     */
    private long responseTime = 0;
    /**
     * 上次心跳时间
     */
    private long heartTime;
    /**
     * 心跳验证密钥
     */
    private String heartKey = "";
    /**
     * 心跳验证间隔
     */
    private long heartDelay = 3000;



    private OnReceivedMsgListener onReceivedMsgListener;


    public SocketServerClient(int port) {
        this.port = port;
        messageQueue = new LinkedList<>();
    }

    public boolean initServer() {
        System.out.println("initServer...");
        try {
            server = new ServerSocket(port);
            socketState = 333;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            serverError = e.getMessage();
            return false;
        }
    }

    @Override
    protected void loop() {
        if (socketState == -1) {
            boolean result = initServer();
            if (!result) {
                this.stopSelf();
            }
        }
        try {
            if (socketState == 333) {
                if (client != null)
                    client.close();
                heartTime = 0;
                responseTime = 0;
                heartKey = "";
                System.out.println("wair for connect...");
                client = server.accept();
                socketState = 666;
            }
            if (socketState == 666) {
                System.out.println("connected...");
                readThread = new WriteReadThread(client.getInputStream(), null);
                writeThread = new WriteReadThread(null, client.getOutputStream());
                readThread.start();
                writeThread.start();
                socketState = 888;
            }
            if (socketState == 888) {
                System.out.println("data is transporting");
                pleaseWait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void out() {
        this.releaseServer();
    }

    public void resetServer() {
        try {
            if (server != null && server.isBound()) {
                server.close();
            }
        } catch (Exception e) {
        }
        initServer();
    }

    public void releaseServer() {
        try {

            if (writeThread != null && writeThread.isAlive()) writeThread.stopSelf();
            if (readThread != null && readThread.isAlive()) readThread.stopSelf();

            if (client != null && client.isConnected()) {
                client.close();
            }
            client = null;

            if (server != null && server.isBound()) {
                server.close();
            }
            server = null;

            messageQueue.clear();

            heartKey = "";
            heartTime = 0;
            responseTime = 0;

        } catch (Exception e) {
        }
    }

    private synchronized void pleaseWait() throws InterruptedException {
        this.wait();
    }

    private void responseData(MessageBean b) {
        if (b == null) {
            return;
        }
        if (b.getMessageId() == null || b.getMessageId().equals("") || b.getCode() == null || b.getCode().equals("")) {
            clientError = "数据格式错误";
            System.out.println(clientError);
        } else {
            int code = Integer.valueOf(b.getCode());
            switch (code) {
                case MessageBean.REQUEST_DATA:
                    if (requestBeansForResponse.containsKey(b.getMessageId())) {
                        MessageBean messageBean = requestBeansForResponse.remove(b.getMessageId());
                        messageBean.getResponseLisenter().onResponse(b);
                    } else {
                        this.onMsgReceivedFromSocket(b);
                    }
                    break;
            }
        }
    }

    private MessageBean getWaitMessage() {
        MessageBean bean = new MessageBean("" + System.currentTimeMillis() + random.nextInt(999999));
        bean.setCode("" + MessageBean.WAIT_FOR_REQUEST);
        return bean;
    }


    public synchronized void sendMsg(MessageBean bean) {
        this.onMsgReceivedToSend(bean);
    }

    private void onMsgReceivedFromSocket(MessageBean bean) {
        if (onReceivedMsgListener != null) {
            onReceivedMsgListener.onReceivedMsg(bean);
        }
    }

    protected synchronized void onMsgReceivedToSend(MessageBean bean) {
        if (isClientStart()) {
            messageQueue.offer(bean);
        } else if (bean.getResponseLisenter() != null) {
            MessageBean error = new MessageBean();
            error.setErrorCode(MessageBean.ERROR_UNCONNENT);
            error.setErrorMsg(clientError);
            bean.getResponseLisenter().onError(error);
        }
    }

    public synchronized void changeState(int state) {
        this.socketState = state;
        this.notify();
    }

    public boolean isServerStart() {
        return server.isBound();
    }

    public boolean isClientStart() {
        return client.isConnected();
    }

    public String getServerError() {
        return serverError;
    }

    public String getClientError() {
        return clientError;
    }


    public long getHeartTime() {
        return heartTime;
    }

    public void setHeartTime(long heartTime) {
        this.heartTime = heartTime;
    }

    public String getHeartKey() {
        return heartKey;
    }

    public void setHeartKey(String heartKey) {
        this.heartKey = heartKey;
    }

    public void setHeartDelay(long delay) {
        this.heartDelay = delay;
    }

    public void setOnReceivedMsgListener(OnReceivedMsgListener onReceivedMsgListener) {
        this.onReceivedMsgListener = onReceivedMsgListener;
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
                System.out.println("All Stream is null");
            }
        }

        @Override
        protected void out() {
            this.release();
        }

        private void release() {
            try {
                if (inputStream != null) {
                    inputStream.close();
                    inputStream = null;
                }
                if (outputStream != null) {
                    outputStream.close();
                    outputStream = null;
                }
            } catch (Exception e) {

            }
        }

        private void writeToMobile() {
            try {
                long currentTimeMillis = System.currentTimeMillis();
                if (currentTimeMillis - heartTime > heartDelay && currentTimeMillis - responseTime > heartDelay) {
                    if (!heartKey.equals("")) {
                        System.out.println("There is no response for heart message, the key is " + heartKey);
                        this.stopSelf();
                        changeState(333);
                    }
                    heartKey = "heart_check" + currentTimeMillis;
                    heartTime = currentTimeMillis;
                    outputStream.write(heartKey.getBytes());
                    outputStream.write(StreamUtils.END.getBytes());
                    outputStream.flush();
                    System.out.println("flush : " + heartKey);
                }
                if (messageQueue.size() > 0) {
                    MessageBean bean = messageQueue.poll();
                    String json = JSON.toJSONString(bean);
                    outputStream = client.getOutputStream();
                    outputStream.write(json.getBytes());
                    outputStream.write(StreamUtils.END.getBytes());
                    outputStream.flush();
                    System.out.println("flush : " + json);
                }
            } catch (Exception e) {
                e.printStackTrace();
                clientError = e.getMessage();
                if (e instanceof SocketException) {
                    this.stopSelf();
                    changeState(333);
                }
            }

        }

        private void readFromMobile() {
            try {
                String response = StreamUtils.input2String(inputStream);
                if (response == null) return;
                responseTime = System.currentTimeMillis();
                System.out.println("response : " + response);
                heartKey = "";
                heartTime = responseTime;
                if (response.contains("heart_check"))return;
                MessageBean res = JSON.parseObject(response, MessageBean.class);
                responseData(res);
            } catch (Exception e) {
                e.printStackTrace();
                clientError = e.getMessage();
                if (e instanceof SocketException) {
                    this.stopSelf();
                    changeState(333);
                }
            }
        }
    }
}
