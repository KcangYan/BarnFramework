package com.kcang.ioc.impl;

import com.kcang.aop.annotaion.AopAnnotationImpl;
import com.kcang.exception.IocInstanceRepeatException;
import com.kcang.exception.IocWithoutInstanceException;
import com.kcang.ioc.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IocAdmin {
    private static Logger myLogger = LoggerFactory.getLogger(IocAdmin.class);
    /**
     * 存储同样需要托管到ioc容器的注解
     * 这些注解在执行注解实现方法之前会先将注解了的类托管至容器
     */
    private static final List<Class> annotationList = new ArrayList<>();

    static {
        annotationList.add(ManagedInstance.class);
        annotationList.add(Component.class);
        annotationList.add(Service.class);
        annotationList.add(Configuration.class);
        annotationList.add(AopAnnotationImpl.class);
    }

    /**
     * 获取类上面ioc注解的命名，如果类上没有ioc注解则返回null
     * @param cls 类
     * @return
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public static String getIocInstanceName(Class cls) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        String instanceName = null;
        for(Class annotation : annotationList){
            Object annotationObj = cls.getDeclaredAnnotation(annotation);
            if(annotationObj != null){
                Method name = annotation.getMethod("name");
                instanceName = (String) name.invoke(annotationObj);
                instanceName = instanceName.equals("")? cls.getName() : instanceName;
                break;
            }
        }
        return instanceName;
    }

    /**
     * ioc实例托管容器对象 以及对应的添加 获取 和替换ioc容器中实例的方法
     */
    private static Map<String, Object> iocContainer = new ConcurrentHashMap<>();

    /**
     * 往容器中添加实例
     * @param name
     * @param instance
     */
    public static void addIocInstance(String name, Object instance) throws IocInstanceRepeatException {
        if (!iocContainer.containsKey(name)) {
            iocContainer.put(name, instance);
        } else {
            Primary primary = instance.getClass().getDeclaredAnnotation(Primary.class);
            if(primary != null){
                Primary primary2 = iocContainer.get(name).getClass().getDeclaredAnnotation(Primary.class);
                if(primary2 != null){
                    throw new IocInstanceRepeatException(name + " 多个Primary实例，无法判断主次");
                }else {
                    iocContainer.put(name, instance);
                }
            }else {
                throw new IocInstanceRepeatException(name + " 实例在容器中已存在");
            }
        }
    }

    /**
     * 获取实例
     * @param name 实例的全名或者是注解的命名name
     * @return
     */
    public static Object getIocInstance(String name) throws IocWithoutInstanceException {
        if (iocContainer.containsKey(name)) {
            return iocContainer.get(name);
        } else {
            throw new IocWithoutInstanceException(name + " 实例在容器中不存在");
        }
    }

    /**
     * 覆盖容器中的实例，慎用
     *
     * @param name
     * @param instance
     */
    public static void replaceIocInstance(String name, Object instance) {
        iocContainer.put(name, instance);
    }

    /**
     * 判断容器中是否存在该实例
     *
     * @param name 实例全名或者通过注解命名的name
     * @return
     */
    public static boolean checkIocInstance(String name) {
        return iocContainer.containsKey(name);
    }

    /**
     * 获取容器中所有的实例名集合
     * @return
     */
    public static Set<String> getIocInstanceNames(){
        return iocContainer.keySet();
    }

    /**
     * 判断class是否可以实例化，如final类 接口 抽象类 则不可以实例化
     * @param cls class
     * @return
     */
    public static Object newInstance(Class cls) {
        if(!Modifier.isInterface(cls.getModifiers())
                && !Modifier.isAbstract(cls.getModifiers())
                && !Modifier.isFinal(cls.getModifiers())){
            try {

                return cls.newInstance();
            }catch (InstantiationException e){
                throw new RuntimeException(cls.getName()+ " 没有无参构造器，无法实例化托管，请通过@Bean注解实例化");
            }catch (IllegalAccessException e){
                throw new RuntimeException(cls.getName()+ " 没有公开的构造器，无法实例化托管，请通过@Bean注解实例化");
            }
        }else {
            throw new RuntimeException(cls.getName()+" 不可以被实例化");
        }
    }

    /**
     * 打印当前容器里所有单例
     */
    public static void print(){
        for(String key : iocContainer.keySet()){
            myLogger.info(key);
            myLogger.info(iocContainer.get(key)+"");
        }
    }
}
