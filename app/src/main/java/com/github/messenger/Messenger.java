package com.github.messenger;

import com.github.messenger.bean.MessageBean;
import com.github.messenger.connect.SocketClient;
import com.github.messenger.connect.SocketServerClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Messenger {

    public static final int TYPE_MOBILE = 9999;
    public static final int TYPE_SERVER = 8888;

    public static final String LOCALHOST = "127.0.0.1";

    private int port;
    private int type;
    private String host;
    private SocketServerClient serverClient;
    private SocketClient client;
    private static List<Integer> ports = new ArrayList<>();
    private Random random = new Random();

    private Messenger(String host, int port, int type) {
        this.port = port;
        this.type = type;
        ports.add(port);
        if (type == TYPE_SERVER) {
            serverClient = new SocketServerClient(port);
            serverClient.start();
        } else if (type == TYPE_MOBILE) {
            client = new SocketClient(host, port);
            client.start();
        }

    }

    public static Messenger create(int port, int type) {
        return create(Messenger.LOCALHOST, port, type);
    }

    /**
     * create a messager
     *
     * @param host 连接地址（服务端为）
     * @param port 端口号
     * @param type {@link Messenger.TYPE_MOBILE} or {@link Messenger.TYPE_SERVER}
     */
    public static Messenger create(String host, int port, int type) {
        if (ports.contains(port)) return null;
        return new Messenger(host, port, type);
    }

    public void release() {
        if (serverClient != null)
            serverClient.stopSelf();
        if (client != null)
            client.stopSelf();
        ports.remove(Integer.valueOf(this.port));
    }

    private String getMessageID() {
        return "" + System.currentTimeMillis() + (100000 + random.nextInt(899999));
    }

    public void sendMessage(MessageBean bean) {
        send(bean);
    }

    public void sendMessage(MessageBean bean, MessageBean.ResponseLisenter lisenter) {
        bean.setResponseLisenter(lisenter);
        bean.setNeedResponse(MessageBean.NEED_RESPONSE);
        send(bean);
    }

    public void sendMessage(String s) {
        MessageBean bean = new MessageBean(getMessageID());
        bean.setResponseLisenter(null);
        bean.setInfo(s);
        bean.setNeedResponse(MessageBean.NEED_NO_RESPONSE);
        send(bean);
    }

    public void sendMessage(String s, MessageBean.ResponseLisenter lisenter) {
        MessageBean bean = new MessageBean(getMessageID());
        bean.setResponseLisenter(lisenter);
        bean.setInfo(s);
        bean.setNeedResponse(MessageBean.NEED_RESPONSE);
        send(bean);
    }

    private void send(MessageBean bean) {
        if (bean == null) return;
        if (type == TYPE_SERVER) {
            serverClient.sendMsg(bean);
        } else if (type == TYPE_MOBILE) {
            client.sendMsg(bean);
        }
    }


}
