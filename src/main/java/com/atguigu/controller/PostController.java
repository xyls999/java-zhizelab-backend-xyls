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
    public Result publish(@ModelAttribute PostPublishRequest request,
                          @RequestHeader(value = "token", required = false) String token,
                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        return postService.publish(requireUserId(token, authorization), request);
    }

    @GetMapping("public")
    public Result publicPage(@RequestParam(defaultValue = "1") Integer pageNum,
                             @RequestParam(defaultValue = "10") Integer pageSize,
                             @RequestHeader(value = "token", required = false) String token,
                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        return postService.publicPage(optionalUserId(token, authorization), pageNum, pageSize);
    }

    @GetMapping("my")
    public Result myPage(@RequestParam(defaultValue = "1") Integer pageNum,
                         @RequestParam(defaultValue = "10") Integer pageSize,
                         @RequestHeader(value = "token", required = false) String token,
                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        return postService.myPage(requireUserId(token, authorization), pageNum, pageSize);
    }

    @GetMapping("{postId}")
    public Result postDetail(@PathVariable Long postId,
                             @RequestHeader(value = "token", required = false) String token,
                             @RequestHeader(value = "Authorization", required = false) String authorization) {
        return postService.postDetail(postId, optionalUserId(token, authorization));
    }

    @PostMapping("{postId}/like")
    public Result like(@PathVariable Long postId,
                       @RequestHeader(value = "token", required = false) String token,
                       @RequestHeader(value = "Authorization", required = false) String authorization) {
        return postService.like(postId, requireUserId(token, authorization));
    }

    @PostMapping("{postId}/unlike")
    public Result unlike(@PathVariable Long postId,
                         @RequestHeader(value = "token", required = false) String token,
                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        return postService.unlike(postId, requireUserId(token, authorization));
    }

    @PostMapping("{postId}/reply")
    public Result reply(@PathVariable Long postId,
                        @RequestBody PostReplyRequest request,
                        @RequestHeader(value = "token", required = false) String token,
                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        return postService.reply(postId, requireUserId(token, authorization), request == null ? null : request.getContent());
    }

    private Integer requireUserId(String token, String authorization) {
        String realToken = jwtHelper.resolveToken(token, authorization);
        if (!StringUtils.hasText(realToken) || jwtHelper.isExpiration(realToken)) {
            return null;
        }
        Long userId = jwtHelper.getUserId(realToken);
        return userId == null ? null : userId.intValue();
    }

    private Integer optionalUserId(String token, String authorization) {
        String realToken = jwtHelper.resolveToken(token, authorization);
        if (!StringUtils.hasText(realToken) || jwtHelper.isExpiration(realToken)) {
            return null;
        }
        Long userId = jwtHelper.getUserId(realToken);
        return userId == null ? null : userId.intValue();
    }

}
