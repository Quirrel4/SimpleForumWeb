package com.he.community.entity;

public class Page {

    //数据总数，用于计算页码
    int rows;
    //当前页
    int current=1;

    //一页显示的个数
    int limit=10;

    //分页链接
    String path;

    @Override
    public String toString() {
        return "Page{" +
                "rows=" + rows +
                ", current=" + current +
                ", limit=" + limit +
                ", path='" + path + '\'' +
                '}';
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }

    public int getCurrent() {

            return current;

    }

    public void setCurrent(int current) {
        if(current>=1) {
            this.current = current;
        }
    }

    public int getLimit() {

            return limit;

    }

    public void setLimit(int limit) {
        if(limit>=0&&limit<100) {
            this.limit = limit;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Page(int rows, int current, int limit, String path) {
        this.rows = rows;
        this.current = current;
        this.limit = limit;
        this.path = path;
    }

    public Page() {
    }

    public  int getOffset(){
        return (current-1)*limit;
    }

    //获取总页数
    public int getTotalPage(){
        if((rows%limit)==0){
            return rows/limit;
        }else{
            return rows/limit+1;
        }
    }

    public int getFrom(){
        int from=current-2;
        return Math.max(from, 1);
    }

    public int getTo(){
        int to=current+2;
        int total=getTotalPage();
        return Math.min(total,to);
    }

}
