package com.atguigu.service.impl;

import com.atguigu.mapper.PostLikeMapper;
import com.atguigu.mapper.PostMapper;
import com.atguigu.mapper.PostReplyMapper;
import com.atguigu.pojo.Post;
import com.atguigu.pojo.PostLike;
import com.atguigu.pojo.PostReply;
import com.atguigu.result.Result;
import com.atguigu.result.ResultCodeEnum;
import com.atguigu.service.IPostService;
import com.atguigu.vo.PostPublishRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl extends ServiceImpl<PostMapper, Post> implements IPostService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final long MAX_CODE_FILE_SIZE = 1024 * 1024;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    @Autowired
    private PostMapper postMapper;

    @Autowired
    private PostLikeMapper postLikeMapper;

    @Autowired
    private PostReplyMapper postReplyMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result publish(Integer userId, PostPublishRequest request) {
        if (request == null) {
            return paramError("request is null");
        }

        try {
            String textContent = normalize(request.getTextContent());
            String imageUrl = resolveImageUrl(request);
            String codeContent = resolveCodeContent(request);
            String codeLanguage = resolveCodeLanguage(request);

            if (!StringUtils.hasText(textContent)
                    && !StringUtils.hasText(imageUrl)
                    && !StringUtils.hasText(codeContent)) {
                return paramError("text/image/code must exist at least one");
            }

            Post post = new Post();
            post.setUserId(userId);
            post.setTextContent(textContent);
            post.setImageUrl(imageUrl);
            post.setCodeLanguage(codeLanguage);
            post.setCodeContent(codeContent);
            post.setLikeCount(0);
            post.setCommentCount(0);
            post.setIsDeleted(0);
            post.setCreateTime(new Date());
            post.setUpdateTime(new Date());

            postMapper.insert(post);

            Map<String, Object> data = new HashMap<>();
            data.put("postId", post.getId());
            return Result.ok(data);
        } catch (IllegalArgumentException e) {
            return paramError(e.getMessage());
        } catch (Exception e) {
            return operateFail("publish failed: " + e.getMessage());
        }
    }

    @Override
    public Result publicPage(Integer userId, Integer pageNum, Integer pageSize) {
        Page<Map<String, Object>> page = new Page<>(safePageNum(pageNum), safePageSize(pageSize));
        IPage<Map<String, Object>> pageResult = postMapper.selectPublicPage(page);
        List<Map<String, Object>> records = pageResult.getRecords();
        enrichLikedFlag(records, userId);
        return Result.ok(buildPageData(pageResult, records));
    }

    @Override
    public Result myPage(Integer userId, Integer pageNum, Integer pageSize) {
        Page<Map<String, Object>> page = new Page<>(safePageNum(pageNum), safePageSize(pageSize));
        IPage<Map<String, Object>> pageResult = postMapper.selectMyPage(page, userId);
        List<Map<String, Object>> records = pageResult.getRecords();
        enrichLikedFlag(records, userId);
        return Result.ok(buildPageData(pageResult, records));
    }

    @Override
    public Result postDetail(Long postId, Integer userId) {
        Map<String, Object> detail = postMapper.selectDetailMap(postId);
        if (detail == null || detail.isEmpty()) {
            return Result.build(null, ResultCodeEnum.DATA_NOT_FOUND);
        }

        boolean liked = false;
        if (userId != null) {
            LambdaQueryWrapper<PostLike> likeQuery = new LambdaQueryWrapper<>();
            likeQuery.eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId);
            liked = postLikeMapper.selectCount(likeQuery) > 0;
        }

        List<Map<String, Object>> replies = postReplyMapper.selectReplyList(postId);

        Map<String, Object> data = new HashMap<>();
        data.put("post", detail);
        data.put("liked", liked);
        data.put("replies", replies);
        return Result.ok(data);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result like(Long postId, Integer userId) {
        if (!existsPost(postId)) {
            return Result.build(null, ResultCodeEnum.DATA_NOT_FOUND);
        }

        LambdaQueryWrapper<PostLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId);
        if (postLikeMapper.selectCount(queryWrapper) > 0) {
            return Result.ok(null);
        }

        PostLike postLike = new PostLike();
        postLike.setPostId(postId);
        postLike.setUserId(userId);
        postLike.setCreateTime(new Date());

        try {
            postLikeMapper.insert(postLike);
        } catch (DuplicateKeyException ignored) {
            return Result.ok(null);
        }

        UpdateWrapper<Post> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", postId).setSql("like_count = like_count + 1");
        postMapper.update(null, updateWrapper);
        return Result.ok(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result unlike(Long postId, Integer userId) {
        if (!existsPost(postId)) {
            return Result.build(null, ResultCodeEnum.DATA_NOT_FOUND);
        }

        LambdaQueryWrapper<PostLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(PostLike::getPostId, postId).eq(PostLike::getUserId, userId);
        int deleted = postLikeMapper.delete(queryWrapper);
        if (deleted > 0) {
            UpdateWrapper<Post> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("id", postId).setSql("like_count = if(like_count > 0, like_count - 1, 0)");
            postMapper.update(null, updateWrapper);
        }

        return Result.ok(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result reply(Long postId, Integer userId, String content) {
        if (!existsPost(postId)) {
            return Result.build(null, ResultCodeEnum.DATA_NOT_FOUND);
        }
        if (!StringUtils.hasText(content)) {
            return paramError("reply content is empty");
        }

        PostReply postReply = new PostReply();
        postReply.setPostId(postId);
        postReply.setUserId(userId);
        postReply.setContent(content.trim());
        postReply.setCreateTime(new Date());
        postReply.setIsDeleted(0);

        postReplyMapper.insert(postReply);

        UpdateWrapper<Post> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", postId).setSql("comment_count = comment_count + 1");
        postMapper.update(null, updateWrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("replyId", postReply.getId());
        return Result.ok(data);
    }

    private boolean existsPost(Long postId) {
        return postId != null && postMapper.selectById(postId) != null;
    }

    private Map<String, Object> buildPageData(IPage<Map<String, Object>> pageResult, List<Map<String, Object>> records) {
        Map<String, Object> pageInfo = new HashMap<>();
        pageInfo.put("pageData", records);
        pageInfo.put("pageNum", pageResult.getCurrent());
        pageInfo.put("pageSize", pageResult.getSize());
        pageInfo.put("totalPage", pageResult.getPages());
        pageInfo.put("totalSize", pageResult.getTotal());

        Map<String, Object> data = new HashMap<>();
        data.put("pageInfo", pageInfo);
        return data;
    }

    private void enrichLikedFlag(List<Map<String, Object>> records, Integer userId) {
        if (records == null || records.isEmpty()) {
            return;
        }

        Set<Long> likedPostIds = Collections.emptySet();
        if (userId != null) {
            List<Long> postIds = records.stream()
                    .map(record -> toLong(record.get("id")))
                    .filter(id -> id != null)
                    .collect(Collectors.toList());

            if (!postIds.isEmpty()) {
                LambdaQueryWrapper<PostLike> likeQuery = new LambdaQueryWrapper<>();
                likeQuery.eq(PostLike::getUserId, userId).in(PostLike::getPostId, postIds);
                likedPostIds = postLikeMapper.selectList(likeQuery).stream()
                        .map(PostLike::getPostId)
                        .collect(Collectors.toSet());
            }
        }

        for (Map<String, Object> record : records) {
            Long postId = toLong(record.get("id"));
            record.put("liked", postId != null && likedPostIds.contains(postId));
        }
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
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

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String resolveImageUrl(PostPublishRequest request) throws IOException {
        MultipartFile imageFile = request.getImageFile();
        if (imageFile == null || imageFile.isEmpty()) {
            return normalize(request.getImageUrl());
        }

        String contentType = imageFile.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("imageFile must be image/*");
        }

        String originalFilename = imageFile.getOriginalFilename();
        String ext = "";
        if (StringUtils.hasText(originalFilename) && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }

        String dateFolder = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Path folder = Paths.get(uploadDir, "images", dateFolder).toAbsolutePath().normalize();
        Files.createDirectories(folder);

        String targetFileName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path targetPath = folder.resolve(targetFileName);
        imageFile.transferTo(targetPath.toFile());

        return "/uploads/images/" + dateFolder + "/" + targetFileName;
    }

    private String resolveCodeContent(PostPublishRequest request) throws IOException {
        String codeContent = normalize(request.getCodeContent());
        if (StringUtils.hasText(codeContent)) {
            return codeContent;
        }

        MultipartFile codeFile = request.getCodeFile();
        if (codeFile == null || codeFile.isEmpty()) {
            return null;
        }
        if (codeFile.getSize() > MAX_CODE_FILE_SIZE) {
            throw new IllegalArgumentException("codeFile exceeds 1MB");
        }

        return new String(codeFile.getBytes(), StandardCharsets.UTF_8);
    }

    private String resolveCodeLanguage(PostPublishRequest request) {
        String codeLanguage = normalize(request.getCodeLanguage());
        if (StringUtils.hasText(codeLanguage)) {
            return codeLanguage;
        }

        MultipartFile codeFile = request.getCodeFile();
        if (codeFile == null || codeFile.isEmpty()) {
            return null;
        }

        String filename = codeFile.getOriginalFilename();
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "text";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private Result paramError(String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("reason", reason);
        return Result.build(data, ResultCodeEnum.PARAM_ERROR);
    }

    private Result operateFail(String reason) {
        Map<String, Object> data = new HashMap<>();
        data.put("reason", reason);
        return Result.build(data, ResultCodeEnum.OPERATE_FAIL);
    }
}
