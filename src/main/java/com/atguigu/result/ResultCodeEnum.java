package com.atguigu.result;
public enum ResultCodeEnum {

    SUCCESS(200, "success"),
    USERNAME_ERROR(501, "用户名有误"),
    PASSWORD_ERROR(503, "密码有误"),
    NOTLOGIN(504, "notLogin"),
    USERNAME_USED(505, "用户名占用"),
    PARAM_ERROR(506, "参数错误"),
    OPERATE_FAIL(507, "操作失败"),
    DATA_NOT_FOUND(508, "数据不存在"),
    LLM_NOT_CONFIGURED(509, "大模型配置缺失"),
    LLM_ERROR(510, "大模型调用失败");

    private final Integer code;
    private final String message;

    ResultCodeEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
