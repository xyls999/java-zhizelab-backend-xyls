# 部署文档（Java + Docker）

## 1. 要不要 Docker 编排？

建议用：`Dockerfile + docker-compose.yml`。  
这是当前项目最实用的方案：

- `Dockerfile` 负责把 Spring Boot 项目打成可运行镜像。
- `docker-compose.yml` 负责编排 `app + mysql + volume + 环境变量`。
- 单机服务器（1 台 ECS/VPS）用 compose 就够了。
- 多机高可用再上 Kubernetes，不是当前必须项。

## 2. 服务器准备

- 操作系统：Ubuntu/CentOS 均可
- 安装 Docker 与 Docker Compose 插件
- 开放端口：
  - `8080`（后端）
  - `3306`（若要外部直连 MySQL）

## 3. 部署文件

项目已提供：

- `Dockerfile`
- `docker-compose.yml`
- `src/main/resources/application-docker.yaml`

## 4. 首次部署步骤

### 4.1 拉代码

```bash
git clone <你的仓库地址>
cd com.atguigu
```

### 4.2 配置环境变量（推荐）

在项目根目录创建 `.env`（compose 会自动读取）：

```env
MYSQL_ROOT_PASSWORD=请改成强密码
MYSQL_DB=sm_db
JWT_TOKEN_SIGN_KEY=请改成JWT密钥
JWT_TOKEN_EXPIRATION=120
```

### 4.3 启动容器

```bash
docker compose up -d --build
```

### 4.4 初始化数据库

`social_post` 等表依赖 `news_user`。  
如果你是全新数据库，先执行用户表脚本，再执行动态表脚本：

```bash
docker exec -i social-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" sm_db < src/main/resources/sql/user_schema.sql
docker exec -i social-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" sm_db < src/main/resources/sql/social_schema.sql
```

如果你已有历史 `news_user` 数据，只执行第二条即可。

### 4.5 验证

```bash
curl http://127.0.0.1:8080/user/checkLogin
docker logs -f social-app
```

## 5. 升级发布

代码更新后执行：

```bash
git pull
docker compose up -d --build
```

## 6. 常用运维命令

```bash
# 查看状态
docker compose ps

# 查看日志
docker logs -f social-app
docker logs -f social-mysql

# 停止
docker compose down

# 停止并删除数据卷（危险操作）
docker compose down -v
```

## 7. 不用 Docker 的方案（备选）

也可以直接 `mvn -DskipTests package` 后 `java -jar` 启动，并用 `systemd` 守护。  
但相比 compose，会少掉环境隔离、标准化启动和一键重建能力，不推荐作为首选。
