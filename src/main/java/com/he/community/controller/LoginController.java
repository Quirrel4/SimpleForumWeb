package com.he.community.controller;


import com.google.code.kaptcha.Producer;
import com.he.community.entity.User;
import com.he.community.service.UserService;
import com.he.community.utils.CommunityConstant;
import com.he.community.utils.CommunityUtil;
import com.he.community.utils.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController {
    @Autowired
    private UserService userService;

    @Autowired
    private  Producer kaptchaProducer;

    private Logger logger= LoggerFactory.getLogger(LoginController.class);

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private RedisTemplate redisTemplate;


    @RequestMapping(path = "/register", method = RequestMethod.GET)
    public String getRegisterPage(){
        return "site/register";
    }

    @RequestMapping(path = "/login", method = RequestMethod.GET)
    public String getLoginPage(){
        return "site/login";
    }

    @RequestMapping(path = "/register",method = RequestMethod.POST)
    public  String register(Model model, User user){

        //user已经存在model里了，如果注册失败，user的内容也会一并传给注册页面
        Map<String, Object> map = userService.register(user);

        if(map==null||map.isEmpty()){
            model.addAttribute("msg","注册成功");
            model.addAttribute("target","/index");
            return "site/operate-result";
        }else{
            model.addAttribute("usernameMsg",map.get("usernameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            model.addAttribute("emailMsg",map.get("emailMsg"));
            return "site/register";
        }
    }

    // http://localhost:8080/community/activation/101/code
    @RequestMapping(path="/activation/{userId}/{code}",method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId")int userId, @PathVariable("code")String code){

        int activation = userService.activation(userId, code);

        if(activation== CommunityConstant.ACTIVATION_SUCCESS){

            model.addAttribute("msg","激活成功，您的帐号可以正常使用了");

            model.addAttribute("target","/login");

        }else if(activation==CommunityConstant.ACTIVATION_REPEAT){

            model.addAttribute("msg","无效操作，账号已被激活");

            model.addAttribute("target","/index");

        }else if(activation==CommunityConstant.ACTIVATION_FAILURE){

            model.addAttribute("msg","激活失败，激活码错误");

            model.addAttribute("target","/index");

        }
        return "site/operate-result";
    }

    @RequestMapping(path ="/kaptcha",method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session){

        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);

        //创建一个验证码的归属owner,存到每个客户端中（浏览器），每个客户端登录的时候，服务器就会从浏览器的cookie中取出，来跟验证码做验证
        String kaptchaOwner= CommunityUtil.generateUUID();
        Cookie cookie=new Cookie("kaptchaOwner",kaptchaOwner);
        cookie.setMaxAge(60);
        //让这个cookie在全路径下都有效
        cookie.setPath(contextPath);
        response.addCookie(cookie);

        //将验证码存入Redis
        //redis中的key值为处理过的KaptchaOwner，是一个简单的uuid，存到cookie中，由登录请求拿到这个cookie里的值，前缀处理过之后就可以拿到redis里的数据了
        String redisKey= RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey,text,60, TimeUnit.SECONDS);



        //原本是存入session中的，现在改成存在redis中，提高性能
        /*
        session.setAttribute("kaptcha",text);
         */


        response.setContentType("image/png");
        try {
            ServletOutputStream outputStream = response.getOutputStream();
            ImageIO.write(image,"png",outputStream);
        } catch (IOException e) {
            logger.error("响应验证码失败"+e.getMessage());
        }
    }

    @RequestMapping(path ="/login",method = RequestMethod.POST)
    public  String login(String userName,String password,String code,boolean rememberMe,
                         Model model,
                         /*HttpSession session,*/
                         HttpServletResponse response,@CookieValue("kaptchaOwner")String kaptchaOwner)
    {
        //得到验证码,通过从客户端的cookie中取到标识字符串kaptchaOwner来得到其对应的kaptcha
        //String kaptcha= (String) session.getAttribute("kaptcha");
        String kaptcha=null;
        String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        if (StringUtils.isNotBlank(kaptchaOwner)){
            String redisKey=RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha= (String) redisTemplate.opsForValue().get(redisKey);
        }



        //判断验证码
        if(StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code)|| !kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsg","验证码不正确");
            return "/site/login";
        }
        //检查账号密码
        int expiredSeconds=rememberMe?CommunityConstant.REMEM_EXPIRED_SECONDS:CommunityConstant.DEFAULT_EXPIRED_SECONDS;
        Map<String, Object> map = userService.login(userName, password, expiredSeconds);
        if (map.containsKey("ticket")){
            //成功
            //ticket不是敏感信息，所以存在cookie里就可以
            Cookie cookie=new Cookie("ticket",map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);

            //防止第一次登录时客户端还没有ticket的cookie
            // 不能从request中得到这个cookie从而经过拦截器
            // 所以要重定向刷新状态
            return "redirect:/index";
        }else{
            //失败，重新回到登录页面
            model.addAttribute("userNameMsg",map.get("userNameMsg"));
            model.addAttribute("passwordMsg",map.get("passwordMsg"));
            return "site/login";
        }
    }

    @RequestMapping(path = "/logout",method = RequestMethod.GET)
    public String logout(@CookieValue("ticket")String ticket){
        userService.logout(ticket);
        return "redirect:/login";
    }


}
