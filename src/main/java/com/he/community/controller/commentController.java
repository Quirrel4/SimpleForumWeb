package com.he.community.controller;


import com.he.community.entity.Comment;
import com.he.community.entity.DiscussPost;
import com.he.community.entity.Event;
import com.he.community.event.EventProducer;
import com.he.community.service.CommentService;
import com.he.community.service.DiscussPostService;
import com.he.community.utils.CommunityConstant;
import com.he.community.utils.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class commentController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private DiscussPostService discussPostService;

    //添加评论，最后重定向到该帖子页面，所以需要该帖子的id来重定向
    @RequestMapping("/add/{discussPostId}")
    public String addComment(@PathVariable("discussPostId")int discussPostId, Comment comment){
        comment.setUserId(hostHolder.getUsers().getId());
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);

        //触发评论事件
        Event event = new Event()
                .setTopic(CommunityConstant.TOPIC_COMMENT)
                .setUserId(hostHolder.getUsers().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId",discussPostId);

        //设置被提醒人
        //如果评论的是一个帖子，把帖子作者存入，需要把post对象查出来
        if (comment.getEntityType()==CommunityConstant.ENTITY_TYPE_POST){
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());

            //如果评论的是一个评论，把评论的作者存入，也需要把被评论的comment对象查出来
        }else if (comment.getEntityType()==CommunityConstant.ENTITY_TYPE_COMMENT){
            Comment target = commentService.findCommentById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        }
        //这里并发执行，提高了效率
        //把event丢到队列里，等待执行，接下来继续处理业务
        eventProducer.fireEvent(event);

        return "redirect:/discuss/detail/"+discussPostId;
    }


}
