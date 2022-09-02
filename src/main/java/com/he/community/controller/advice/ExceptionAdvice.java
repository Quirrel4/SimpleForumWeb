package com.he.community.controller.advice;


import com.he.community.utils.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;


//异常Advice
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {

    private static final Logger logger= LoggerFactory.getLogger(ExceptionAdvice.class);


    //handleException()是处理异常的方法
    @ExceptionHandler({Exception.class})        //规定异常的范围
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        //日志记录部分
        logger.error("服务器发生异常: "+e.getMessage());
        for (StackTraceElement element:e.getStackTrace()){
            logger.error(element.toString());
        }

        //根据消息头来判断是否是异步请求，因为只有异步请求请求返回XML(当然也可以返回JSON来作为代替)
        //异步请求不用转到错误页面
        String xRequestedWith = request.getHeader("x-requested-with");
        if (xRequestedWith.equals("XMLHttpRequest")){
            //设置返回数据类型
            response.setContentType("application/json;charset=utf-8");
            PrintWriter writer=response.getWriter();
            //传回一个json字符串，存入错误的信息
            writer.write(CommunityUtil.getJSONString(1,"服务器异常"));
        }else{
            //同步请求异常，直接转到错误页面
            response.sendRedirect(request.getContextPath()+"/error");
        }

    }
}
