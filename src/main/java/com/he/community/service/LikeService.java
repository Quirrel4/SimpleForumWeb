package com.he.community.service;


import com.he.community.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    @Autowired
    private RedisTemplate redisTemplate;


    //点赞,userId标识点赞者，entity标识被点赞的帖子/评论,entityUserId标识发帖人/评论人
    //entity代表实体，可以是帖子/评论/回复
    //entityId与数据库里评论/回复所属的id不同,代表是的标识实体的id
    public void like(int userId,int entityType,int entityId,int entityUserId){
/*
        String entityLikeKey= RedisKeyUtil.getEntityLikeKey(entityType,entityId);
        boolean isMember=redisTemplate.opsForSet().isMember(entityLikeKey,userId);
        if (isMember){
            redisTemplate.opsForSet().remove(entityLikeKey,userId);
        }else{
            redisTemplate.opsForSet().add(entityLikeKey,userId);
        }
*/
        redisTemplate.execute(new SessionCallback() {
            //做一个编程式事务
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String entityLikeKey= RedisKeyUtil.getEntityLikeKey(entityType,entityId);
                String userLikeKey=RedisKeyUtil.getUserLikeKey(entityUserId);

                //判断是否点过赞
                boolean isMemeber=operations.opsForSet().isMember(entityLikeKey,userId);

                //开启事务
                operations.multi();

                if (isMemeber){
                    operations.opsForSet().remove(entityLikeKey,userId);
                    operations.opsForValue().decrement(userLikeKey);
                }else{
                    operations.opsForSet().add(entityLikeKey,userId);
                    operations.opsForValue().increment(userLikeKey);
                }

                //执行事务
                return operations.exec();
            }
        });


    }

    //查询某个帖子/评论的点赞数量
    public long findEntityLikeCount(int entityType,int entityId){
        String entityLikeKey=RedisKeyUtil.getEntityLikeKey(entityType,entityId);
        return redisTemplate.opsForSet().size(entityLikeKey);
    }

    //查询某人对某个实体的点赞状态
    public int findEntityLikeStatus(int userId,int entityType,int entityId){
        String entityLikeKey=RedisKeyUtil.getEntityLikeKey(entityType,entityId);
        return redisTemplate.opsForSet().isMember(entityLikeKey,userId)?1:0;
    }

    //查询某个用户获得的赞
    public int findUserLikedCount(int userId){
        String userLikeKey=RedisKeyUtil.getUserLikeKey(userId);
        Integer count= (Integer) redisTemplate.opsForValue().get(userLikeKey);

        return count==null?0: count;
    }





}
