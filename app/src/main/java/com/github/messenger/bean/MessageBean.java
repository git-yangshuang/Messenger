package com.github.messenger.bean;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;

/**
 * Created by
 * yangshuang on 2019/7/25.
 */
public class MessageBean implements Serializable {

    public static final long serialVersionUID = 11L;

    public static final int WAIT_FOR_REQUEST = 901;
    public static final int REQUEST_DATA = 902;

    public static final int NO_RESPONSE = 999;

    public static final int NEED_RESPONSE = 101;
    public static final int NEED_NO_RESPONSE = 202;

    public static final int ERROR_TIME_OUT = 1001;
    public static final int ERROR_JSON_EXCEPTION = 1002;
    public static final int ERROR_UNCONNENT = 1003;

    private String time;
    private String info;
    private String messageId;
    private String code;
    private int needResponse = -1;
    private int errorCode;
    private String errorMsg;

    @JSONField(serialize = false, deserialize = false)
    private ResponseLisenter responseLisenter;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public ResponseLisenter getResponseLisenter() {
        return responseLisenter;
    }

    public void setResponseLisenter(ResponseLisenter responseLisenter) {
        this.responseLisenter = responseLisenter;
    }

    public int getNeedResponse() {
        return needResponse;
    }

    public void setNeedResponse(int needResponse) {
        this.needResponse = needResponse;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public MessageBean() {
    }

    public MessageBean(String messageId) {
        this.messageId = messageId;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public interface ResponseLisenter {
        void onResponse(MessageBean bean);

        void onError(MessageBean bean);
    }
}
