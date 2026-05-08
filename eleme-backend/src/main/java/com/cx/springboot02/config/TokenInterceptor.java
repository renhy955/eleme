package com.cx.springboot02.config;

import com.alibaba.fastjson.JSONObject;
import com.cx.springboot02.common.E.AuthorizeType;
import com.cx.springboot02.common.E.ResultCode;
import com.cx.springboot02.common.ResultTool;
import com.cx.springboot02.common.redis.RedisOperator;
import com.cx.springboot02.common.utils.*;

import com.cx.springboot02.service.impl.BusinessServiceImpl;
import com.cx.springboot02.service.impl.CustomerServiceImpl;
import com.cx.springboot02.service.impl.ManagerServiceImpl;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static com.cx.springboot02.common.utils.Final.*;

/**
 * 验证token，是否登录
 */
@Component
@Slf4j
public class TokenInterceptor implements HandlerInterceptor {

    @Autowired
    RedisOperator redisOperator;

    @Autowired
    CustomerServiceImpl iCustomerService;

    @Autowired
    BusinessServiceImpl iBusinessService;

    @Autowired
    ManagerServiceImpl iManagerService;


    /**
     * 忽略拦截的url
     */
    private final List<String> urls = Arrays.asList(
            "/customer/login",
            "/upload",
            "/error",
            "/verifyCode",
            "/image"
    );
    @Autowired
    CustomerServiceImpl customerService;
    /**
     * 进入controller层之前拦截请求
     * @param httpServletRequest
     * @param httpServletResponse
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object handler) throws Exception {
        String uri = httpServletRequest.getRequestURI();
        System.out.println("本次准备拦截的路径:"+uri);
        httpServletResponse.setContentType("application/json; charset=utf-8");
        //先检查无论如何都可以放行的资源
        if(checkCanPassByStatic(httpServletRequest,handler)){
            return true;
        }
        //再检查方法上是否含有开放接口的注解 tips:这2个判断需要分开,因为handler需要映射到方法才可强转
        //java.lang.ClassCastException: org.springframework.web.servlet.resource.ResourceHttpRequestHandler cannot be cast to org.springframework.web.method.HandlerMethod
        if(checkPassByAnnotation(httpServletRequest,handler)){
            return true;
        }
        //检查token是否有效
        TokenResult tokenResult = checkByToken(httpServletRequest, handler);
        if(tokenResult==null){
            writeReturn(httpServletResponse, ResultCode.USER_ACCOUNT_TOKEN_ERROR);
            return false;
        }

        //根据token判断是否有权限
        if(powerVerification(tokenResult.getIdentity(),handler)){
            return true;
        }
        writeReturn(httpServletResponse, ResultCode.COMMON_FAIL);
        return false;
    }

    /**
     * 默认放行的资源
     * @param httpServletRequest
     * @param handler
     * @return
     */
    public Boolean checkCanPassByStatic(HttpServletRequest httpServletRequest,Object handler){
        if(HttpMethod.OPTIONS.toString().equals(httpServletRequest.getMethod())) {
            //options请求.放行
            return true;
        }
        if(!(handler instanceof HandlerMethod)){
            //如果不是映射到方法直接通过
            return true;
        }
        //不拦截路径（登录路径等等）
        String uri = httpServletRequest.getRequestURI();
        for (String url : urls) {
            if(uri.contains(url)) {
                System.out.println("不拦截的路径:"+url);
                return true;
            }
        }
        return false;
    }

    @Data
    static class TokenResult{
        private AuthorizeType identity;
        private String name;
        private String password;
    }

    /**
     * 根据token解析token是否有效
     * @param httpServletRequest
     * @param handler
     * @return
     */
    public TokenResult checkByToken(HttpServletRequest httpServletRequest,Object handler){
        //2.拿到请求头里面的token（如果是第一次登录，那么是没有请求头的）
        String token = httpServletRequest.getHeader("token");
        TokenResult tokenResult = new TokenResult();
        if(token == null){
            return null;
        }else{
            //根据前端的token取出对应的身份(枚举类),根据前端的token取出对应的name,password
            AuthorizeType identity = AuthorizeType.StringToAuthorizeType(JwtUtils.getValue(token, "identity"));
            tokenResult.setIdentity(identity);
            String name = JwtUtils.getValue(token, "name");
            tokenResult.setName(name);
            String password = JwtUtils.getValue(token, "password");
            tokenResult.setPassword(password);
            if(password==null || name==null || identity==null) {
                return null;
            }
            //判断name和身份去对应的表当中身份,看是否存在此用户,存在就继续验证,不存在就直接返回
            if(identity.equals(AuthorizeType.CUSTOMER)){
                if(!iCustomerService.login(token,name,password)){
                    return null;
                }
            }else if(identity.equals(AuthorizeType.MANAGE)){
                if(!iManagerService.login(token,name, password)){
                    return null;
                }
            }else if(identity.equals(AuthorizeType.BUSINESS)){
                if(!iBusinessService.login(token,name, password)){
                    return null;
                }
            }else{
                //没有此身份
                return null;
            }
        }
        return tokenResult;
    }

    /**
     * token验权
     * @param identity
     * @param handler
     * @return
     */
    public Boolean powerVerification(AuthorizeType identity, Object handler){
        if(identity==null) return false;
        HandlerMethod handlerMethod = (HandlerMethod) handler;
        //拿到method
        Method method = handlerMethod.getMethod();
        //拿到clz对象
        Class clz = handlerMethod.getBeanType();
        //判断是否有权限发此请求
        //拿到方法上的注解,为空或者没有权限参数的方法不给访问
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        PreAuthorize preAuthorizeCLZ = (PreAuthorize) clz.getAnnotation(PreAuthorize.class);
        //当类上没有注解时 并且 方法上也没有注解的时候
        if((preAuthorize==null || preAuthorize.values().length==0)&& (preAuthorizeCLZ==null || preAuthorizeCLZ.values().length==0)){
            return false;
        }
        //拿到类上的权限列表,判断类上的注解是否符合
        if(preAuthorizeCLZ != null){
            AuthorizeType[] values = preAuthorizeCLZ.values();
            for (AuthorizeType authorizeType : values) {
                if(identity.equals(authorizeType)){
                    return true;
                }
            }
        }
        //拿到方法上面的权限列表
        if(preAuthorize != null){
            AuthorizeType[] values = preAuthorize.values();
            for (AuthorizeType authorizeType : values) {
                if(identity.equals(authorizeType)){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 根据Unobstructed注解判断是否可以放行
     * @param httpServletRequest
     * @param handler
     * @return
     */
    public Boolean checkPassByAnnotation(HttpServletRequest httpServletRequest,Object handler){
        //拿到method
        Method method = ((HandlerMethod) handler).getMethod();
        //拿到clz对象
        Class clz = ((HandlerMethod) handler).getBeanType();
        //判断是否有放行注解
        Unobstructed unobstructed1 = method.getAnnotation(Unobstructed.class);
        Unobstructed unobstructed2 = (Unobstructed) clz.getAnnotation(Unobstructed.class);
        if(unobstructed2!=null || unobstructed1!=null) return true;//有就直接放行
        return false;
    }


    /**
     * 写回数据
     * @param httpServletResponse
     * @param resultCode
     */
    public void writeReturn(HttpServletResponse httpServletResponse, ResultCode resultCode){
        try {
            httpServletResponse.getWriter().print(JSONObject.toJSON(ResultTool.fail(resultCode)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void postHandle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, ModelAndView modelAndView) throws Exception {
        // System.out.println("处理请求完成后视图渲染之前的处理操作");
    }

    @Override
    public void afterCompletion(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Object o, Exception e) throws Exception {
        // System.out.println("视图渲染之后的操作");
    }
}

