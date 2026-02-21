package com.atguigu.service;

import com.atguigu.pojo.Post;
import com.atguigu.result.Result;
import com.atguigu.vo.PostPublishRequest;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IPostService extends IService<Post> {

    Result publish(Integer userId, PostPublishRequest request);

    Result publicPage(Integer userId, Integer pageNum, Integer pageSize);

    Result myPage(Integer userId, Integer pageNum, Integer pageSize);

    Result postDetail(Long postId, Integer userId);

    Result like(Long postId, Integer userId);

    Result unlike(Long postId, Integer userId);

    Result reply(Long postId, Integer userId, String content);
}
