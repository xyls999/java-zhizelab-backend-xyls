package com.atguigu.service;

import com.atguigu.pojo.User;
import com.atguigu.result.Result;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IUserService extends IService<User> {

    Result login(User user);

    Result getUserInfo(String token);

    Result checkUserName(String username);

    Result regist(User user);
}
