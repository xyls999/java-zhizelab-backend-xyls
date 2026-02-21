package com.atguigu.result;
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(ResultCodeEnum.SUCCESS.getCode());
        r.setMessage(ResultCodeEnum.SUCCESS.getMessage());
        r.setData(data);
        return r;
    }

    public static <T> Result<T> build(T data, ResultCodeEnum codeEnum) {
        Result<T> r = new Result<>();
        r.setCode(codeEnum.getCode());
        r.setMessage(codeEnum.getMessage());
        r.setData(data);
        return r;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
