# 后端接口文档

## 1. 通用说明

- 基础地址：`http://{host}:8080`
- 统一返回结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

- 鉴权方式：请求头 `token: <JWT>`
- 需要登录的接口：
  - `/post/publish`
  - `/post/my`
  - `/post/{postId}/like`
  - `/post/{postId}/unlike`
  - `/post/{postId}/reply`
  - `/llm/status`
  - `/llm/chat`

## 2. 状态码

- `200` success
- `501` 用户名有误
- `503` 密码有误
- `504` notLogin
- `505` 用户名占用
- `506` 参数错误
- `507` 操作失败
- `508` 数据不存在
- `509` 大模型配置缺失
- `510` 大模型调用失败

## 3. 用户模块 `user`

### 3.1 登录

- 方法：`POST /user/login`
- Body(JSON)：

```json
{
  "username": "test1",
  "userPwd": "123456"
}
```

- 成功返回 `data`：

```json
{
  "token": "xxx.yyy.zzz"
}
```

### 3.2 获取当前用户信息

- 方法：`GET /user/getUserInfo`
- Header：`token`
- 成功返回 `data`：

```json
{
  "loginUser": {
    "uid": 1,
    "username": "test1",
    "userPwd": "",
    "nickName": "测试用户"
  }
}
```

### 3.3 检查用户名是否可注册

- 方法：`POST /user/checkUserName?username=test1`
- 成功：`code=200`
- 已占用：`code=505`

### 3.4 注册

- 方法：`POST /user/regist`
- Body(JSON)：

```json
{
  "username": "test2",
  "userPwd": "123456",
  "nickName": "新用户"
}
```

### 3.5 校验登录状态

- 方法：`GET /user/checkLogin`
- Header：`token`
- 已登录：`code=200`
- 未登录/过期：`code=504`

## 4. 动态模块 `post`

### 4.1 发布动态（文字/图片/代码）

- 方法：`POST /post/publish`
- Header：`token`
- Content-Type：`multipart/form-data`
- 表单字段：
  - `textContent`：文字（可空）
  - `imageUrl`：图片 URL（可空，和 `imageFile` 二选一）
  - `imageFile`：图片文件（可空，需 `image/*`）
  - `codeLanguage`：代码语言（可空）
  - `codeContent`：代码文本（可空）
  - `codeFile`：代码文件（可空，<=1MB，UTF-8）
- 约束：`textContent/imageUrl(imageFile)/codeContent(codeFile)` 至少提供一个。
- 成功返回 `data`：

```json
{
  "postId": 1001
}
```

### 4.2 公共动态分页

- 方法：`GET /post/public?pageNum=1&pageSize=10`
- Header：`token` 可选（传了会返回每条是否已点赞）
- 成功返回 `data.pageInfo`：

```json
{
  "pageData": [
    {
      "id": 1001,
      "userId": 1,
      "username": "test1",
      "nickName": "测试用户",
      "textContent": "hello",
      "imageUrl": "/uploads/images/20260220/a.png",
      "codeLanguage": "java",
      "codeContent": "System.out.println(1);",
      "likeCount": 2,
      "commentCount": 1,
      "createTime": "2026-02-20T15:00:00.000+00:00",
      "updateTime": "2026-02-20T15:00:00.000+00:00",
      "liked": true
    }
  ],
  "pageNum": 1,
  "pageSize": 10,
  "totalPage": 1,
  "totalSize": 1
}
```

### 4.3 我的动态分页

- 方法：`GET /post/my?pageNum=1&pageSize=10`
- Header：`token`
- 返回结构同 4.2，仅数据范围是当前登录用户。

### 4.4 动态详情

- 方法：`GET /post/{postId}`
- Header：`token` 可选
- 成功返回 `data`：

```json
{
  "post": {
    "id": 1001,
    "userId": 1,
    "username": "test1",
    "nickName": "测试用户",
    "textContent": "hello",
    "imageUrl": null,
    "codeLanguage": "java",
    "codeContent": "System.out.println(1);",
    "likeCount": 2,
    "commentCount": 1,
    "createTime": "2026-02-20T15:00:00.000+00:00",
    "updateTime": "2026-02-20T15:00:00.000+00:00"
  },
  "liked": true,
  "replies": [
    {
      "id": 5001,
      "postId": 1001,
      "userId": 2,
      "username": "test2",
      "nickName": "回复用户",
      "content": "写得不错",
      "createTime": "2026-02-20T16:00:00.000+00:00"
    }
  ]
}
```

### 4.5 点赞

- 方法：`POST /post/{postId}/like`
- Header：`token`
- 成功：`code=200`

### 4.6 取消点赞

- 方法：`POST /post/{postId}/unlike`
- Header：`token`
- 成功：`code=200`

### 4.7 回复动态

- 方法：`POST /post/{postId}/reply`
- Header：`token`
- Body(JSON)：

```json
{
  "content": "回复内容"
}
```

- 成功返回 `data`：

```json
{
  "replyId": 5002
}
```

## 5. 大模型模块 `llm`

> 当前代码里 `llm` 接口同样受登录拦截，必须带 `token`。

### 5.1 大模型配置状态

- 方法：`GET /llm/status`
- Header：`token`
- 成功返回 `data`：

```json
{
  "enabled": true,
  "baseUrl": "https://api.openai.com/v1/chat/completions",
  "model": "gpt-4o-mini",
  "apiKeyMasked": "sk-1****abcd"
}
```

### 5.2 大模型对话

- 方法：`POST /llm/chat`
- Header：`token`
- Body(JSON)：

```json
{
  "prompt": "给我一段Java快速排序"
}
```

- 成功返回 `data`：

```json
{
  "answer": "...模型回复...",
  "model": "gpt-4o-mini"
}
```

## 6. 示例 cURL

```bash
# 登录
curl -X POST http://127.0.0.1:8080/user/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"test1\",\"userPwd\":\"123456\"}"

# 发布动态（文字+代码）
curl -X POST http://127.0.0.1:8080/post/publish \
  -H "token: YOUR_TOKEN" \
  -F "textContent=今天提交了新功能" \
  -F "codeLanguage=java" \
  -F "codeContent=System.out.println(\"hello\");"

# 公共动态
curl "http://127.0.0.1:8080/post/public?pageNum=1&pageSize=10"

# 点赞
curl -X POST http://127.0.0.1:8080/post/1001/like -H "token: YOUR_TOKEN"

# 回复
curl -X POST http://127.0.0.1:8080/post/1001/reply \
  -H "token: YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"content\":\"支持一下\"}"

# 大模型调用
curl -X POST http://127.0.0.1:8080/llm/chat \
  -H "token: YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"prompt\":\"写一个Spring Boot Controller示例\"}"
```
