package com.he.community.event;

import com.alibaba.fastjson.JSONObject;
import com.he.community.entity.DiscussPost;
import com.he.community.entity.Event;
import com.he.community.entity.Message;
import com.he.community.service.DiscussPostService;
import com.he.community.service.ElasticSearchService;
import com.he.community.service.MessageService;
import com.he.community.utils.CommunityConstant;
import com.he.community.utils.CommunityUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;


@Component
public class EventConsumer {
    private static final Logger logger= LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticSearchService elasticSearchService;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;


    //一个方法处理评论，点赞，关注三种事件
    @KafkaListener(topics = {CommunityConstant.TOPIC_COMMENT,CommunityConstant.TOPIC_LIKE,CommunityConstant.TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息的内容为空");
            return;
        }
        //生产者把Event转换成JSON字符串send给消费者，consumer再转成Event类型来使用
        Event event= JSONObject.parseObject(record.value().toString(),Event.class);
        if (event==null){
            logger.error("格式错误");
            return;
        }
        //利用Event对象里的信息，发送给用户一个Message对象
        Message message = new Message();
        message.setFromId(CommunityConstant.SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        message.setCreateTime(new Date());

        //content处理
        Map<String,Object> map=new HashMap<>();
        map.put("userId",event.getUserId());
        map.put("entityType",event.getEntityType());
        map.put("entityId",event.getEntityId());
        if (!event.getData().isEmpty()){
            for (Map.Entry<String, Object> entry : event.getData().entrySet()) {
                map.put(entry.getKey(), entry.getValue());
            }
        }

        message.setContent(JSONObject.toJSONString(map));
        messageService.addMessage(message);
    }



    //消费发帖事件，主要是负责装载到ElasticSearch中
    @KafkaListener(topics = {CommunityConstant.TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null){
            logger.error("消息格式错误");
            return;
        }


        DiscussPost post=discussPostService.findDiscussPostById(event.getEntityId());
        elasticSearchService.saveDiscussPost(post);
    }


    //消费删帖事件
    @KafkaListener(topics = {CommunityConstant.TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息的内容为空!");
            return;
        }

        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event==null){
            logger.error("消息格式错误");
            return;
        }

        elasticSearchService.deleteDiscussPost(event.getEntityId());
    }

    //消费分享事件
    @KafkaListener(topics = CommunityConstant.TOPIC_SHARE)
    public void handleShareMessage(ConsumerRecord record){
        if (record==null||record.value()==null){
            logger.error("消息的内容为空!");
            return;
        }
        Event event=JSONObject.parseObject(record.value().toString(),Event.class);
        if (event==null){
            logger.error("消息格式错误!");
            return;
        }
        String htmlUrl= (String) event.getData().get("htmlUrl");
        String fileName= (String) event.getData().get("fileName");
        String suffix= (String) event.getData().get("suffix");

        String cmd=wkImageCommand+" --quality 75 "+htmlUrl+" "+wkImageStorage+"/"+fileName+suffix;

        try {
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功: "+cmd);
        } catch (IOException e) {
            logger.info(cmd);
            logger.error("生成长图失败: "+e.getMessage());
        }

        //因为图片生成需要事件，防止还没有生成图片就执行把图片上传到七牛云的逻辑
        //启动一个定时器，检测图片是否生成了，生成则上传七牛云
        //因为kafka的消费者抢占机制，这部分代码只有一台服务器会执行，不存在多台服务器执行同一段任务
        UploadTask task = new UploadTask(fileName, suffix);
        //触发定时器
        Future future = taskScheduler.scheduleAtFixedRate(task, 500);
        task.setFuture(future);

    }


    class UploadTask implements Runnable{

        //文件名称
        private String fileName;
        //文件后缀
        private String suffix;
        //启动任务的返回值，可以用来停止定时器
        private Future future;

        //开始事件，用于检测超时
        private long startTime;
        // 上传次数,用于检测云服务器是否出问题（或网络问题）
        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            startTime=System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            // 生成图片失败
            if (System.currentTimeMillis()-startTime>30000){
                logger.error("执行时间过长,终止任务: "+fileName);
                future.cancel(true);
                return;
            }
            //上传失败
            if (uploadTimes>=3){
                logger.error("上传次数过多,终止任务: "+fileName);
                future.cancel(true);
                return;
            }

            String path=wkImageStorage+"/"+fileName+suffix;
            File file = new File(path);
            //定时调用直到存在
            if (file.exists()){
                logger.info(String.format("开始第%d次上传[%s].",++uploadTimes,fileName));
                //设置响应信息
                StringMap policy=new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));
                //生成上传凭证
                Auth auth = Auth.create(accessKey, secretKey);
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600, policy);
                //指定上传的机房
                UploadManager manager=new UploadManager(new Configuration(Zone.zone1()));
                try {
                    //开始上传图片,得到一个JSON的封装对象
                    Response response=manager.put(
                            path,fileName,uploadToken,null,"image/"+suffix,false
                    );
                    // 处理响应结果
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if (json==null||json.get("code")==null||json.get("code").toString().equals("0")){
                    }else{
                        logger.info(String.format("第%d次上传成功[%s]."),uploadTimes,fileName);
                    }
                }catch (QiniuException e){
                    logger.info(String.format("第%d次上传失败[%d].",uploadTimes,fileName));
                }
            }else{
                logger.info("等待图片生成["+fileName+"].");
            }
        }
    }


}
