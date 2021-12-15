package me.liuweiqiang.idempotent;

public class UnknownException extends Exception {

    public UnknownException(Throwable e) {
        super(e);
    }
}
