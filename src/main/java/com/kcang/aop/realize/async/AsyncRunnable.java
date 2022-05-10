package com.kcang.aop.realize.async;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 标识异步启动类
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.METHOD})
public @interface AsyncRunnable {
    AsyncType type() default AsyncType.ArrayBlocking;
    /**
     *  当且仅当type = CustomThreadPool 时生效 为传入线程池实例在容器中的名字
     *   线程池实例为 ThreadPoolExecutor 对象
     */
    String name() default "";
}