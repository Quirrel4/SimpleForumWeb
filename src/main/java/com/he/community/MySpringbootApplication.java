package com.he.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.PostConstruct;

@SpringBootApplication
public class MySpringbootApplication {

    @PostConstruct
    public void init(){
        //解决redis和elasticsearch共同依赖netty的启动问题
        //问题来自 Netty4Util.setAvailableProcessors()
        System.setProperty("es.set.netty.runtime.available.processors","false");
    }

    public static void main(String[] args) {
        SpringApplication.run(MySpringbootApplication.class, args);
    }

}
