package com.he.community;


import com.he.community.entity.DiscussPost;
import com.he.community.service.DiscussPostService;
import org.junit.*;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes=MySpringbootApplication.class)
public class SpringBootTests {

    @Autowired
    private DiscussPostService discussPostService;

    private DiscussPost data;

    @BeforeClass
    public static void beforeClass(){
        System.out.println("beforeClass");
    }

    @AfterClass
    public static void afterClass(){
        System.out.println("afterClass");
    }


    //每次调用方法都会执行
    @Before
    public void before(){
        System.out.println("before");
        data = new DiscussPost();
        data.setUserId(111);
        data.setTitle("test Title");
        data.setContent("test Content");
        data.setCreateTime(new Date());
        discussPostService.addDiscussPost(data);

    }

    @After
    public void after(){
        System.out.println("after");
        discussPostService.updateStatus(data.getId(),2);
    }

    @Test
    public void test1(){
        System.out.println("test1");
    }

    @Test
    public void test2(){
        System.out.println("test2");
    }

    @Test
    public void testFindById(){
        DiscussPost res = discussPostService.findDiscussPostById(data.getId());
        Assert.assertNotNull(res);
        Assert.assertEquals(data.getTitle(),res.getTitle());
        Assert.assertEquals(data.getContent(),res.getContent());
    }

    @Test
    public void testUpdateScore(){
        int rows = discussPostService.updateScore(data.getId(), 2000);
        Assert.assertEquals(1,rows);
        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
        Assert.assertEquals(2000.00,post.getScore(),2);
    }


}
