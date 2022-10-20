package com.he.community.service;

import com.he.community.utils.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class DataService {
    @Autowired
    private RedisTemplate redisTemplate;

    private SimpleDateFormat dateFormat=new SimpleDateFormat("yyyyMMdd");

    //将指定的IP计入UV
    public void recordUV(String ip){
        String redisKey = RedisKeyUtil.getUVKey(dateFormat.format(new Date()));
        redisTemplate.opsForHyperLogLog().add(redisKey,ip);
    }

    //统计指定日期范围内的UV
    public long caculateUV(Date start,Date end){
        if (start==null||end==null){
            throw new IllegalArgumentException("参数不能为空!");
        }
        //整理日期范围内的key
        List<String> keyList=new ArrayList<>();
        Calendar calendar=Calendar.getInstance();
        calendar.setTime(start);
        //calendar的时间 不晚于 end这个时间
        while(!calendar.getTime().after(end)){
            String key = RedisKeyUtil.getUVKey(dateFormat.format(calendar.getTime()));
            keyList.add(key);
            calendar.add(Calendar.DATE,1);
        }

        // 合并这些数据
        String redisKey=RedisKeyUtil.getUVKey(dateFormat.format(start), dateFormat.format(end));
        redisTemplate.opsForHyperLogLog().union(redisKey,keyList.toArray());

        return redisTemplate.opsForHyperLogLog().size(redisKey);
    }

    //将指定用户计入DAU
    public void recordDAU(int userId){
        String redisKey=RedisKeyUtil.getDAUKey(dateFormat.format(new Date()));
        redisTemplate.opsForValue().setBit(redisKey,userId,true);
    }

    //统计日期区间范围内的DAU
    public long caculateDAU(Date start,Date end){
        if (start==null||end==null){
            throw new IllegalArgumentException("参数不能为空!");
        }

        //整理该日期范围内的key
        ArrayList<byte[]> keyList = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(start);
        while(!calendar.getTime().after(end)){
            String key = RedisKeyUtil.getDAUKey(dateFormat.format(calendar.getTime()));
            keyList.add(key.getBytes());
            calendar.add(calendar.DATE,1);
        }

        //进行OR运算
        return (long)redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                String redisKey=RedisKeyUtil.getDAUKey(dateFormat.format(start),dateFormat.format(end));
                connection.bitOp(RedisStringCommands.BitOperation.OR,redisKey.getBytes(),keyList.toArray(new byte[0][0]));
                return connection.bitCount(redisKey.getBytes());
            }
        });
    }


}
