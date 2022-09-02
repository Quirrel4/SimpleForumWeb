package com.he.community.service;

import com.he.community.dao.LoginTicketMapper;
import com.he.community.dao.UserMapper;
import com.he.community.entity.LoginTicket;
import com.he.community.entity.User;
import com.he.community.utils.CommunityConstant;
import com.he.community.utils.CommunityUtil;
import com.he.community.utils.MailClient;
import com.he.community.utils.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;


@Service
public class UserService {



    @Autowired
    private UserMapper userMapper;

/*    @Autowired
    private LoginTicketMapper loginTicketMapper;*/

    //邮件发送工具
    @Autowired
    private MailClient mailClient;
    //模板引擎
    @Autowired
    private TemplateEngine templateEngine;


    @Value("${community.path.domain}")
    private  String domain;

    @Value("${server.servlet.context-path}")
    private  String contextPath;

    @Autowired
    private RedisTemplate redisTemplate;


    public User findUserById(int id){
        //return userMapper.selectById(id);
        User user=getCache(id);
        if (user==null){
            user=initCache(id);
        }
        return user;
    }

    public Map<String,Object> register(User user){
        HashMap<String, Object> map = new HashMap<>();

        //数据合理性检测
        if(user==null){
            throw new IllegalArgumentException("参数不能为空");
        }
        if (StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        if(StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg","邮箱不能为空");
            return map;
        }

        User u = userMapper.selectByName(user.getUsername());
        if(u!=null){
            map.put("usernameMsg","账号已存在");
            return map;
        }
        u=userMapper.selectByEmail(user.getEmail());
        if(u!=null){
            map.put("usernameMsg","该邮箱已被注册");
            return map;
        }


        //排除问题之后进行注册
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0);
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //发送激活邮件
        Context context=new Context();
        context.setVariable("email",user.getEmail());

        // http://localhost:8080/community/activation/101/code

        String url= domain+contextPath+"/activation/"+user.getId()+"/"+user.getActivationCode();
        context.setVariable("url",url);
        String content=templateEngine.process("/mail/activation",context);
        mailClient.sendMail(user.getEmail(),"激活账号",content);

        return map;
    }

    //激活业务
    public int activation(int userId,String code){
        User user = userMapper.selectById(userId);
        if(user.getStatus()==1){
            return CommunityConstant.ACTIVATION_REPEAT;
        } else if(user.getActivationCode().equals(code)){
            userMapper.updateStatus(userId,1);
            clearCache(userId);
            return CommunityConstant.ACTIVATION_SUCCESS;
        }else{
            return CommunityConstant.ACTIVATION_FAILURE;
        }
    }

    //登录业务,同注册业务一样，向服务端返回一个包含错误信息的map
    public Map<String,Object> login(String userName,String password,long expiredSeconds){
        HashMap<String, Object> map = new HashMap<>();

        //空值处理
        if(StringUtils.isBlank(userName)){
            map.put("userNameMsg","账号不能为空");
            return map;
        }
        if(StringUtils.isBlank(password)){
            map.put("passwordMsg","密码不能为空");
            return map;
        }

        //验证账号
        User user = userMapper.selectByName(userName);
        if(user==null){
            map.put("userNameMsg","该账号不存在");
            return map;
        }
        if(user.getStatus()==0){
            map.put("userNameMsg","该账号未激活");
            return map;
        }

        String pwd=CommunityUtil.md5(password+user.getSalt());
        if(!pwd.equals(user.getPassword())){
            map.put("passwordMsg","密码不正确");
            return map;
        }

        //生成登录凭证,用于记录登录状态的保持时间
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setStatus(0);
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setExpired(new Date(System.currentTimeMillis()+expiredSeconds*1000));
        //把凭证插入数据库
        //loginTicketMapper.insertLoginTicket(loginTicket);
        String redisKey= RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(redisKey,loginTicket);  //这里就会自动序列化成一个json字符串


        map.put("ticket",loginTicket.getTicket());
        return map;
    }

    public void logout(String ticket){
        //把凭证设置为无效
        //loginTicketMapper.updateStatus(ticket,1);
        String redisKey= RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket= (LoginTicket) redisTemplate.opsForValue().get(redisKey);
        if(loginTicket==null)return;
        loginTicket.setStatus(1);
        redisTemplate.opsForValue().set(redisKey,loginTicket);
    }

    //查询凭证
    public LoginTicket findLoginTicket(String ticket){
        //return loginTicketMapper.selectByTicket(ticket);
        String redisKey= RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(redisKey);
    }

    public int updateHeader(int userId,String headerUrl){
        //return userMapper.updateHeader(userId,headerUrl);

        //这里先更新再清理数据
        //redis和mysql的事务是分开的,这里更新成功再清理缓存
        int rows = userMapper.updateHeader(userId, headerUrl);
        if (rows!=0)clearCache(userId);
        return rows;
    }

    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

    //使用redis缓存用户信息
    //1，优先从缓存中取值
    private User getCache(int userId){
        String redisKey=RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(redisKey);
    }

    //2，取不到时初始化缓存数据
    private User initCache(int userId){
        User user=userMapper.selectById(userId);
        String redisKey=RedisKeyUtil.getUserKey(userId);
        redisTemplate.opsForValue().set(redisKey,user,3600, TimeUnit.SECONDS);
        return user;
    }

    //3，数据变更时清楚缓存数据
    private void clearCache(int userId){
        String redisKey=RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(redisKey);
    }

}
