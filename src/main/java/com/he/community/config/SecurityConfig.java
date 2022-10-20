package com.he.community.config;

import com.he.community.utils.CommunityConstant;
import com.he.community.utils.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


//忘写这个了，呕
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    //忽略对静态资源访问的过滤
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {

        //授权
        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unfollow"
                )
                .hasAnyAuthority(
                        CommunityConstant.AUTHORITY_ADMIN,
                CommunityConstant.AUTHORITY_MODERATOR,
                CommunityConstant.AUTHORITY_USER
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                )
                .hasAnyAuthority(
                        CommunityConstant.AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/delete",
                        "/data/**",
                        "/actuator/**"
                )
                .hasAnyAuthority(
                        CommunityConstant.AUTHORITY_ADMIN
                )
                .anyRequest().permitAll()
                        .and().csrf().disable();

        //权限不够
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() {
                    //未登录
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                String xRequestWith = request.getHeader("x-requested-with");
                if ("XMLHttpRequest".equals(xRequestWith)){
                    response.setContentType("application/plain;charset=utf-8");
                    PrintWriter writer = response.getWriter();
                    writer.write(CommunityUtil.getJSONString(403,"你还没有登录!"));
                }else{
                    //跳转到登录页面和登录请求都是/login路径，前者是get后者是表单post
                    response.sendRedirect(request.getContextPath()+"/login");
                }
            }
        })
                .accessDeniedHandler(new AccessDeniedHandler() {
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                        String xRequestWith = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(xRequestWith)){
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(CommunityUtil.getJSONString(403,"你没有访问此功能的权限!"));
                        }else{
                            //跳转到登录页面和登录请求都是/login路径，前者是get后者是表单post
                            response.sendRedirect(request.getContextPath()+"/denied");
                        }
                    }
                });

        // Security底层会默认拦截 /logout 请求，进行退出处理，而不会执行我们自己写的逻辑
        // 需要覆盖他默认的逻辑
        http.logout().logoutUrl("/securityLogout");
    }
}
