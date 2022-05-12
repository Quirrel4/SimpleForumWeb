package com.he.myspringboot;


import com.he.community.MySpringbootApplication;
import com.he.community.utils.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = MySpringbootApplication.class)
public class MailTest {

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine engine;

    @Test
    public void testTextMail(){
        mailClient.sendMail("1186105627he@sina.com","Hello!","I am processing!");
    }

    @Test
    public void testHtmlMail(){
        Context context=new Context();
        context.setVariable("username","Tuesday");

        String content = engine.process("/mail/demo", context);
        System.out.println(content);

        mailClient.sendMail("1186105627he@sina.com","Hi!",content);
    }
}
