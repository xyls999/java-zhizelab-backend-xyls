# 后端功能实现原理说明

本文从代码实现角度说明当前项目主要能力，包括登录、分页查询、社交动态、图片存储与访问、统一返回结构和前端接口对齐方式。

## 1. 整体架构

项目采用经典分层结构：

1. `controller`：接收 HTTP 请求，做参数接入和路由分发。  
2. `service`：核心业务逻辑（登录校验、发布动态、点赞、回复、分页封装等）。  
3. `mapper` + `mapper/*.xml`：数据库访问（MyBatis-Plus + 自定义 SQL）。  
4. `pojo/vo`：实体对象与请求对象。  
5. `config/interceptor/utils`：拦截器、资源映射、JWT、MD5 工具等。

核心入口文件：

- `src/main/java/com/atguigu/Main.java`
- `src/main/java/com/atguigu/config/WebMvcConfig.java`
- `src/main/java/com/atguigu/config/DatabaseSchemaInitializer.java`

## 2. 统一返回结构

统一响应由 `Result<T>` 定义：

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

实现位置：

- `src/main/java/com/atguigu/result/Result.java`
- `src/main/java/com/atguigu/result/ResultCodeEnum.java`

常用状态码：

1. `200` 成功  
2. `504` 未登录或 token 失效  
3. `505` 用户名占用  
4. `506` 参数错误  
5. `507` 操作失败  
6. `508` 数据不存在

## 3. 登录实现原理

### 3.1 登录流程

接口：`POST /user/login`  
实现：`UserController.login -> UserServiceImpl.login`

步骤：

1. 校验用户名和密码是否为空。  
2. 按 `username` 查询 `news_user`。  
3. 用 `MD5Util.encrypt(明文密码)` 与库中 `user_pwd` 比较。  
4. 成功后用 `JwtHelper.createToken(uid)` 生成 JWT，返回 `data.token`。

对应代码：

- `src/main/java/com/atguigu/service/impl/UserServiceImpl.java`
- `src/main/java/com/atguigu/utils/MD5Util.java`
- `src/main/java/com/atguigu/utils/JwtHelper.java`

### 3.2 token 解析与校验

`JwtHelper` 做了三件事：

1. 生成 token（带 `userId` claim 和过期时间）。  
2. 解析 token 获取 `userId`。  
3. 判断 token 是否过期/无效。

并且兼容两种请求头：

1. `token: <JWT>`  
2. `Authorization: Bearer <JWT>`

对应方法：

- `resolveToken(tokenHeader, authorizationHeader)`
- `normalizeToken(rawToken)`

### 3.3 拦截器保护接口

`LoginProtectInterceptor` 对以下接口拦截：

1. `/post/publish`  
2. `/post/my`  
3. `/post/*/like`  
4. `/post/*/unlike`  
5. `/post/*/reply`

逻辑：

1. 从 `token` 或 `Authorization` 解析 JWT。  
2. 缺失或过期返回 `code=504`。  
3. 否则放行。

对应代码：

- `src/main/java/com/atguigu/interceptor/LoginProtectInterceptor.java`
- `src/main/java/com/atguigu/config/WebMvcConfig.java`

## 4. 用户管理与分页查询

### 4.1 用户管理接口

用户管理集中在 `/user/manage/*`：

1. 用户名检查：`POST /user/manage/checkUserName`  
2. 新增用户（注册）：`POST /user/manage/register`  
3. 分页查询：`GET /user/manage/page`  
4. 详情：`GET /user/manage/{uid}`  
5. 更新：`PUT /user/manage/{uid}`  
6. 删除：`DELETE /user/manage/{uid}`

实现文件：

- `src/main/java/com/atguigu/controller/UserController.java`
- `src/main/java/com/atguigu/service/impl/UserServiceImpl.java`

### 4.2 用户分页实现

使用 MyBatis-Plus `Page<T>`：

1. 入参 `pageNum/pageSize/keyword`。  
2. `safePageNum/safePageSize` 保证边界（默认 1/10，最大 50）。  
3. `keyword` 按 `username/nickName` 模糊查询。  
4. 返回结构统一封装为 `data.pageInfo`。

返回结构示例：

```json
{
  "pageInfo": {
    "pageData": [],
    "pageNum": 1,
    "pageSize": 10,
    "totalPage": 1,
    "totalSize": 0
  }
}
```

## 5. 社交平台与动态系统

### 5.1 功能清单

接口与能力对应：

1. `POST /post/publish`：发布动态（文字/图片/代码）。  
2. `GET /post/public`：公共动态分页（游客可看）。  
3. `GET /post/my`：我的动态分页。  
4. `GET /post/{postId}`：动态详情 + 回复列表 + 当前用户点赞状态。  
5. `POST /post/{postId}/like`：点赞。  
6. `POST /post/{postId}/unlike`：取消点赞。  
7. `POST /post/{postId}/reply`：回复。

实现文件：

- `src/main/java/com/atguigu/controller/PostController.java`
- `src/main/java/com/atguigu/service/impl/PostServiceImpl.java`
- `src/main/resources/mapper/PostMapper.xml`
- `src/main/resources/mapper/PostReplyMapper.xml`

### 5.2 发布动态实现细节

请求对象：`PostPublishRequest`，支持：

1. `textContent`  
2. `imageUrl` 或 `imageFile`  
3. `codeLanguage` + `codeContent` 或 `codeFile`

关键校验：

1. 文字、图片、代码三类内容至少一类存在。  
2. `imageFile` 必须 `image/*`。  
3. `codeFile` 最大 1MB，按 UTF-8 读取。

发布成功返回：

```json
{
  "postId": 123
}
```

### 5.3 点赞与回复实现细节

点赞：

1. 先检查动态是否存在。  
2. 防重复点赞（唯一索引 + 业务判断 + `DuplicateKeyException` 兜底）。  
3. 点赞成功 `like_count + 1`。

取消点赞：

1. 删除点赞关系。  
2. 成功后 `like_count` 最低降到 0，不会出现负数。

回复：

1. 校验内容非空。  
2. 写入 `social_post_reply`。  
3. `comment_count + 1`。  
4. 返回 `replyId`。

## 6. 图片存储与显示原理

### 6.1 存储

图片上传路径规则：

1. 根目录来自配置 `app.upload-dir`（默认 `uploads`）。  
2. 按日期分目录：`images/yyyyMMdd/`。  
3. 文件名使用 UUID，避免重名。

返回给前端的 `imageUrl` 形如：

`/uploads/images/20260301/xxxxxxxx.jpg`

实现位置：

- `PostServiceImpl.resolveImageUrl`

### 6.2 访问

资源映射在 `WebMvcConfig`：

1. `/uploads/** -> <uploadDir>`  
2. `/api/uploads/** -> <uploadDir>`

这样可以同时兼容：

1. 直连后端路径 `/uploads/...`  
2. 前端网关代理路径 `/api/uploads/...`

## 7. 数据库模型与自动建表

### 7.1 主要表

1. `news_user`：用户表  
2. `social_post`：动态主表  
3. `social_post_like`：点赞关系表（`post_id + user_id` 唯一）  
4. `social_post_reply`：回复表

SQL 脚本：

- `src/main/resources/sql/user_schema.sql`
- `src/main/resources/sql/social_schema.sql`

### 7.2 自动初始化

为避免缺表导致接口失败，项目有两层初始化：

1. `spring.sql.init`（dev/docker 配置中启用）  
2. `DatabaseSchemaInitializer` 启动时执行脚本（`app.schema-auto-init=true`）

即使某一层未触发，另一层也能兜底。

## 8. 分页查询对齐方式（前端最关心）

无论用户模块还是动态模块，分页都返回统一结构：

```json
{
  "pageInfo": {
    "pageData": [ ... ],
    "pageNum": 1,
    "pageSize": 10,
    "totalPage": 3,
    "totalSize": 25
  }
}
```

前端只要读取 `data.pageInfo.pageData` 即可渲染列表。

## 9. 社交接口返回字段对齐

### 9.1 动态列表字段

`/post/public`、`/post/my` 单条动态包含：

1. `id`  
2. `userId`  
3. `username`  
4. `nickName`  
5. `textContent`  
6. `imageUrl`  
7. `codeLanguage`  
8. `codeContent`  
9. `likeCount`  
10. `commentCount`  
11. `createTime`  
12. `updateTime`  
13. `liked`（根据当前用户动态计算）

### 9.2 动态详情字段

`/post/{postId}` 返回：

1. `post`：动态主体（同上核心字段）  
2. `liked`：当前用户是否点赞  
3. `replies`：回复列表

回复项字段：

1. `id`  
2. `postId`  
3. `userId`  
4. `username`  
5. `nickName`  
6. `content`  
7. `createTime`

## 10. 与你当前前端请求的对齐结论

你的前端组件（登录、广场、发布、聊天）与后端当前实现已对齐要点：

1. 支持 `token` 与 `Authorization: Bearer` 两种 header。  
2. 广场接口游客可读，登录后可拿 `liked` 精准状态。  
3. 发布接口支持 `multipart/form-data`，并返回 `postId`。  
4. 图片 URL 可走 `/uploads/...` 或 `/api/uploads/...`。  
5. 聊天消息走 `/post/{postId}/reply`，详情接口返回 `replies` 用于渲染会话。

如果出现“发布失败”，优先检查三件事：

1. 数据库表是否存在（尤其 `social_post`）。  
2. 是否带了有效 token（发布/点赞/回复/我的动态必须登录）。  
3. 图片文件是否为 `image/*` 且上传目录具备写权限。

---

如需，我可以再补一版《时序图版》文档（登录时序、发布动态时序、拉取详情与回复时序），方便你发给前端同学联调。
