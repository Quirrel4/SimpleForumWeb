package com.he.community;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.stream.Stream;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = MySpringbootApplication.class)
public class KafkaTests {

    @Autowired
    private KafkaProducer kafkaProducer;


    @Test
    public void testKafka(){
        kafkaProducer.sendMessage("test","你好");
        kafkaProducer.sendMessage("test","在吗");

        ArrayList<Integer> list = new ArrayList<>();


        try {
            Thread.sleep(1000*10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

@Component
class KafkaProducer{
    @Autowired
    private KafkaTemplate kafkatemplate;

    public void sendMessage(String topic,String content){
        kafkatemplate.send(topic,content);
    }
}

@Component
class KafkaConsumer{
    //监听名为test的topic，如果有消息，就调用注解标识的方法
    @KafkaListener(topics = {"test"})
    public void handleMessage(ConsumerRecord consumerRecord){
        System.out.println(consumerRecord.value());
    }
}
