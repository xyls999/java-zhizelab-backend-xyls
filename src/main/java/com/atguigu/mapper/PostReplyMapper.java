package com.atguigu.mapper;

import com.atguigu.pojo.PostReply;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface PostReplyMapper extends BaseMapper<PostReply> {

    List<Map<String, Object>> selectReplyList(@Param("postId") Long postId);
}
