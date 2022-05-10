package com.kcang.exception;

/**
 * 强制修改异常错误抛出
 */
public class ForcedChangeException extends Exception {
    public ForcedChangeException(String msg){
        super(msg);
    }
}
