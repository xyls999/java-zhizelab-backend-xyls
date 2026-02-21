package com.atguigu.controller;

import com.atguigu.pojo.User;
import com.atguigu.result.Result;
import com.atguigu.result.ResultCodeEnum;
import com.atguigu.service.IUserService;
import com.atguigu.utils.JwtHelper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("user")
@CrossOrigin
public class UserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private JwtHelper jwtHelper;

    // 1) 登录：/user/login
    @PostMapping("login")
    public Result login(@RequestBody User user) {
        return userService.login(user);
    }

    // 2) 根据 token 获取用户：/user/getUserInfo
    @GetMapping("getUserInfo")
    public Result userInfo(@RequestHeader String token) {
        return userService.getUserInfo(token);
    }

    // 3) 注册用户名检查：/user/checkUserName（param）
    @PostMapping("checkUserName")
    public Result checkUserName(@RequestParam String username) {
        return userService.checkUserName(username);
    }

    // 4) 注册：/user/regist
    @PostMapping("regist")
    public Result regist(@RequestBody User user) {
        return userService.regist(user);
    }

    // 5) 登录校验：/user/checkLogin
    @GetMapping("checkLogin")
    public Result checkLogin(@RequestHeader(required = false) String token) {
        if (StringUtils.isBlank(token) || jwtHelper.isExpiration(token)) {
            return Result.build(null, ResultCodeEnum.NOTLOGIN);
        }
        return Result.ok(null);
    }
}
