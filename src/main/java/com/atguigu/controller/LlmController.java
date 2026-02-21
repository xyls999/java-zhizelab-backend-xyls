package com.atguigu.controller;

import com.atguigu.result.Result;
import com.atguigu.service.ILlmService;
import com.atguigu.vo.LlmChatRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("llm")
@CrossOrigin
public class LlmController {

    @Autowired
    private ILlmService llmService;

    @GetMapping("status")
    public Result status() {
        return llmService.status();
    }

    @PostMapping("chat")
    public Result chat(@RequestBody LlmChatRequest request) {
        return llmService.chat(request == null ? null : request.getPrompt());
    }
}
