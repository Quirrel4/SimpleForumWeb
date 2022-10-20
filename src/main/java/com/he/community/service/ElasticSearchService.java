package com.he.community.service;

import com.alibaba.fastjson.JSONObject;
import com.he.community.dao.elasticsearch.DiscussPostRepository;
import com.he.community.entity.DiscussPost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticSearchService {
    @Autowired
    private DiscussPostRepository discussRepository;
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public void saveDiscussPost(DiscussPost post){
        discussRepository.save(post);
    }

    public void deleteDiscussPost(int id){
        discussRepository.deleteById(id);
    }

    public Map<String,Object> searchDiscussPost(String keyword, int current, int limit) throws IOException {

        ArrayList<DiscussPost> list = new ArrayList<>();
        HashMap<String, Object> map = new HashMap<>();
        SearchRequest searchRequest = new SearchRequest("discusspost");

        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title").field("content")
                .preTags("<span style='color:red'>")
                .postTags("</span>");

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
                .query(QueryBuilders.multiMatchQuery(keyword, "title", "content"))
                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
                .from(current)
                .size(limit)
                .highlighter(highlightBuilder);

        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);


        for (SearchHit hit : searchResponse.getHits().getHits()) {
            DiscussPost post = JSONObject.parseObject(hit.getSourceAsString(), DiscussPost.class);
            //注入高亮标题
            HighlightField title = hit.getHighlightFields().get("title");
            if (title!=null){
                post.setTitle(title.getFragments()[0].toString());
            }
            //注入高亮内容
            HighlightField content = hit.getHighlightFields().get("content");
            if (content!=null){
                post.setContent(content.getFragments()[0].toString());
            }
            list.add(post);

        }
        map.put("DiscussPosts",list);
        map.put("HitCount",searchResponse.getHits().getTotalHits().value);
        return map;



    }

}
