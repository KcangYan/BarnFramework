package com.kcang.external;

/**
 * 标识启动类
 */
public abstract class MainRunnable {
    /**
     * 设置启动类级别，order越小执行顺序越高，越先被执行
     */
    public int order = 1;
    public abstract void run();
}
