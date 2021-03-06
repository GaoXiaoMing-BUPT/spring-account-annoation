package com.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 *
 * @author : gxm
 * @date : 2020/5/5
 * @time : 21:25
 * @projectName : spring-account
 * @description : 工具类，控制连接的事务，注解中配置为切面类
 *  四个通知的注解使用会有顺序调用问题，开发时应选择环绕通知，手动控制通知
 * To change this template use File | Settings | File Templates.
 * Description:
 **/
@Component("transactionManager")
@Aspect
public class TransactionManager {
    @Resource(name = "connectionUtils")
    private ConnectionUtils connectionUtils;

    public void setConnectionUtils(ConnectionUtils connectionUtils) {
        this.connectionUtils = connectionUtils;
    }

    /**
     * @author gxm
     * @date 2020/5/8 15:38
     * @description 配置切入点表达式
     * @param
     * @return void
     * @since version-1.0
     */
    @Pointcut("execution(* *.*.*.AccountServiceImpl.*(..))")
    private void pointCut(){}
    /**
     * @param
     * @return void
     * @author gxm
     * @date 2020/5/5 21:30
     * @description 关闭当前当前线程连接的自动提交
     * @since version-1.0
     */
    @Before("pointCut()")
    public void beginTransaction() {
        System.out.println("beginTransaction");
        try {
            connectionUtils.getThreadConnection().setAutoCommit(false);
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
    }

    /**
     * @param
     * @return void
     * @author gxm
     * @date 2020/5/5 21:30
     * @description 出错就回滚
     * @since version-1.0
     */
    @AfterThrowing("pointCut()")
    public void rollBack() {

        try {
            connectionUtils.getThreadConnection().rollback();
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
        System.out.println("rollBack");
    }

    /**
     * @param
     * @return void
     * @author gxm
     * @date 2020/5/5 21:32
     * @description 提交操作
     * @since version-1.0
     */
    @AfterReturning("pointCut()")
    public void commit() {
        System.out.println("commit");
        try {
            connectionUtils.getThreadConnection().commit();
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
    }

    /**
     * @param
     * @return void
     * @author gxm
     * @date 2020/5/5 21:33
     * @description This is description of method
     * @since version-1.0
     */
    @After("pointCut()")
    public void release() {
        System.out.println("release");
        try {
            connectionUtils.getThreadConnection().close();
            connectionUtils.remove();
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
    }
    /**
     * @author gxm
     * @date 22:44
     * @description 环绕通知，类似于动态代理，手动实现aop过程
     * @param joinPoint 1
     * @return java.lang.Object
     * @since version-1.0
     */
    //@Around("pointCut()")
    public Object aopAround(ProceedingJoinPoint joinPoint) {
        Object proceed = null;
        try {
            // 1. 前置通知
            beginTransaction();
            // 2. 环绕通知
            proceed = joinPoint.proceed(joinPoint.getArgs());
            // 3. 后置通知
            commit();
        } catch (Throwable throwable) {
            // 4. 异常通知
            rollBack();
            throw new RuntimeException(throwable);
        }finally {
            // 5. 最终通知
            release();
        }
        return proceed;
    }
}
