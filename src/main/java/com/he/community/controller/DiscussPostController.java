package com.he.community.controller;


import com.he.community.entity.Comment;
import com.he.community.entity.DiscussPost;
import com.he.community.entity.Page;
import com.he.community.entity.User;
import com.he.community.service.CommentService;
import com.he.community.service.DiscussPostService;
import com.he.community.service.UserService;
import com.he.community.utils.CommunityConstant;
import com.he.community.utils.CommunityUtil;
import com.he.community.utils.HostHolder;
import com.sun.corba.se.spi.ior.ObjectKey;
import org.springframework.beans.factory.annotation.Autowired;
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


}
