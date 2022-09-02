package com.he.community.service;


import com.he.community.dao.CommentMapper;
import com.he.community.dao.DiscussPostMapper;
import com.he.community.entity.Comment;
import com.he.community.utils.CommunityConstant;
import com.he.community.utils.CommunityUtil;
import com.he.community.utils.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private SensitiveFilter sensitiveFilter;

    public List<Comment> findCommentsByEntity(int entityType,int entityId,int offset,int limit){
        return commentMapper.selectCommentsByEntity(entityType,entityId,offset,limit);
    }

    public int findCommentCount(int entityType,int entityId){
        return commentMapper.selectCountByEntity(entityType,entityId);
    }

    /**
     * 增加评论业务
     * @param comment
     * @return
     */
    //提供事物支持
    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public int addComment(Comment comment){
        if (comment==null){
            throw new IllegalArgumentException("参数不能为空!");
        }
        //过滤html标签和敏感词
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        //添加评论
        int rows=commentMapper.insertComment(comment);

        //更新评论数量
        if (comment.getEntityType()== CommunityConstant.ENTITY_TYPE_POST){
            int count = commentMapper.selectCountByEntity(comment.getEntityType(), comment.getEntityId());
            discussPostMapper.updateCommentCount(comment.getEntityId(),count);
        }

        return rows;
    }

    public Comment findCommentById(int commentId){
        return commentMapper.selectCommentById(commentId);
    }

}
