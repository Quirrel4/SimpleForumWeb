package com.he.community.controller;


import com.alibaba.fastjson.JSONObject;
import com.he.community.entity.Message;
import com.he.community.entity.Page;
import com.he.community.entity.User;
import com.he.community.service.MessageService;
import com.he.community.service.UserService;
import com.he.community.utils.CommunityConstant;
import com.he.community.utils.CommunityUtil;
import com.he.community.utils.HostHolder;
import com.sun.org.apache.xpath.internal.operations.Mod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.sql.Array;
import java.util.*;

@Controller
public class MessageController {

    @Autowired
    private MessageService messageService;
    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private UserService userService;

    //私信列表
    @RequestMapping(path="/letter/list",method = RequestMethod.GET)
    public String getLetterList(Model model, Page page){
        User user = hostHolder.getUsers();
        //设置分页的信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));
        //查询用户所有的私信。所有的私信的conversationId都是不重复的，都是每个conversationId对应的最大的id私信（最新的私信）
        List<Message> conversationList = messageService.findConversations(
                user.getId(), page.getOffset(), page.getLimit());
        List<Map<String,Object>> conversations=new ArrayList<>();
        if (conversationList!=null){
            for (Message message : conversationList) {
                Map<String, Object> map = new HashMap<>();
                map.put("conversation",message);
                map.put("unreadCount",messageService.findLetterUnreadCount(user.getId(), message.getConversationId()));
                map.put("letterCount",messageService.findLetterCount(message.getConversationId()));
                int targetId= user.getId()==message.getFromId()?message.getToId():message.getFromId();
                map.put("target",userService.findUserById(targetId));

                conversations.add(map);
            }
        }
        model.addAttribute("conversations",conversations);

        //查询用户所有未读消息数量
        int letterUnreadCount=messageService.findLetterUnreadCount(user.getId(),null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        //查询所有未读系统通知
        int noticeUnreadCount= messageService.findNoticeUnreadCount(user.getId(),null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);

        return "/site/letter";
    }

    //访问私信细节页面
    @RequestMapping(path="/letter/detail/{conversationId}",method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId")String conversationId, Page page, Model model){
        //设置分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/"+conversationId);
        page.setRows(messageService.findLetterCount(conversationId));

        List<Message> letterList= messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String,Object>> letters=new ArrayList<>();
        if (letterList!=null){
            for (Message message : letterList) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("letter",message);
                map.put("fromUser",userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        model.addAttribute("letters",letters);

        //私信目标
        model.addAttribute("target",getLetterTarget(conversationId));

        //设置已读
        List<Integer> ids=getLetterIds(letterList);
        if (!ids.isEmpty()){
            messageService.readMessage(ids);
        }

        return "/site/letter-detail";
    }

    private List<Integer> getLetterIds(List<Message> letterList){
        ArrayList<Integer> ids = new ArrayList<>();

        if (letterList!=null){
            for (Message message : letterList) {
                if (hostHolder.getUsers().getId()==message.getToId()&&message.getStatus()==0){
                    ids.add(message.getId());
                }

            }
        }
        return ids;
    }

    private User getLetterTarget(String conversationId){
        String[] ids = conversationId.split("_");
        int d0=Integer.parseInt(ids[0]);
        int d1=Integer.parseInt(ids[1]);

        if (d0!=hostHolder.getUsers().getId()){
            return userService.findUserById(d0);
        }else {
            return userService.findUserById(d1);
        }
    }

    @RequestMapping(path="/letter/send",method = RequestMethod.POST)
    @ResponseBody
    public String sendLetter(String toName,String content){
        User target = userService.findUserByName(toName);
        if (target==null){
            return CommunityUtil.getJSONString(1,"目标用户不存在");
        }
        Message message = new Message();
        message.setFromId(hostHolder.getUsers().getId());
        message.setToId(target.getId());
        if (message.getFromId()<message.getToId()){
            message.setConversationId(message.getFromId()+"_"+message.getToId());
        }else{
            message.setConversationId(message.getToId()+"_"+message.getFromId());
        }
        message.setContent(content);
        message.setStatus(0);
        message.setCreateTime(new Date());
        messageService.addMessage(message);

        return CommunityUtil.getJSONString(0);
    }

    @RequestMapping(path = "/notice/list",method =RequestMethod.GET)
    public String getNoticeList(Model model){
        User user = hostHolder.getUsers();
        //查询评论类的通知
        Message message=messageService.findLatestNotice(user.getId(), CommunityConstant.TOPIC_COMMENT);
        Map<String,Object> messageVO=null;
        if (message!=null){
            messageVO=new HashMap<>();
            messageVO.put("message",message);
            //去除转义字符
            String content= HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);

            //xxx评论了你的xxx实体
            messageVO.put("user",userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));
            messageVO.put("postId",data.get("postId"));

            int count=messageService.findNoticeCount(user.getId(),CommunityConstant.TOPIC_COMMENT);
            messageVO.put("count",count);
            int unread=messageService.findNoticeUnreadCount(user.getId(),CommunityConstant.TOPIC_COMMENT);
            messageVO.put("unread",unread);
        }
        model.addAttribute("commentNotice",messageVO);

        //查询点赞类的通知
        message=messageService.findLatestNotice(user.getId(), CommunityConstant.TOPIC_LIKE);

        if (message!=null){
            messageVO=new HashMap<>();
            messageVO.put("message",message);

            String content= HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);

            //xxx点赞了你的xxx实体
            messageVO.put("user",userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));
            messageVO.put("postId",data.get("postId"));

            int count=messageService.findNoticeCount(user.getId(),CommunityConstant.TOPIC_LIKE);
            messageVO.put("count",count);
            int unread=messageService.findNoticeUnreadCount(user.getId(),CommunityConstant.TOPIC_LIKE);
            messageVO.put("unread",unread);
        }
        model.addAttribute("likeNotice",messageVO);

        //查询关注类的通知
        message=messageService.findLatestNotice(user.getId(), CommunityConstant.TOPIC_FOLLOW);

        if (message!=null){
            messageVO=new HashMap<>();
            messageVO.put("message",message);

            String content= HtmlUtils.htmlUnescape(message.getContent());
            Map<String,Object> data = JSONObject.parseObject(content, HashMap.class);

            //xxx点赞了你的xxx实体
            messageVO.put("user",userService.findUserById((Integer) data.get("userId")));
            messageVO.put("entityType",data.get("entityType"));
            messageVO.put("entityId",data.get("entityId"));
            //关注类的通知不需要传入帖子的坐标
            //messageVO.put("postId",data.get("postId"));


            int count=messageService.findNoticeCount(user.getId(),CommunityConstant.TOPIC_FOLLOW);
            messageVO.put("count",count);
            int unread=messageService.findNoticeUnreadCount(user.getId(),CommunityConstant.TOPIC_FOLLOW);
            messageVO.put("unread",unread);
        }
        model.addAttribute("followNotice",messageVO);

        //查询未读消息数量
        int letterUnreadCount=messageService.findLetterUnreadCount(user.getId(),null);
        model.addAttribute("letterUnreadCount",letterUnreadCount);
        int noticeUnreadCount= messageService.findNoticeUnreadCount(user.getId(),null);
        model.addAttribute("noticeUnreadCount",noticeUnreadCount);

        return "/site/notice";
    }

    @RequestMapping(path = "/notice/detail/{topic}",method = RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic")String topic,Page page,Model model){
        //先得到当前用户
        User user = hostHolder.getUsers();

        //设置分页
        page.setLimit(5);
        page.setPath("/notice/detail/"+topic);
        page.setRows(messageService.findNoticeCount(user.getId(),topic));

        List<Message> noticeList = messageService.findNotices(user.getId(), topic,page.getOffset(),page.getLimit());
        //设置视图需要的内容信息
        List<Map<String,Object>> noticeVoList=new ArrayList<>();
        if (noticeList!=null){
            for (Message notice : noticeList) {
                HashMap<String, Object> map = new HashMap<>();
                //通知
                map.put("notice",notice);
                //内容
                String content=HtmlUtils.htmlUnescape(notice.getContent());
                Map<String,Object> data=JSONObject.parseObject(content,HashMap.class);
                map.put("user",userService.findUserById((Integer)data.get("userId")));
                map.put("entityType",data.get("entityType"));
                map.put("entityId",data.get("entityId"));
                map.put("postId",data.get("postId"));
                //通知的作者
                map.put("fromUser",userService.findUserById(notice.getFromId()));
                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices",noticeVoList);

        //设置已读
        List<Integer> ids=getLetterIds(noticeList);
        if (!ids.isEmpty()){
            messageService.readMessage(ids);
        }
        return "/site/notice-detail";
    }

}
