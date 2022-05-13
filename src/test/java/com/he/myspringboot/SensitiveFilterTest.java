package com.he.myspringboot;


import com.he.community.MySpringbootApplication;
import com.he.community.utils.SensitiveFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes= MySpringbootApplication.class)
public class SensitiveFilterTest {

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Test
    public void Test01(){
        String text="这里可以开票，可以吸毒，可以嫖娼，可以赌博，哈哈哈！";
        text = sensitiveFilter.filter(text);
        System.out.println(text);
    }
}
