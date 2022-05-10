package com.kcang.aop.realize.async;


import com.kcang.aop.annotaion.AopAnnotationImpl;
import com.kcang.aop.impl.AopMethod;
import com.kcang.aop.impl.AopMethodProxy;
import com.kcang.exception.IocWithoutInstanceException;
import com.kcang.ioc.impl.IocAdmin;

import java.util.concurrent.ThreadPoolExecutor;

@AopAnnotationImpl(AopAnnotation = AsyncRunnable.class, order = 0)
public class AsyncRunnableImpl extends AopMethodProxy {
    private AsyncRunnable asyncRunnable;


    @Override
    public void setAnnotation(Object annotation) {
        this.asyncRunnable = (AsyncRunnable) annotation;
    }

    @Override
    public void before(Object[] args) {
    }

    @Override
    public Object around(AopMethod aopMethod) throws IocWithoutInstanceException {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    aopMethod.methodRun();
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        };
        if(this.asyncRunnable.type() == AsyncType.Synchronous){
            GlobalThreadPoolExecutor.SynchronousPool.execute(runnable);
        }
        if(this.asyncRunnable.type() == AsyncType.ArrayBlocking){
            GlobalThreadPoolExecutor.ArrayBlockingPool.execute(runnable);
        }
        if(this.asyncRunnable.type() == AsyncType.CustomThreadPool){
            String poolName = this.asyncRunnable.name();
            ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) IocAdmin.getIocInstance(poolName);
            threadPoolExecutor.execute(runnable);
        }
        return null;
    }

    @Override
    public void after(Object result) {
    }

}
