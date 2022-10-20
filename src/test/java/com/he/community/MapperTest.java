package com.he.community;


import com.he.community.dao.DiscussPostMapper;
import com.he.community.dao.LoginTicketMapper;
import com.he.community.dao.MessageMapper;
import com.he.community.dao.UserMapper;
import com.he.community.entity.DiscussPost;
import com.he.community.entity.LoginTicket;
import com.he.community.entity.Message;
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
    private MessageMapper messageMapper;


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
        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(149, 0, 10,0);
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
    @Test
    public void testSelectLetters(){
        List<Message> messages = messageMapper.selectConversations(111, 0, 10);
        for (Message message : messages) {
            System.out.println(message.toString());
        }
        int count = messageMapper.selectConversationCount(111);
        System.out.println(count);

        List<Message> messages1 = messageMapper.selectLetters("111_112", 0, 10);
        for (Message message : messages1) {
            System.out.println(message.toString());
        }

        int count1 = messageMapper.selectLetterCount("111_112");
        System.out.println(count1);

        int count2 = messageMapper.selectLetterUnreadCount(111, "111_131");
        System.out.println(count2 );
    }

}
