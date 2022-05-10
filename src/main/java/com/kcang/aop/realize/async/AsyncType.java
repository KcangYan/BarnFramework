package com.kcang.aop.realize.async;

public enum AsyncType {
    /**
     * 普通队列，如果核心线程都在运行会先存在队列等待，队列满了 才会创建新的线程执行
     * 队列和线程都满了 会报错但不会终止程序运行
     */
    ArrayBlocking,
    /**
     * 同步交换队列，提交任务就会执行，除非线程池满了
     */
    Synchronous,
    /**
     * 自定义线程池在容器中的名字
     */
    CustomThreadPool
}
