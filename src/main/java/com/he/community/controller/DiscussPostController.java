package com.he.community.controller;


import com.he.community.entity.*;
import com.he.community.event.EventProducer;
import com.he.community.service.CommentService;
import com.he.community.service.DiscussPostService;
import com.he.community.service.LikeService;
import com.he.community.service.UserService;
import com.he.community.utils.CommunityConstant;
import com.he.community.utils.CommunityUtil;
import com.he.community.utils.HostHolder;
import com.he.community.utils.RedisKeyUtil;
import com.sun.corba.se.spi.ior.ObjectKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController {

    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add",method = RequestMethod.POST)
    @ResponseBody
    public String addDiscussPost(String title,String content){
        User user=hostHolder.getUsers();
        if (user==null){
            return CommunityUtil.getJSONString(403,"你还没有登录哦！");
        }
        DiscussPost post=new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle(title);
        post.setContent(content);
        post.setCreateTime(new Date());
        discussPostService.addDiscussPost(post);


        //触发发帖事件
        Event event = new Event()
                .setTopic(CommunityConstant.TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityId(CommunityConstant.ENTITY_TYPE_POST)
                .setEntityId(post.getId());
        eventProducer.fireEvent(event);

        //计算帖子的分数
        String redisKey= RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,post.getId());


        return CommunityUtil.getJSONString(0,"发布成功！");
    }



    //帖子详情页面
    @RequestMapping(path = "/detail/{discussPostId}",method = RequestMethod.GET)
    public String getDiscussPost(@PathVariable("discussPostId")int discussPostId, Model model, Page page){
        //查询帖子
        //只有帖子还不够，还需要查找到发布的user信息
        DiscussPost post= discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post",post);
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user",user);

        long postLikeCount=likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_POST,discussPostId);
        model.addAttribute("postLikeCount",postLikeCount);

        int postLikeStatus=hostHolder.getUsers()==null?0:likeService.findEntityLikeStatus(hostHolder.getUsers().getId(),CommunityConstant.ENTITY_TYPE_POST,discussPostId);
        model.addAttribute("postLikeStatus",postLikeStatus);



        //评论的分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/"+discussPostId);
        page.setRows(post.getCommentCount());


        List<Comment> commentList=commentService.findCommentsByEntity(
                CommunityConstant.ENTITY_TYPE_POST,post.getId(),page.getOffset(),page.getLimit()
        );
        //查出来的评论列表还需要加上作者的信息，所以需要放入另一个list里
        List<Map<String,Object>> commentVOList=new ArrayList<>();

        if (commentList!=null){
            for (Comment comment:commentList){

                /**
                 * VO表示是模型对象，要注入到template里的数据
                 */
                Map<String, Object> commentVO=new HashMap<>();
                //评论列表
                commentVO.put("comment",comment);
                //作者
                commentVO.put("user", userService.findUserById(comment.getUserId()));

                //点赞数量
                long commentLikeCount=likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_COMMENT,comment.getId());
                commentVO.put("commentLikeCount",commentLikeCount);
                //点赞状态
                int commentLikeStatus=hostHolder.getUsers()==null?0:likeService.findEntityLikeStatus(hostHolder.getUsers().getId(),CommunityConstant.ENTITY_TYPE_COMMENT,comment.getId());
                commentVO.put("commentLikeStatus",commentLikeStatus);


                //每个评论都有个回复列表
                List<Comment> replyList = commentService.findCommentsByEntity(
                        CommunityConstant.ENTITY_TYPE_COMMENT, comment.getId(), 0, 5);
                //每条回复也需要加上用户的消息
                List<Map<String,Object>> replyVOList=new ArrayList<>();
                if (replyList!=null){
                    for(Comment reply:replyList){
                        Map<String, Object> replyVO = new HashMap<>();
                        //回夫
                        replyVO.put("reply",reply);
                        //回复用户
                        replyVO.put("user",userService.findUserById(reply.getUserId()));

                        //点赞数量
                        long replyLikeCount=likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_COMMENT,comment.getId());
                        replyVO.put("replyLikeCount",replyLikeCount);
                        //点赞状态
                        int replyLikeStatus=hostHolder.getUsers()==null?0:likeService.findEntityLikeStatus(hostHolder.getUsers().getId(),CommunityConstant.ENTITY_TYPE_COMMENT,comment.getId());
                        replyVO.put("replyLikeStatus",replyLikeStatus);


                        //回复的目标用户
                        User target=reply.getTargetId()==0?null:userService.findUserById(reply.getUserId());
                        replyVO.put("target",target);
                        replyVOList.add(replyVO);
                    }
                }
                commentVO.put("replys",replyVOList);
                //查询回复数量
                int replyCount = commentService.findCommentCount(CommunityConstant.ENTITY_TYPE_COMMENT, comment.getId());
                commentVO.put("replyCount",replyCount);
                commentVOList.add(commentVO);
            }
        }

        model.addAttribute("comments",commentVOList);
        return "/site/discuss-detail";
    }


    //置顶
    @RequestMapping(path = "/top",method = RequestMethod.POST)
    @ResponseBody
    public String setTop(int id){
        discussPostService.updateType(id,1);

        //触发发帖事件
        Event event = new Event()
                .setTopic(CommunityConstant.TOPIC_PUBLISH)
                .setUserId(hostHolder.getUsers().getId())
                .setEntityId(CommunityConstant.ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return  CommunityUtil.getJSONString(0);
    }


    //加精
    @RequestMapping(path = "/wonderful",method = RequestMethod.POST)
    @ResponseBody
    public String setWonderful(int id){
        discussPostService.updateStatus(id,1);

        //触发发帖事件
        Event event = new Event()
                .setTopic(CommunityConstant.TOPIC_PUBLISH)
                .setUserId(hostHolder.getUsers().getId())
                .setEntityId(CommunityConstant.ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        //计算帖子的分数
        String redisKey= RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey,id);


        return  CommunityUtil.getJSONString(0);
    }

    //删除
    @RequestMapping(path = "/delete",method = RequestMethod.POST)
    @ResponseBody
    public String setDelete(int id){
        discussPostService.updateStatus(id,2);

        //触发删帖事件
        Event event = new Event()
                .setTopic(CommunityConstant.TOPIC_DELETE)
                .setUserId(hostHolder.getUsers().getId())
                .setEntityId(CommunityConstant.ENTITY_TYPE_POST)
                .setEntityId(id);
        eventProducer.fireEvent(event);

        return  CommunityUtil.getJSONString(0);
    }


}
