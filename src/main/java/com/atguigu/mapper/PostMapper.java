package com.atguigu.mapper;

import com.atguigu.pojo.Post;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

public interface PostMapper extends BaseMapper<Post> {

    IPage<Map<String, Object>> selectPublicPage(IPage<Map<String, Object>> page);

    IPage<Map<String, Object>> selectMyPage(IPage<Map<String, Object>> page, @Param("userId") Integer userId);

    Map<String, Object> selectDetailMap(@Param("postId") Long postId);
}
