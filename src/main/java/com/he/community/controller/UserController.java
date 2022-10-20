package com.he.community.controller;


import com.he.community.annotation.LoginRequired;
import com.he.community.entity.User;
import com.he.community.service.FollowService;
import com.he.community.service.LikeService;
import com.he.community.service.UserService;
import com.he.community.utils.CommunityConstant;
import com.he.community.utils.CommunityUtil;
import com.he.community.utils.HostHolder;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;


import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping("/user")
public class UserController {

    private static Logger logger= LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private  String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @LoginRequired
    @RequestMapping(path="/setting",method = RequestMethod.GET)
    public String getSettings(Model model){
        //上传文件的名称
        String fileName=CommunityUtil.generateUUID();

        //设置响应信息
        StringMap policy=new StringMap();
        policy.put("returnBody",CommunityUtil.getJSONString(0));

        //生成上传凭证，让表单post给七牛云才能使上传生效
        Auth auth=Auth.create(accessKey,secretKey);
        String uploadToken=auth.uploadToken(headerBucketName,fileName,3600,policy);

        model.addAttribute("uploadToken",uploadToken);
        model.addAttribute("fileName",fileName);

        return "/site/setting";
    }


    //更新数据库中头像的路径
    @RequestMapping(path = "/header/url",method = RequestMethod.POST)
    @ResponseBody
    public String updateHeaderUrl(String fileName){
        if (StringUtils.isBlank(fileName)){
            return CommunityUtil.getJSONString(1,"文件名不能为空!");
        }
        String url=headerBucketUrl+"/"+fileName;
        userService.updateHeader(hostHolder.getUsers().getId(),url);

        return CommunityUtil.getJSONString(0);
    }


    //废弃
    //处理图片上传.表单要用字节流，post方法
    @LoginRequired
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        //没有上传图片
        if (headerImage==null){
            model.addAttribute("error","还没有选择图片");
            return "/site/setting";
        }

        //没有后缀
        String fileName = headerImage.getOriginalFilename();
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件格式不正确");
            return "/site/setting";
        }

        //生成随机文件名
        fileName=CommunityUtil.generateUUID()+suffix;
        //确认存放路径
        File dest=new File(uploadPath+"/"+fileName);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败"+e.getMessage());
            throw  new RuntimeException("上传文件失败，服务器发生异常!",e);
        }
        //改变当前用户头像路径,web访问路径 http://localhost8080/community/user/header/xxx.png
        //为这个url写一个controller方法映射
        User user=hostHolder.getUsers();
        String headerUrl=domain+contextPath+"/user/header/"+fileName;
        //写入数据库
        userService.updateHeader(user.getId(),headerUrl);
        return "redirect:/index";
    }


    // 废弃
    //还可以获取别人的头像
    //通过流向浏览器响应一个图片
    @RequestMapping(path = "/header/{fileName}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName")String fileName, HttpServletResponse response){
        //服务器存放路径
        fileName=uploadPath+"/"+fileName;
        //后缀
        String suffix=fileName.substring(fileName.lastIndexOf("."));
        //响应图片
        response.setContentType("image/"+suffix);

        try (
                FileInputStream fis=new FileInputStream(fileName);
                OutputStream os = response.getOutputStream();
        ){
            byte[] buffer=new byte[1024];
            int b=0;
            while((b=fis.read(buffer))!=-1){
                os.write(buffer,0,b);
            }

        } catch (IOException e) {
            logger.error("读取头像失败"+e.getMessage());
        }
    }

    //个人主页
    @RequestMapping(path = "/profile/{userId}",method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId")int userId,Model model){
        User user=userService.findUserById(userId);
        if (user==null){
            throw new RuntimeException("该用户不存在");
        }

        //该用户的粉丝
        long followerCount = followService.findFollowerCount(CommunityConstant.ENTITY_TYPE_USER, userId);
        //该用户的关注
        long followeeCount = followService.findFolloweeCount(userId, CommunityConstant.ENTITY_TYPE_USER);
        //是否关注，还要检查下是否登录
        boolean hasFollowed=false;
        if (hostHolder.getUsers()!=null){
            hasFollowed=followService.hasFollowed(hostHolder.getUsers().getId(),CommunityConstant.ENTITY_TYPE_USER,userId);
        }

        model.addAttribute("hasFollowed",hasFollowed);
        model.addAttribute("followerCount",followerCount);
        model.addAttribute("followeeCount",followeeCount);

        //用户的基本信息
        model.addAttribute("user",user);
        //查询获赞数量
        int likedCount = likeService.findUserLikedCount(userId);
        model.addAttribute("likeCount",likedCount);
        return "/site/profile";
    }



}
