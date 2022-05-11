package com.kcang.ioc.impl;

import com.kcang.exception.IocInstanceRepeatException;
import com.kcang.exception.IocWithoutInstanceException;
import com.kcang.ioc.annotation.InjectInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;

public class InjectInstanceImpl {
    private Logger myLogger = LoggerFactory.getLogger(InjectInstanceImpl.class);

    public void init(Set<String> instanceNameSet) throws IocWithoutInstanceException, IllegalAccessException {
        for(String instanceName : instanceNameSet){
            Object instance = IocAdmin.getIocInstance(instanceName);
            injectBarn(instanceName, instance);
        }
    }
    /**
     * 注入不带@Bean注解的类的属性
     * @param instanceName 实例名
     * @param instance 实例
     */
    private void injectBarn(String instanceName, Object instance) throws IocWithoutInstanceException, IllegalAccessException {
        //Field[] fields = instance.getClass().getDeclaredFields();
        List<Field> fields = IocAdmin.getAllFields(instance.getClass());
        for(Field field : fields){
            InjectInstance injectInstance = field.getDeclaredAnnotation(InjectInstance.class);
            if(injectInstance != null){
                String barnNameInject = injectInstance.name().equals("")? field.getType().getSimpleName(): injectInstance.name();
                field.setAccessible(true);
                try {
                    field.set(instance, IocAdmin.getIocInstance(barnNameInject));
                }catch (IllegalAccessException e){
                    throw new IllegalAccessException(instanceName+" 的属性 "+field.getName()+" 注入实例失败");
                }
                myLogger.debug("注入属性实例 "+barnNameInject+" -> "+instanceName+"."+field.getName()+" 成功");
            }
        }
    }

    /**
     * 注解@Bean的属性注入方法，
     * @param instanceName 实例名
     * @param cls 实例类
     * @throws IocWithoutInstanceException
     * @throws IllegalAccessException
     */
    static void injectConfigurationBarn(String instanceName, Class cls) throws IocWithoutInstanceException, IllegalAccessException, InvocationTargetException, IocInstanceRepeatException {
        //Field[] fields = cls.getDeclaredFields();
        List<Field> fields = IocAdmin.getAllFields(cls);
        for(Field field : fields){
            InjectInstance injectInstance = field.getDeclaredAnnotation(InjectInstance.class);
            if(injectInstance != null){
                String beanNameInject = injectInstance.name().equals("")? field.getType().getSimpleName(): injectInstance.name();
                field.setAccessible(true);
                Object getInjectBean = null;
                try {
                    getInjectBean = IocAdmin.getIocInstance(beanNameInject);
                }catch (IocWithoutInstanceException e){
                    //没有要注入的实例, 就去bean里面找 找到就实例化注入 找不到就报错
                    getInjectBean = IocImplement.createBeanInstance(beanNameInject);
                }
                if(getInjectBean == null){
                    throw new IocWithoutInstanceException(beanNameInject + " 不存在！注入属性依赖失败");
                }
                field.set(IocAdmin.getIocInstance(instanceName), getInjectBean);
            }
        }
    }
}
