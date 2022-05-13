package com.he.community.utils;


import jdk.internal.util.xml.impl.Input;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger= LoggerFactory.getLogger(SensitiveFilter.class);
    //替换符号
    private static final String REPLACEMENT="***";

    private TrieNode rootNode=new TrieNode();

    //容器实例化bean，调用构造器之后就会调用这个@PostConstruct标注的方法
    @PostConstruct
    public void init(){
        //读取target的classes目录下的敏感词文件
        try (
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
                BufferedReader reader=new BufferedReader(new InputStreamReader(is));
                ){
            String keyword;
            while((keyword=reader.readLine())!=null){
                //添加到前缀树
                this.addKeyWord(keyword);
            }
        } catch (IOException e) {
            logger.error("加载敏感词文件失败"+e.getMessage());
        }



    }


    //将一个敏感词添加到前缀树中
    private void addKeyWord(String keyword){
        TrieNode tempNode=rootNode;
        TrieNode sub=null;
        for (int i = 0; i < keyword.length(); i++) {
            char c=keyword.charAt(i);
            if ((sub=tempNode.getSubNode(c))==null){
                sub=new TrieNode();
                tempNode.addSubNodes(c,sub);
            }
            tempNode=sub;
            if (i==keyword.length()-1){
                tempNode.setKeyWordsEnd(true);
            }
        }
    }




    /**
     * 过滤敏感词方法
     * @param text 待过滤的文本
     * @return 过滤后的文本
     */
    public String filter(String text){
        if (StringUtils.isBlank(text)){
            return null;
        }
        //指针1
        TrieNode tempNode=rootNode;

        //指针2,记录text探查位置
        int begin=0;

        //指针3,从begin向后移动
        int postion=0;

        //结果
        StringBuilder stringBuilder=new StringBuilder();

        while(begin< text.length()){
            char c=text.charAt(postion);

                //跳过符号
                if (isSymbol(c)){
                    //若指针1处于root,指针2和3都移动
                    if (tempNode==rootNode){
                        stringBuilder.append(c);
                        begin++;
                    }
                    postion++;
                    continue;
                }

                tempNode=tempNode.getSubNode(c);
                //前缀树里找不到后续单词或者position越界
                if(tempNode==null||postion>=text.length()){
                    //以begin开头的字符串不是敏感词
                    stringBuilder.append(text.charAt(begin));

                    //指针归位
                    tempNode=rootNode;
                    postion=++begin;
                }else if(tempNode.isKeyWordsEnd()){
                    //发现敏感词。begin~postion
                    stringBuilder.append(REPLACEMENT);

                    //指针归位
                    begin=++postion;
                    tempNode=rootNode;
                }else{
                    //继续查找
                    postion++;
                }

        }
        return stringBuilder.toString();
    }


    //判断是否为符号
    public boolean isSymbol(Character c){
        //0x2E80~0x9FFF是东亚文字，日文中文韩文等,欧洲人认为这些也不是正常字符
        //在这些文字之外才有可能是特殊符号
        return !CharUtils.isAsciiAlphanumeric(c)&&(c<0x2E80||c>0x9FFF);
    }


    //前缀树
    private  class TrieNode{
        //关键词结束的标识
        private boolean isKeyWordsEnd=false;
        //key是下级字符，value是下级结点
        private Map<Character,TrieNode> subNodes=new HashMap<>();

        public boolean isKeyWordsEnd() {
            return isKeyWordsEnd;
        }

        public void setKeyWordsEnd(boolean keyWordsEnd) {
            isKeyWordsEnd = keyWordsEnd;
        }

        public void addSubNodes(Character c,TrieNode node){
            subNodes.put(c,node);
        }
        public TrieNode getSubNode(Character c){
            return subNodes.get(c);
        }
    }




}
