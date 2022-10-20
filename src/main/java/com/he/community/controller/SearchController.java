package com.he.community.controller;

import com.he.community.entity.DiscussPost;
import com.he.community.entity.Page;
import com.he.community.service.ElasticSearchService;
import com.he.community.service.LikeService;
import com.he.community.service.UserService;
import com.he.community.utils.CommunityConstant;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class SearchController {

    @Autowired
    private ElasticSearchService elasticSearchService;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeService likeService;

    //  search?keyword=xxx
    @RequestMapping(path = "/search",method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) throws IOException {
        //搜索帖子,elasticsearch的current从0开始
        Map<String, Object> searchResult = elasticSearchService.searchDiscussPost(keyword, page.getOffset(), page.getLimit());
        List<DiscussPost> list = (List<DiscussPost>) searchResult.get("DiscussPosts");
        ArrayList<Map<String, Object>> discussPosts = new ArrayList<>();

        if (list!=null){
            for (DiscussPost discussPost : list) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("post",discussPost);
                map.put("user",userService.findUserById(discussPost.getUserId()));
                map.put("likeCount",likeService.findEntityLikeCount(CommunityConstant.ENTITY_TYPE_POST,discussPost.getId()));

                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts",discussPosts);
        model.addAttribute("keyword",keyword);

        page.setPath("/search?keyword="+keyword);
        page.setRows(list==null?0: ((Long)searchResult.get("HitCount")).intValue());



        return "/site/search";
    }
}
