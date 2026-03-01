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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    @Autowired
    private JwtHelper jwtHelper;
    @Autowired
    private UserMapper userMapper;

    @Override
    public Result login(User user) {
        if (user == null || StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getUserPwd())) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR);
        }
        String username = user.getUsername().trim();

        // 1) 账号查询
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, username);
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
        if (StringUtils.isBlank(token) || jwtHelper.isExpiration(token)) {
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
        maskPassword(user);

        Map<String, Object> data = new HashMap<>();
        data.put("loginUser", user);
        return Result.ok(data);
    }

    @Override
    public Result checkUserName(String username) {
        if (StringUtils.isBlank(username)) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR);
        }

        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, username.trim());
        Long count = userMapper.selectCount(qw);

        if (count != null && count > 0) {
            return Result.build(null, ResultCodeEnum.USERNAME_USED);
        }

        return Result.ok(null);
    }

    @Override
    public Result regist(User user) {
        if (user == null || StringUtils.isBlank(user.getUsername()) || StringUtils.isBlank(user.getUserPwd())) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR);
        }
        String username = user.getUsername().trim();
        String rawPassword = user.getUserPwd().trim();
        if (StringUtils.isBlank(username) || StringUtils.isBlank(rawPassword)) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR);
        }

        // 1) 用户名占用校验
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, username);
        Long count = userMapper.selectCount(qw);
        if (count != null && count > 0) {
            return Result.build(null, ResultCodeEnum.USERNAME_USED);
        }

        String nickName = StringUtils.isBlank(user.getNickName()) ? username : user.getNickName().trim();

        // 2) 密码加密
        user.setUsername(username);
        user.setNickName(nickName);
        user.setUserPwd(MD5Util.encrypt(rawPassword));

        // 3) 入库
        userMapper.insert(user);

        return Result.ok(null);
    }

    @Override
    public Result managePage(Integer pageNum, Integer pageSize, String keyword) {
        Page<User> page = new Page<>(safePageNum(pageNum), safePageSize(pageSize));
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.trim();
            qw.and(wrapper -> wrapper.like(User::getUsername, kw).or().like(User::getNickName, kw));
        }
        qw.orderByDesc(User::getUid);

        Page<User> result = userMapper.selectPage(page, qw);
        List<User> records = result.getRecords();
        for (User item : records) {
            maskPassword(item);
        }

        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("pageData", records);
        pageInfo.put("pageNum", result.getCurrent());
        pageInfo.put("pageSize", result.getSize());
        pageInfo.put("totalPage", result.getPages());
        pageInfo.put("totalSize", result.getTotal());

        Map<String, Object> data = new HashMap<>();
        data.put("pageInfo", pageInfo);
        return Result.ok(data);
    }

    @Override
    public Result manageDetail(Integer uid) {
        if (uid == null || uid < 1) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR);
        }

        User user = userMapper.selectById(uid);
        if (user == null) {
            return Result.build(null, ResultCodeEnum.DATA_NOT_FOUND);
        }

        maskPassword(user);

        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        return Result.ok(data);
    }

    @Override
    public Result manageUpdate(Integer uid, User user) {
        if (uid == null || uid < 1 || user == null) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR);
        }
        if (user.getUsername() != null && StringUtils.isBlank(user.getUsername())) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR);
        }
        if (user.getUserPwd() != null && StringUtils.isBlank(user.getUserPwd())) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR);
        }

        User dbUser = userMapper.selectById(uid);
        if (dbUser == null) {
            return Result.build(null, ResultCodeEnum.DATA_NOT_FOUND);
        }

        boolean changed = false;
        if (StringUtils.isNotBlank(user.getUsername())) {
            String username = user.getUsername().trim();
            if (!username.equals(dbUser.getUsername()) && existsByUsername(username, uid)) {
                return Result.build(null, ResultCodeEnum.USERNAME_USED);
            }
            dbUser.setUsername(username);
            changed = true;
        }
        if (user.getNickName() != null) {
            String nickName = user.getNickName().trim();
            dbUser.setNickName(StringUtils.isBlank(nickName) ? null : nickName);
            changed = true;
        }
        if (StringUtils.isNotBlank(user.getUserPwd())) {
            dbUser.setUserPwd(MD5Util.encrypt(user.getUserPwd().trim()));
            changed = true;
        }

        if (!changed) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR);
        }

        int rows = userMapper.updateById(dbUser);
        if (rows <= 0) {
            return Result.build(null, ResultCodeEnum.OPERATE_FAIL);
        }

        maskPassword(dbUser);

        Map<String, Object> data = new HashMap<>();
        data.put("user", dbUser);
        return Result.ok(data);
    }

    @Override
    public Result manageDelete(Integer uid) {
        if (uid == null || uid < 1) {
            return Result.build(null, ResultCodeEnum.PARAM_ERROR);
        }

        int rows = userMapper.deleteById(uid);
        if (rows <= 0) {
            return Result.build(null, ResultCodeEnum.DATA_NOT_FOUND);
        }

        return Result.ok(null);
    }

    private int safePageNum(Integer pageNum) {
        if (pageNum == null || pageNum < 1) {
            return DEFAULT_PAGE_NUM;
        }
        return pageNum;
    }

    private int safePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private boolean existsByUsername(String username, Integer excludeUid) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        qw.eq(User::getUsername, username);
        if (excludeUid != null) {
            qw.ne(User::getUid, excludeUid);
        }
        Long count = userMapper.selectCount(qw);
        return count != null && count > 0;
    }

    private void maskPassword(User user) {
        if (user != null) {
            user.setUserPwd("");
        }
    }
}
