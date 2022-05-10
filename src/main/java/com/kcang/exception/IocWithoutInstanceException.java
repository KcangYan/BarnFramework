package com.kcang.exception;

public class IocWithoutInstanceException extends Exception {
    public IocWithoutInstanceException(String msg){
        super(msg);
    }
}
