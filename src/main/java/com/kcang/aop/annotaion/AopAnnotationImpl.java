package com.kcang.aop.annotaion;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记为aop注解的实现类
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.TYPE})
public @interface AopAnnotationImpl {
    /**
     * 关联你的自定义注解
     * @return
     */
    Class AopAnnotation();

    /**
     * 注解执行顺序，由order从小到大执行
     * @return
     */
    int order() default 10;

    /**
     * 托管ioc容器的实例使用名字
     */
    String name() default "";
}
