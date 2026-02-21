package com.atguigu.service.impl;

import com.atguigu.mapper.UserMapper;
import com.atguigu.pojo.User;
import com.atguigu.result.Result;
import com.atguigu.result.ResultCodeEnum;
import com.atguigu.service.IUserService;
import com.atguigu.utils.JwtHelper;
import com.atguigu.utils.MD5Util;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private JwtHelper jwtHelper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Result login(User user) {

        // 1) 账号查询
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, user.getUsername());
        User loginUser = userMapper.selectOne(qw);

        // 2) 账号不存在
        if (loginUser == null) {
            return Result.build(null, ResultCodeEnum.USERNAME_ERROR);
        }

        // 3) 密码校验（明文 -> md5 与数据库对比）
        if (!StringUtils.isBlank(user.getUserPwd())
                && loginUser.getUserPwd().equals(MD5Util.encrypt(user.getUserPwd()))) {

            String token = jwtHelper.createToken(loginUser.getUid().longValue());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            return Result.ok(data);
        }

        // 4) 密码错误
        return Result.build(null, ResultCodeEnum.PASSWORD_ERROR);
    }

    @Override
    public Result getUserInfo(String token) {

        // 1) token 是否过期/无效
        if (jwtHelper.isExpiration(token)) {
            return Result.build(null, ResultCodeEnum.NOTLOGIN);
        }

        // 2) 解析 userId
        Long userId = jwtHelper.getUserId(token);
        if (userId == null) {
            return Result.build(null, ResultCodeEnum.NOTLOGIN);
        }

        // 3) 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            return Result.build(null, ResultCodeEnum.NOTLOGIN);
        }

        // 4) 密码置空再返回
        user.setUserPwd("");

        Map<String, Object> data = new HashMap<>();
        data.put("loginUser", user);
        return Result.ok(data);
    }

    @Override
    public Result checkUserName(String username) {

        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, username);
        Long count = userMapper.selectCount(qw);

        if (count != null && count > 0) {
            return Result.build(null, ResultCodeEnum.USERNAME_USED);
        }

        return Result.ok(null);
    }

    @Override
    public Result regist(User user) {

        // 1) 用户名占用校验
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, user.getUsername());
        Long count = userMapper.selectCount(qw);
        if (count != null && count > 0) {
            return Result.build(null, ResultCodeEnum.USERNAME_USED);
        }

        // 2) 密码加密
        user.setUserPwd(MD5Util.encrypt(user.getUserPwd()));

        // 3) 入库
        userMapper.insert(user);

        return Result.ok(null);
    }
}
