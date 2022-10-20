package com.he.community.actuator;


import com.he.community.utils.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@Endpoint(id = "database")
public class DataBaseEndPoint {
    private static final Logger logger= LoggerFactory.getLogger(DataBaseEndPoint.class);

    @Autowired
    private DataSource dataSource;

    // 注解表示一个get请求
    @ReadOperation
    public String checkConnectiion(){
        try(
                Connection connection = dataSource.getConnection();
         ) {
            return CommunityUtil.getJSONString(0,"获取链接成功!");
        } catch (SQLException e) {
            logger.error("获取连接失败: "+ e.getMessage());
            return CommunityUtil.getJSONString(1,"获取连接失败!");
        }
    }
}
