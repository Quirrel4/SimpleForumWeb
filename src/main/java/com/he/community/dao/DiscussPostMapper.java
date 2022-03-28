package com.he.community.dao;


import com.he.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DiscussPostMapper {

    List<DiscussPost> selectDiscussPosts(int userId,int offset,int limit);

    //如果只有一个参数，且在<if>里使用，则必须使用@Param
    int selectDiscussPostsRows(@Param("userId") int userId);
}
