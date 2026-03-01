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
    public Result userInfo(@RequestHeader(value = "token", required = false) String token,
                           @RequestHeader(value = "Authorization", required = false) String authorization) {
        return userService.getUserInfo(jwtHelper.resolveToken(token, authorization));
    }

    // 3) 用户管理内用户名检查（需登录）：/user/manage/checkUserName（param）
    @PostMapping("manage/checkUserName")
    public Result manageCheckUserName(@RequestParam String username) {
        return userService.checkUserName(username);
    }

    // 4) 用户管理内新增用户（需登录）：/user/manage/register
    @PostMapping("manage/register")
    public Result manageRegister(@RequestBody User user) {
        return userService.regist(user);
    }

    // 5) 登录校验：/user/checkLogin
    @GetMapping("checkLogin")
    public Result checkLogin(@RequestHeader(value = "token", required = false) String token,
                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        String realToken = jwtHelper.resolveToken(token, authorization);
        if (StringUtils.isBlank(realToken) || jwtHelper.isExpiration(realToken)) {
            return Result.build(null, ResultCodeEnum.NOTLOGIN);
        }
        return Result.ok(null);
    }

    // 6) 用户管理分页：/user/manage/page
    @GetMapping("manage/page")
    public Result managePage(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestParam(required = false) String keyword) {
        return userService.managePage(pageNum, pageSize, keyword);
    }

    // 7) 用户管理详情：/user/manage/{uid}
    @GetMapping("manage/{uid}")
    public Result manageDetail(@PathVariable Integer uid) {
        return userService.manageDetail(uid);
    }

    // 8) 用户管理更新：/user/manage/{uid}
    @PutMapping("manage/{uid}")
    public Result manageUpdate(@PathVariable Integer uid, @RequestBody User user) {
        return userService.manageUpdate(uid, user);
    }

    // 9) 用户管理删除：/user/manage/{uid}
    @DeleteMapping("manage/{uid}")
    public Result manageDelete(@PathVariable Integer uid) {
        return userService.manageDelete(uid);
    }
}
