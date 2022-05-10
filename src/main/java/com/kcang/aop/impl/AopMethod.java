package com.kcang.aop.impl;

import com.kcang.exception.ForcedChangeException;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 封装的代理方法代理过程，包括入参出参的修改和方法执行过程的修改
 */
public final class AopMethod {
    //代理后生成的对象
    private final Object object;
    //被代理的方法
    private final Method method;
    //入参
    private Object[] args;
    private Object argsVersion = null;
    //出参
    private Object result = null;
    private Object resultVersion = null;
    //代理对象执行方法
    private final MethodProxy methodProxy;
    //记录方法被aop代理执行过几次，执行了几次可以找到下一个around方法
    private int funcCount = 0;
    //代理方法对象责任链观察者，递归调用
    private List<AopMethodProxy> aopMethodProxyList;


    AopMethod(Object object, Method method, Object[] args, MethodProxy methodProxy) {
        this.object = object;
        this.method = method;
        this.args = args;
        this.methodProxy = methodProxy;
        this.aopMethodProxyList = new ArrayList<>();
    }

    void setAopMethodProxy(AopMethodProxy aopMethodProxy){
        this.aopMethodProxyList.add(aopMethodProxy);
    }

    /**
     * 如果实现类都执行了这个方法，类似责任链调度模式，中间有一个注解实现类没有执行，则排在后面的around都不会被执行，则最后这个代理方法也不会被实际运行
     * @return
     * @throws Throwable
     */
    public Object methodRun() throws Throwable {
        int index = this.funcCount;
        //判断是否是最后一个观察者proxy
        if(index >= this.aopMethodProxyList.size()){
            this.setResult(this.methodProxy.invokeSuper(this.object, this.args));
            return this.result;
        }

        //执行before
        this.aopMethodProxyList.get(index).before(this.args);

        //递归调用下一个around方法
        this.funcCount = this.funcCount + 1;
        this.setResult(this.aopMethodProxyList.get(index).around(this));

        //执行after
        this.aopMethodProxyList.get(index).after(this.result);
        return this.result;
    }

    /**
     * 获取方法当前入参
     * @return
     */
    public Object[] getArgs(){
        return this.args;
    }

    /**
     * 修改方法当前入参
     * @param args
     */
    public void setArgs(Object[] args){
        if(this.argsVersion == null){
            this.args = args;
        }
    }

    /**
     * 强制修改方法最终入参，修改后其他注解无法再修改入参
     * @param args 参数
     * @param version 当前修改的注解实现类对象
     * @throws ForcedChangeException 如果别的注解已经强制修改过，则当前无法修改抛出异常
     */
    public void setArgs(Object[] args, Object version) throws ForcedChangeException {
        if(this.argsVersion == null){
            this.args = args;
            this.argsVersion = version;
        }else {
            throw new ForcedChangeException(this.method.getName()+" 代理方法已有强制参数版本 "+this.argsVersion+" 无法再强制改变");
        }
    }

    /**
     * 获取方法当前返回值
     * @return
     */
    public Object getResult(){
        return this.result;
    }

    /**
     * 设置当前返回值
     * @param result
     */
    private void setResult(Object result){
        if(this.resultVersion == null){
            this.result = result;
        }
    }

    /**
     * 强制设置方法最终返回值，设置后其他注解无法再修改
     * @param result 返回值
     * @param version 当前注解实现类对象
     * @throws ForcedChangeException 其他注解也强制设置过，则无法继续强制设置，抛出异常
     */
    public void setResult(Object result, Object version) throws ForcedChangeException {
        if(this.resultVersion == null){
            this.result = result;
            this.resultVersion = version;
        }else {
            throw new ForcedChangeException(this.method+" 代理方法已有强制返回值版本: "+this.resultVersion+" 无法再强制改变");
        }
    }
}
