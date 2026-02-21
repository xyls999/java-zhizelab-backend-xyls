package com.atguigu.service;

import com.atguigu.result.Result;

public interface ILlmService {

    Result status();

    Result chat(String prompt);
}
