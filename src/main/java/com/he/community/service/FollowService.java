package com.he.community.service;



import com.he.community.entity.User;
import com.he.community.utils.CommunityConstant;
import com.he.community.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;


    //用户关注了某个实体
    //因为follower的关注列表要增加，followee的粉丝列表也要增加，两项操作需要用到事务
    public void follow(int userId,int entityType,int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //查询操作在事务外进行
                String followeeKey= RedisKeyUtil.getFolloweeKey(userId,entityType);
                String followerKey=RedisKeyUtil.getFollowerKey(entityType,entityId);
                operations.multi();

                operations.opsForZSet().add(followeeKey,entityId,System.currentTimeMillis());
                operations.opsForZSet().add(followerKey,userId,System.currentTimeMillis());

                return operations.exec();

            }
        });
    }

    //取消关注
    public void unfollow(int userId,int entityType,int entityId){
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                //查询操作在事务外进行
                String followeeKey= RedisKeyUtil.getFolloweeKey(userId,entityType);
                String followerKey=RedisKeyUtil.getFollowerKey(entityType,entityId);
                operations.multi();

                operations.opsForZSet().remove(followeeKey,entityId);
                operations.opsForZSet().remove(followerKey,userId);

                return operations.exec();

            }
        });
    }

    //查询关注实体的数量
    public long findFolloweeCount(int userId,int entityType){
        String followeeKey=RedisKeyUtil.getFolloweeKey(userId,entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    //查询实体的粉丝数量
    public long findFollowerCount(int entityType,int entityId){
        String followerKey=RedisKeyUtil.getFollowerKey(entityType,entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    //查询当前用户是否关注了该实体
    public boolean hasFollowed(int userId,int entityType,int entityId){
        String followeeKey=RedisKeyUtil.getFolloweeKey(userId,entityType);
        return (redisTemplate.opsForZSet().score(followeeKey,entityId)!=null);
    }

    //查询某用户关注的人
    public List<Map<String,Object>> findFollowees(int userId,int offset,int limit){
        String followeeKey=RedisKeyUtil.getFolloweeKey(userId, CommunityConstant.ENTITY_TYPE_USER);
        Set<Integer> targetId = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset + limit - 1);
        if (targetId==null&&targetId.size()==0)return null;
        List<Map<String,Object>> list=new ArrayList<>();
        for (Integer integer : targetId) {
            Map<String,Object> map=new HashMap<>();
            User user = userService.findUserById(integer);
            map.put("user",user);
            Double score = redisTemplate.opsForZSet().score(followeeKey, integer);
            map.put("followTime",new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }


    //查询某用户的粉丝
    public List<Map<String,Object>> findFollowers(int userId,int offset,int limit){
        String followerKey=RedisKeyUtil.getFollowerKey(CommunityConstant.ENTITY_TYPE_USER,userId);
        Set<Integer> targetId=redisTemplate.opsForZSet().range(followerKey,offset,offset+limit-1);

        if (targetId==null&&targetId.size()==0)return null;
        List<Map<String,Object>> list=new ArrayList<>();
        for (Integer integer : targetId) {
            Map<String,Object> map=new HashMap<>();
            User user = userService.findUserById(integer);
            map.put("user",user);
            Double score = redisTemplate.opsForZSet().score(followerKey, integer);
            map.put("followTime",new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }

}
