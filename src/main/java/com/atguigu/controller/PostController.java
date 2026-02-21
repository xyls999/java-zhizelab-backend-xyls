package com.atguigu.controller;

import com.atguigu.result.Result;
import com.atguigu.service.IPostService;
import com.atguigu.utils.JwtHelper;
import com.atguigu.vo.PostPublishRequest;
import com.atguigu.vo.PostReplyRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("post")
@CrossOrigin
public class PostController {

    @Autowired
    private IPostService postService;

    @Autowired
    private JwtHelper jwtHelper;

    @PostMapping(value = "publish", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result publish(@ModelAttribute PostPublishRequest request, @RequestHeader String token) {
        return postService.publish(requireUserId(token), request);
    }

    @GetMapping("public")
    public Result publicPage(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestHeader(required = false) String token) {
        return postService.publicPage(optionalUserId(token), pageNum, pageSize);
    }

    @GetMapping("my")
    public Result myPage(@RequestParam(defaultValue = "1") Integer pageNum,
                         @RequestParam(defaultValue = "10") Integer pageSize,
                         @RequestHeader String token) {
        return postService.myPage(requireUserId(token), pageNum, pageSize);
    }

    @GetMapping("{postId}")
    public Result postDetail(@PathVariable Long postId, @RequestHeader(required = false) String token) {
        return postService.postDetail(postId, optionalUserId(token));
    }

    @PostMapping("{postId}/like")
    public Result like(@PathVariable Long postId, @RequestHeader String token) {
        return postService.like(postId, requireUserId(token));
    }

    @PostMapping("{postId}/unlike")
    public Result unlike(@PathVariable Long postId, @RequestHeader String token) {
        return postService.unlike(postId, requireUserId(token));
    }

    @PostMapping("{postId}/reply")
    public Result reply(@PathVariable Long postId, @RequestBody PostReplyRequest request, @RequestHeader String token) {
        return postService.reply(postId, requireUserId(token), request == null ? null : request.getContent());
    }

    private Integer requireUserId(String token) {
        Long userId = jwtHelper.getUserId(token);
        return userId == null ? null : userId.intValue();
    }

    private Integer optionalUserId(String token) {
        if (!StringUtils.hasText(token) || jwtHelper.isExpiration(token)) {
            return null;
        }
        Long userId = jwtHelper.getUserId(token);
        return userId == null ? null : userId.intValue();
    }
}
