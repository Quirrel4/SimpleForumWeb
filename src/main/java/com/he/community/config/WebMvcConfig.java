package com.he.community.config;


import com.he.community.annotation.LoginRequired;
import com.he.community.controller.interceptor.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AlphaInterceptor alphaInterceptor;

    @Autowired
    private LoginTicketInterceptor loginTicketInterceptor;

    //@Autowired
    //private LoginRequiredInterceptor loginRequiredInterceptor;

    @Autowired
    private MessageInterceptor messageInterceptor;

    @Autowired
    private DataInterceptor dataInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(alphaInterceptor)
                .excludePathPatterns("/**/*.png","/**/*.css","/**/*.js","/**/*.html","/**/*.jpg","/**/*.jpeg")
                .addPathPatterns("/register","/login");

        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/**/*.png","/**/*.css","/**/*.js","/**/*.html","/**/*.jpg","/**/*.jpeg");
        //registry.addInterceptor(loginRequiredInterceptor)
      //          .excludePathPatterns("/**/*.png","/**/*.css","/**/*.js","/**/*.html","/**/*.jpg","/**/*.jpeg");

        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/**/*.png","/**/*.css","/**/*.js","/**/*.html","/**/*.jpg","/**/*.jpeg");

        registry.addInterceptor(dataInterceptor)
                .excludePathPatterns("/**/*.png","/**/*.css","/**/*.js","/**/*.html","/**/*.jpg","/**/*.jpeg");


    }


}
