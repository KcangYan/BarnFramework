package com.kcang.aop.impl;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aop 注解通用拦截器，会为每个被代理的对象生成一个拦截器对象。
 * 具体aop实现都在这里
 */
public final class AopMethodInterceptor implements MethodInterceptor {
    //根据代理方法的id 存储 代理这个方法的注解实现类
    private Map<Method, List<AopMethodProxy>> aopMethodProxyListMap;
    //根据代理方法的id 存储 当前注解对象 每个注解实现类都对应方法上唯一的注解对象（一个方法上不能有两个一样的注解）
    private Map<Method, Map<AopMethodProxy, Object>> aopMethodAnnotation;

    public AopMethodInterceptor(){
        this.aopMethodProxyListMap = new HashMap<>();
        this.aopMethodAnnotation = new HashMap<>();
    }

    //写入当前代理方法的具体注解实例，用于获取注解属性
    public void setAnnotation(Method method, AopMethodProxy aopMethodProxy, Object annotation){
        if(this.aopMethodAnnotation.containsKey(method)){
            this.aopMethodAnnotation.get(method).put(aopMethodProxy, annotation);
        }else {
            Map<AopMethodProxy, Object> aopItem = new HashMap<>();
            aopItem.put(aopMethodProxy, annotation);
            this.aopMethodAnnotation.put(method, aopItem);
        }
    }

    //写入代理方法的具体实现类
    public void setAopMethodMap(Method method, AopMethodProxy aopMethodProxy){
        if(this.aopMethodProxyListMap.containsKey(method)){
            List<AopMethodProxy> aopMethodProxyList = this.aopMethodProxyListMap.get(method);
            //根据注解的order属性排序 从小到大
            int len = aopMethodProxyList.size();
            if(aopMethodProxyList.get(0).getOrder() >= aopMethodProxy.getOrder()){
                aopMethodProxyList.add(0,aopMethodProxy);
            }else if(aopMethodProxyList.get(len-1).getOrder() <= aopMethodProxy.getOrder()){
                aopMethodProxyList.add(aopMethodProxy);
            }else {
                for(int i=1;i<len;i++){
                    if( aopMethodProxyList.get(i-1).getOrder() <= aopMethodProxy.getOrder() &&
                        aopMethodProxyList.get(i).getOrder()   >= aopMethodProxy.getOrder() ){
                        aopMethodProxyList.add(i,aopMethodProxy);
                        break;
                    }
                }
            }
            this.aopMethodProxyListMap.put(method, aopMethodProxyList);
        }else {
            List<AopMethodProxy> aopMethodProxyList = new ArrayList<>();
            aopMethodProxyList.add(aopMethodProxy);
            this.aopMethodProxyListMap.put(method, aopMethodProxyList);
        }
    }

    //封装代理过程
    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        Object result = null;
        if(this.aopMethodProxyListMap.containsKey(method)){
            List<AopMethodProxy> aopMethodProxyList = this.aopMethodProxyListMap.get(method);
            Map aopItem = this.aopMethodAnnotation.get(method);
            AopMethod aopMethod = new AopMethod(object, method, args, methodProxy);

            //顺序写入当前方法注解实例以及添加递归责任链
            for(AopMethodProxy aopMethodProxy : aopMethodProxyList){
                Object annotation = aopItem.get(aopMethodProxy);
                //更新注解实现类实例里的当前注解对象
                aopMethodProxy.setAnnotation(annotation);
                //存入递归观察者AopMethodProxy
                aopMethod.setAopMethodProxy(aopMethodProxy);
            }

            //执行所有aop方法
            result = aopMethod.methodRun();
            //result = aopMethodProxyList.get(0).around(aopMethod);

        }else {
            result = methodProxy.invokeSuper(object, args);
        }
        return result;
    }
}
