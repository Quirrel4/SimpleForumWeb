package com.he.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes= MySpringbootApplication.class)
public class RedisTests {

    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testStrings(){
        String redisKey="test:ok";
        redisTemplate.opsForValue().set(redisKey,1);
        System.out.println(redisTemplate.opsForValue().get(redisKey));
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }

    @Test
    public void testHashs(){
        String redisKey="test:hash";
        redisTemplate.opsForHash().put(redisKey,"id",1 );
        redisTemplate.opsForHash().put(redisKey,"username","zhangsan");

        System.out.println(redisTemplate.opsForHash().get(redisKey,"id"));
        System.out.println(redisTemplate.opsForHash().get(redisKey,"username"));
    }

    @Test
    public void testLists(){
        String redisKey="test:list";

        redisTemplate.opsForList().leftPush(redisKey,101);
        redisTemplate.opsForList().leftPush(redisKey,102);
        redisTemplate.opsForList().leftPush(redisKey,103);
        System.out.println(redisTemplate.opsForList().size(redisKey));
        System.out.println(redisTemplate.opsForList().index(redisKey,0));
        System.out.println(redisTemplate.opsForList().range(redisKey,0,2));

        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
    }

    @Test
    public void testSets(){
        String redisKey="test:man";

        redisTemplate.opsForSet().add(redisKey,"刘备","张飞","关羽","赵云","诸葛亮");
        System.out.println(redisTemplate.opsForSet().size(redisKey));
        System.out.println(redisTemplate.opsForSet().pop(redisKey));
        System.out.println(redisTemplate.opsForSet().members(redisKey));
    }

    @Test
    public  void testSortedSets(){
        String redisKey="test:haha";

        redisTemplate.opsForZSet().add(redisKey,"唐僧",80);
        redisTemplate.opsForZSet().add(redisKey,"悟空",70);
        redisTemplate.opsForZSet().add(redisKey,"八戒",60);
        redisTemplate.opsForZSet().add(redisKey,"沙僧",50);
        redisTemplate.opsForZSet().add(redisKey,"白龙马",40);

        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));
        System.out.println(redisTemplate.opsForZSet().score(redisKey,"八戒"));
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey,"八戒"));
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey,0,2));
    }


    @Test
    public void testKeys(){
        redisTemplate.delete("test:haha");
        System.out.println(redisTemplate.hasKey("test:haha"));

        redisTemplate.expire("test:man",10, TimeUnit.SECONDS);
    }

    //多次访问同一个key
    @Test
    public void testBoundOperations(){
        String redisKey="test:count";
        BoundValueOperations operations=redisTemplate.boundValueOps(redisKey);
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());
    }

    @Test
    public void testTransactional(){
       Object obj= redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String redisKey="test:tx";
                operations.multi();

                operations.opsForSet().add(redisKey,"张三");
                operations.opsForSet().add(redisKey,"李四");
                operations.opsForSet().add(redisKey,"王五");
                System.out.println(operations.opsForSet().members(redisKey));

                return operations.exec();
            }
        });
        System.out.println(obj.toString());
    }

    @Test
    public void testHyperLogLog(){
        String redisKey="test:hll:01";
        for (int i = 0; i < 100000; i++) {
            redisTemplate.opsForHyperLogLog().add(redisKey,i);
        }

        for (int i = 0; i < 100000; i++) {
            int r=(int)(Math.random()*100000+1);
            redisTemplate.opsForHyperLogLog().add(redisKey,r);
        }
        Long size = redisTemplate.opsForHyperLogLog().size(redisKey);
        System.out.println(size);
    }

    @Test
    public void testHyperLogLogUnion(){
        String rediskey2="test:hll:02";
        for (int i = 0; i < 10000; i++) {
            redisTemplate.opsForHyperLogLog().add(rediskey2,i);
        }

        String rediskey3="test:hll:03";
        for (int i = 5001; i < 15000; i++) {
            redisTemplate.opsForHyperLogLog().add(rediskey3,i);
        }

        String rediskey4="test:hll:04";
        for (int i = 15001; i < 20000; i++) {
            redisTemplate.opsForHyperLogLog().add(rediskey4,i);
        }

        String unionKey="test:hll:union";
        redisTemplate.opsForHyperLogLog().union(unionKey,rediskey2,rediskey3,rediskey4);

        Long size = redisTemplate.opsForHyperLogLog().size(unionKey);
        System.out.println(size);
    }

    @Test
    public void testBitMap(){
        String redisKey="test:bm:01";
        redisTemplate.opsForValue().setBit(redisKey,1,true);
        redisTemplate.opsForValue().setBit(redisKey,4,true);
        redisTemplate.opsForValue().setBit(redisKey,7,true);

        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey, 2));

        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.bitCount(redisKey.getBytes());
            }
        });
        System.out.println(obj);
    }

    //统计三组数据的布尔值，并做OR运算
    @Test
    public void testBitMapOperation(){
        String redisKey2="test:bm:02";
        redisTemplate.opsForValue().setBit(redisKey2,0,true);
        redisTemplate.opsForValue().setBit(redisKey2,1,true);
        redisTemplate.opsForValue().setBit(redisKey2,2,true);

        String redisKey3="test:bm:03";
        redisTemplate.opsForValue().setBit(redisKey3,2,true);
        redisTemplate.opsForValue().setBit(redisKey3,3,true);
        redisTemplate.opsForValue().setBit(redisKey3,4,true);

        String redisKey4="test:bm:04";
        redisTemplate.opsForValue().setBit(redisKey3,4,true);
        redisTemplate.opsForValue().setBit(redisKey3,5,true);
        redisTemplate.opsForValue().setBit(redisKey3,6,true);


        String redisKey="test:bm:or";
        Object obj=redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                connection.bitOp(RedisStringCommands.BitOperation.OR,
                        redisKey.getBytes(),
                        redisKey2.getBytes(),redisKey3.getBytes(),redisKey4.getBytes());
                return connection.bitCount(redisKey.getBytes());
            }
        });
        System.out.println(obj);

        System.out.println(redisTemplate.opsForValue().getBit(redisKey,0));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,2));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,3));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,4));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,5));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,6));

    }




}
