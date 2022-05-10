package com.kcang.aop.impl;

import com.kcang.exception.IocWithoutInstanceException;
import com.kcang.ioc.impl.IocAdmin;
import net.sf.cglib.proxy.Enhancer;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class AopCglibEnhancer {
    /**
     * 存储每个被代理的类的专属方法拦截器，代理行为在方法拦截器里实现。
     * 需要实现AopMethodProxy接口，并传进来
     */
    private static Map<String, AopMethodInterceptor> version = new HashMap<>();

    /**
     * 内部使用的aop方法代理类实现 与容器结合使用
     * @param instanceName 容器中对应类名
     * @param method 代理方法对象
     * @param aopMethodProxy aop具体实现类
     * @param annotation aop自定义注解对象 用于传递自定义注解参数
     * @throws IocWithoutInstanceException ioc异常
     */
    static void cglibEnhancer(String instanceName, Method method, AopMethodProxy aopMethodProxy, Object annotation) throws IocWithoutInstanceException {
        if(version.containsKey(instanceName)){
            version.get(instanceName).setAopMethodMap(method, aopMethodProxy);
            version.get(instanceName).setAnnotation(method, aopMethodProxy, annotation);
        }else {
            Enhancer enhancer = new Enhancer();
            enhancer.setSuperclass(IocAdmin.getIocInstance(instanceName).getClass());
            AopMethodInterceptor methodInterceptor = new AopMethodInterceptor();

            methodInterceptor.setAopMethodMap(method,aopMethodProxy);
            methodInterceptor.setAnnotation(method, aopMethodProxy, annotation);

            enhancer.setCallback(methodInterceptor);

            Object obj = enhancer.create();
            //用生成的代理类的对象替换容器里的原来的对象
            IocAdmin.replaceIocInstance(instanceName, obj);
            version.put(instanceName,methodInterceptor);
        }
    }

    /**
     * 直接创建cglib生成的代理类对象
     * @param cls 需要代理的对象
     * @param methodInterceptor 对象方法拦截器对象
     * @return 代理对象
     */
    public static Object cglibEnhancer(Class cls, AopMethodInterceptor methodInterceptor){
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(cls);
        enhancer.setCallback(methodInterceptor);
        return enhancer.create();
    }
}
