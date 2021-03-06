package org.javaboy.vhr.aop;

import com.alibaba.fastjson.JSONObject;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.javaboy.vhr.Service.LogService;
import org.javaboy.vhr.model.TSysLogs;
import org.javaboy.vhr.utils.HttpRequestUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.UUID;

/**
 * 系统日志：切面处理类
 */
@Aspect
@Component
public class SysLogAspect {


    @Autowired
    private LogService logService;


    private long currentTime = 0L;

    //定义切点 @Pointcut
    //在注解的位置切入代码
    @Pointcut("@annotation(org.javaboy.vhr.aop.SysLog)")
    public void logPointCut() {
        // 该方法无方法体,主要为了让同类中其他方法使用此切入点
    }


    /**
     * 配置环绕通知,使用在方法logPointcut()上注册的切入点
     *
     * @param joinPoint
     * @return
     */
    @Around("logPointCut()")
    public Object logAround(ProceedingJoinPoint joinPoint) {
        Object result = null;
        currentTime = System.currentTimeMillis();
        try {
            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
        return result;
    }


    //切面 配置通知
    @AfterReturning("logPointCut()")
    public void saveSysLog(JoinPoint joinPoint) {

        //保存日志
        TSysLogs tSysLogs = new TSysLogs();

        //从切面织入点处通过反射机制获取织入点处的方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //获取切入点所在的方法
        Method method = signature.getMethod();
        SysLog sysLog = method.getAnnotation(SysLog.class);
        if (sysLog != null) {
            String value = sysLog.value();
            tSysLogs.setOperation(value);//保存获取的操作
            String type = sysLog.type();
            tSysLogs.setClienttype(type);
        }

        currentTime = System.currentTimeMillis();

        //获取请求的类名
        String className = joinPoint.getTarget().getClass().getName();
        //获取请求的方法名
        String methodName = method.getName();
        tSysLogs.setMethod(className + "." + methodName);


        tSysLogs.setId(UUID.randomUUID().getMostSignificantBits());

        //请求的参数
        Object[] args = joinPoint.getArgs();
        Object[] arguments = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof ServletRequest || args[i] instanceof ServletResponse || args[i] instanceof MultipartFile) {
                //ServletRequest不能序列化，从入参里排除，否则报异常：java.lang.IllegalStateException: It is illegal to call this method if the current request is not in asynchronous mode (i.e. isAsyncStarted() returns false)
                //ServletResponse不能序列化 从入参里排除，否则报异常：java.lang.IllegalStateException: getOutputStream() has already been called for this response
                continue;
            }
            arguments[i] = args[i];
        }
        //将参数所在的数组转换成json
        String params = "";
        if (arguments != null) {
            try {
                params = JSONObject.toJSONString(arguments);
            } catch (Exception e) {
                params = arguments.toString();
            }
        }
        tSysLogs.setParams(params);

        //获取上下文
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();

        //请求url
        tSysLogs.setUrl(request.getRequestURI());

        //获取用户ip地址
        tSysLogs.setIp(HttpRequestUtil.getIpAddress(request));

        //用户id
        HttpSession session = request.getSession();
        tSysLogs.setUserid(String.valueOf(session.getAttribute("userId")));
//        tSysLogs.setUsername();

        tSysLogs.setCreatedate(new Date());

        tSysLogs.setTime(System.currentTimeMillis() - currentTime);
        tSysLogs.setLogType("INFO");
        logService.inserts(tSysLogs);
    }


    /**
     * 配置异常通知
     *
     * @param joinPoint join point for advice
     * @param e         exception
     */
    @AfterThrowing(pointcut = "logPointCut()", throwing = "e")
    public void logAfterThrowing(JoinPoint joinPoint, Throwable e) {
        TSysLogs log = new TSysLogs();
        log.setCreatedate(new Date());
        log.setLogType("Error");
    }
}
