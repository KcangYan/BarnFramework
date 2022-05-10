package com.kcang.aop.impl;

import com.kcang.aop.annotaion.AopAnnotationImpl;
import com.kcang.exception.IocWithoutInstanceException;
import com.kcang.ioc.impl.IocAdmin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 初始化的时候运行的类，会把所有添加了AopAnnotationImpl的类实例化并且添加到aop对象代理方法列表中
 */
public class AopAnnotationImplement {
    public static Map<String, Class> aopAnnotationMap = new HashMap<>();
    /**
     * 遍历包下所有class对象
     * @param classList
     * @throws IllegalAccessException
     * @throws InstantiationException
     */
    public void init(List<Class> classList) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, IocWithoutInstanceException {
        for(Class cls : classList){
            AopAnnotationImpl aopAnnotation = (AopAnnotationImpl) cls.getDeclaredAnnotation(AopAnnotationImpl.class);
            if(aopAnnotation != null){
                annotationFunc(cls, aopAnnotation, classList);
            }
        }
    }

    /**
     * 找到添加了自定义注解的方法，并生成自定义注解实例交由aop管理列表代理对应的方法
     * @param cls 添加了AopAnnotationImpl注解的类
     * @param aopAnnotation AopAnnotationImpl注解实体，用来获取自定义注解对象
     * @param classList 包下所有类的列表
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private void annotationFunc(Class cls, AopAnnotationImpl aopAnnotation, List<Class> classList) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, IocWithoutInstanceException {
        String aopMethodProxyName = IocAdmin.getIocInstanceName(cls);
        aopAnnotationMap.put(aopMethodProxyName, aopAnnotation.AopAnnotation());
        AopMethodProxy aopMethodProxy = (AopMethodProxy) IocAdmin.getIocInstance(aopMethodProxyName);
        aopMethodProxy.setOrder(aopAnnotation.order());
        //自定义注解实现
        for(Class clsItem : classList){
            Method[] methods = clsItem.getMethods();
            for(Method method : methods){
                if(method.getDeclaredAnnotation(aopAnnotation.AopAnnotation()) != null){
                    String instanceName = IocAdmin.getIocInstanceName(clsItem);
                    if(instanceName != null){
                        AopCglibEnhancer.cglibEnhancer(instanceName, method, aopMethodProxy, method.getDeclaredAnnotation(aopAnnotation.AopAnnotation()));
                    }else {
                        throw new IocWithoutInstanceException(clsItem.getName()+" 类需托管至容器，请使用容器注解标识！");
                    }
                }
            }
        }
    }
}
