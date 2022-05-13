package com.he.myspringboot;

import com.he.community.MySpringbootApplication;
import com.he.community.controller.interceptor.AlphaInterceptor;
import com.he.community.service.AlphaService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes= MySpringbootApplication.class)
public class TransactionTest {

    @Autowired
    private AlphaService alphaService;
    @Test
    public void testSave1(){
        Object o = alphaService.save1();
        System.out.printf(o.toString());
    }
}
