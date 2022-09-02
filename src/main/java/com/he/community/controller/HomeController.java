package com.he.community.controller;


import com.he.community.entity.DiscussPost;
import com.he.community.entity.Page;
import com.he.community.entity.User;
import com.he.community.service.DiscussPostService;
import com.he.community.service.LikeService;
import com.he.community.service.UserService;
import com.he.community.utils.CommunityConstant;
import com.sun.org.apache.xpath.internal.operations.Mod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    @RequestMapping(path="/index",method = RequestMethod.GET)
    public String getIndexPage(Model model, Page page){
        //DisPathcerServlet会自动实例化Model和Page，并将Page注入Model
        //所以在Thymeleaf可以直接取到page

        //页码设置
        page.setPath("/index");
        page.setRows(discussPostService.finDiscussPostRows(0));


        List<DiscussPost> discussPosts = discussPostService.findDiscussPosts(0, page.getOffset(), page.getLimit());
        List<Map<String,Object>> list=new ArrayList<>();
        if(list!=null){
            for (DiscussPost discussPost : discussPosts) {
                HashMap<String, Object> map = new HashMap<>();
                //存入文章
                map.put("post",discussPost);
                //查询并存入用户
                User userById = userService.findUserById(discussPost.getUserId());
                map.put("user",userById);

                long likeCount=likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_POST,discussPost.getId());
                map.put("likeCount",likeCount);

                list.add(map);
            }
            model.addAttribute("discussPosts",list);
        }
        return "index";
    }

    @RequestMapping(path="/error",method = RequestMethod.GET)
    public String getErrorPage(){
        return "/error/500";
    }



}
