package com.he.community.controller.interceptor;

import com.he.community.entity.LoginTicket;
import com.he.community.entity.User;
import com.he.community.service.UserService;
import com.he.community.utils.CookieUtil;
import com.he.community.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {
    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    //每次请求都会经过拦截器，查取凭证并在一个线程还未执行的时候放到HostHolder对象里
    //HostHolder对象是容器里的，所有线程共享，但是里面封装的ThreadLocal是线程安全的

    //接口定义的函数，不能用@CookieValues
    //Handler其实就是我们的Controller的方法
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从cookie中获取凭证
        String ticket= CookieUtil.getValue(request,"ticket");
        if(ticket!=null){
            LoginTicket loginTicket = userService.findLoginTicket(ticket);

            //检查凭证是否有效
            if(loginTicket!=null&&loginTicket.getStatus()==0&&loginTicket.getExpired().after(new Date())){
                User user = userService.findUserById(loginTicket.getUserId());
                hostHolder.setUsers(user);

                //构建用户认证的结果，并存入SecurityContext，以便于Security进行授权
                // Authentication 证书的意思，代表登录凭证
                //user是附带的信息，password是登录的证明信息，后面的这个是用户权限，调用userService的方法得到一个集合
                Authentication authentication=new UsernamePasswordAuthenticationToken(
                        user,user.getPassword(),userService.getAuthorities(user.getId()));

                //这里可以存入一个自定义的SecurityContext接口的是是实现类，还是要返回authentication，本质都是对authentication（登录凭证）对象做一个封装来存储
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUsers();
        if(user!=null&&modelAndView!=null){
            modelAndView.addObject("loginUser",user);
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
        //SecurityContextHolder.clearContext();
    }
}
