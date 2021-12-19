package me.liuweiqiang.idempotent;

public class BizException extends RuntimeException {

    private String responseCode;

    public BizException(String responseCode) {
        this.responseCode = responseCode;
    }

    public String getResponseCode() {
        return responseCode;
    }
}
