package com.he.community.utils;


import com.he.community.entity.User;
import org.springframework.stereotype.Component;

/*
持有用户信息，用于代替session对象
且是线程独立的，通过HreadLocal实现，根据线程为key值存储数据
*/
@Component
public class HostHolder {
    private ThreadLocal<User> users=new ThreadLocal<>();

    public void setUsers(User user){
        users.set(user);
    }
    public User getUsers(){
        return users.get();
    }
    public void clear(){
        users.remove();
    }

}
