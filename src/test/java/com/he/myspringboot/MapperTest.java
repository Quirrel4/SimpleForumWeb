package com.he.myspringboot;


import com.he.community.MySpringbootApplication;
import com.he.community.dao.DiscussPostMapper;
import com.he.community.dao.LoginTicketMapper;
import com.he.community.dao.UserMapper;
import com.he.community.entity.DiscussPost;
import com.he.community.entity.LoginTicket;
import com.he.community.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes= MySpringbootApplication.class)
public class MapperTest {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;
    @Test
    public void testSelectUser(){
        User user = userMapper.selectById(1);
        System.out.println(user);
    }

    @Test
    public  void testSelectDiscussPost(){
        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(149, 0, 10);
        for (DiscussPost discussPost : discussPosts) {
            System.out.println(discussPost.toString());
        }

        int rows= discussPostMapper.selectDiscussPostsRows(149);
        System.out.println(rows);

    }

    @Test
    public  void testInsertLoginTicket(){
        LoginTicket loginTicket = new LoginTicket(101,"abc",0,new Date(System.currentTimeMillis()+1000*60*10));
        int i = loginTicketMapper.insertLoginTicket(loginTicket);
    }

    @Test
    public void testSelectLoginTicket(){
        LoginTicket abc = loginTicketMapper.selectByTicket("abc");
        System.out.println(abc);

        loginTicketMapper.updateStatus("abc",1);
        abc=loginTicketMapper.selectByTicket("abc");
        System.out.println(abc);
    }

}
