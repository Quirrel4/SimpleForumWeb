package com.he.community.service;


import com.he.community.dao.MessageMapper;
import com.he.community.entity.Message;
import com.he.community.utils.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class MessageService {
    @Autowired
    private MessageMapper messageMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;

    //查询当前用户所有的私信列表
    public List<Message> findConversations(int userId,int offset,int limit){
        return messageMapper.selectConversations(userId,offset,limit);
    }
    //查询当前用户的私信数量，用于提示
    public int findConversationCount(int userId){
        return messageMapper.selectConversationCount(userId);
    }

    //查询某个会话所有的私信列表
    public List<Message> findLetters(String conversationId,int offset,int limit){
        return messageMapper.selectLetters(conversationId,offset,limit);
    }

    //查询某个会话包含的私信数量
    public int findLetterCount(String conversationId){
        return messageMapper.selectLetterCount(conversationId);
    }

    //查询某个用户的某个会话/全部会话未读的私信数量，用于显示
    public int findLetterUnreadCount(int userId,String conversationId){
        return messageMapper.selectLetterUnreadCount(userId,conversationId);
    }

    //发送私信业务
    public int addMessage(Message message){
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilter.filter(message.getContent()));
        return messageMapper.insertMessage(message);
    }

    //设置已读业务
    public int readMessage(List<Integer> ids){
        return messageMapper.updateStatus(ids,1);
    }
}
