package com.he.community.service;

import com.he.community.dao.DiscussPostMapper;
import com.he.community.dao.UserMapper;
import com.he.community.entity.DiscussPost;
import com.he.community.entity.User;
import com.he.community.utils.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
@Component
public class AlphaService {

    private static final Logger logger= LoggerFactory.getLogger(AlphaService.class);

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public Object save1(){
        //新增用户
        User user = new User();
        user.setUsername("alpha");
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5("123")+user.getSalt());
        user.setEmail("alpha@qq.com");
        user.setHeaderUrl("http://image.nowcoder.com/head/99t.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //新增帖子
        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle("hello");
        discussPost.setContent("新人报到!");
        discussPost.setCreateTime(new Date());
        discussPostMapper.insertDiscussPost(discussPost);

        Integer.valueOf("abc");

        return "ok";
    }

    //可以让该方法在多线程的环境下，被异步调用
    //@Async
    public void execute1(){
        logger.debug("execute1");
    }


    //@Scheduled(initialDelay = 10000,fixedRate = 1000)
    public void execute2(){
        logger.debug("execute2");
    }

}
