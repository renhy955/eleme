# ==============================================================================
# 多阶段构建Dockerfile - 饿了么外卖平台项目 (Ubuntu优化版)
# 工作目录: /app
# 基础镜像: ubuntu:22.04
# 包含: MySQL数据库、Redis缓存、Nginx、Spring Boot后端、Vue前端
# ==============================================================================

# 启用BuildKit并行构建
# syntax=docker/dockerfile:1.4

# ------------------------------
# 阶段1: 构建管理后台前端 (Vue 2 + Webpack)
# ------------------------------
FROM node:16-bullseye-slim AS manage-build

WORKDIR /app

# 先复制package.json安装依赖，利用Docker缓存
COPY eleme-manage/package*.json ./

RUN npm install --legacy-peer-deps --registry=https://registry.npmmirror.com

# 再复制源代码
COPY eleme-manage/ .

# 重新编译node-sass以适配Linux环境
RUN npm rebuild node-sass

RUN npm run build

# ------------------------------
# 阶段2: 构建客户端前端 (Vue 3 + Vite)
# ------------------------------
FROM node:18-bullseye-slim AS client-build

WORKDIR /app

# 先复制package.json安装依赖，利用Docker缓存
COPY eleme-client/package*.json ./

RUN npm install --legacy-peer-deps --registry=https://registry.npmmirror.com

# 再复制源代码
COPY eleme-client/ .

RUN npm run build

# ------------------------------
# 阶段3: 构建后端应用 (Spring Boot)
# ------------------------------
FROM maven:3.8.6-openjdk-8 AS backend-build

WORKDIR /app

# 配置Maven国内镜像（使用华为云镜像）
RUN mkdir -p /root/.m2 && \
    echo '<settings><mirrors><mirror><id>huaweicloud</id><mirrorOf>central</mirrorOf><url>https://repo.huaweicloud.com/repository/maven/</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# 先复制pom.xml下载依赖，利用Docker缓存
COPY eleme-backend/pom.xml ./

RUN mvn dependency:go-offline -q

# 再复制源代码
COPY eleme-backend/ .

RUN mvn clean package -DskipTests -q -Dproject.build.sourceEncoding=UTF-8

# ------------------------------
# 阶段4: 最终运行镜像
# ------------------------------
FROM ubuntu:22.04

WORKDIR /app

# 设置非交互模式
ENV DEBIAN_FRONTEND=noninteractive

# 使用国内源加速
RUN sed -i 's/archive.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    sed -i 's/security.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list

# 安装所有依赖：JDK、Nginx、MySQL、Redis
RUN apt-get update && apt-get install -y --fix-missing --no-install-recommends \
    openjdk-8-jre \
    nginx \
    mysql-server \
    redis-server \
    supervisor \
    && rm -rf /var/lib/apt/lists/*

# 设置Java环境变量
ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# 复制后端jar包
COPY --from=backend-build /app/target/eleme-backend-0.0.1-SNAPSHOT.jar app.jar

# 复制数据库初始化SQL文件
COPY eleme-introductionandrun/sql.txt /app/init.sql

# 复制前端静态文件
COPY --from=manage-build /app/dist /var/www/html/manage
COPY --from=client-build /app/dist /var/www/html/client

# 创建Nginx配置文件
RUN echo 'user www-data; \
worker_processes auto; \
pid /run/nginx.pid; \
include /etc/nginx/modules-enabled/*.conf; \
events { worker_connections 768; } \
http { \
    sendfile on; \
    tcp_nopush on; \
    types_hash_max_size 2048; \
    include /etc/nginx/mime.types; \
    default_type application/octet-stream; \
    gzip on; \
    gzip_disable "msie6"; \
    server { \
        listen 80; \
        location /manage/ { root /var/www/html; index index.html; try_files $uri $uri/ /manage/index.html; } \
        location /client/ { root /var/www/html; index index.html; try_files $uri $uri/ /client/index.html; } \
        location /boot/ { proxy_pass http://localhost:8080/boot/; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; } \
        location / { rewrite ^/$ /manage/ permanent; } \
    } \
}' > /etc/nginx/nginx.conf

# 创建Supervisor配置文件，管理所有服务
RUN echo '[supervisord]' > /etc/supervisor/conf.d/eleme.conf && \
    echo 'nodaemon=true' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'user=root' >> /etc/supervisor/conf.d/eleme.conf && \
    echo '' >> /etc/supervisor/conf.d/eleme.conf && \
    echo '[program:mysql]' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'command=/usr/sbin/mysqld' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'autostart=true' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'autorestart=true' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'priority=10' >> /etc/supervisor/conf.d/eleme.conf && \
    echo '' >> /etc/supervisor/conf.d/eleme.conf && \
    echo '[program:redis]' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'command=/usr/bin/redis-server' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'autostart=true' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'autorestart=true' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'priority=20' >> /etc/supervisor/conf.d/eleme.conf && \
    echo '' >> /etc/supervisor/conf.d/eleme.conf && \
    echo '[program:nginx]' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'command=/usr/sbin/nginx -g "daemon off;"' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'autostart=true' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'autorestart=true' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'priority=30' >> /etc/supervisor/conf.d/eleme.conf && \
    echo '' >> /etc/supervisor/conf.d/eleme.conf && \
    echo '[program:backend]' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'command=/usr/bin/java -jar /app/app.jar' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'directory=/app' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'autostart=true' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'autorestart=true' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'priority=100' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'stdout_logfile=/app/logs/backend.log' >> /etc/supervisor/conf.d/eleme.conf && \
    echo 'stderr_logfile=/app/logs/backend.err' >> /etc/supervisor/conf.d/eleme.conf

# 创建启动脚本
RUN printf '#!/bin/bash\n\
set -e\n\
echo "=== 启动饿了么外卖平台 ==="\n\
\n\
# 创建必要的目录\n\
mkdir -p /app/logs /app/upload\n\
mkdir -p /var/run/mysqld\n\
chown mysql:mysql /var/run/mysqld\n\
\n\
# 初始化MySQL（检查数据库是否存在）\n\
if [ ! -d "/var/lib/mysql/takeoutweb" ]; then\n\
    echo "初始化MySQL数据库..."\n\
    # 启动MySQL\n\
    service mysql start\n\
    sleep 10\n\
    # 创建数据库并设置密码\n\
    mysql -u root -e "CREATE DATABASE IF NOT EXISTS takeoutweb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" || true\n\
    mysql -u root -e "ALTER USER '\''root'\''@'\''localhost'\'' IDENTIFIED WITH mysql_native_password BY '\''chenxiang'\'';" || true\n\
    mysql -u root -e "FLUSH PRIVILEGES;" || true\n\
    # 导入数据库SQL文件\n\
    echo "导入数据库表结构和数据..."\n\
    mysql -u root -pchenxiang takeoutweb < /app/init.sql || true\n\
    echo "数据库导入完成"\n\
    # 创建管理员账户\n\
    echo "创建管理员账户..."\n\
    mysql -u root -pchenxiang -e "INSERT INTO takeoutweb.manager (name, password) VALUES ('\''admin'\'', '\''123456'\'');" || true\n\
    echo "管理员账户创建完成"\n\
    # 停止MySQL\n\
    service mysql stop\n\
    sleep 2\n\
    echo "数据库初始化完成"\n\
fi\n\
\n\
# 启动supervisor管理所有服务\n\
exec /usr/bin/supervisord -c /etc/supervisor/supervisord.conf\n\
' > /app/start.sh && chmod +x /app/start.sh

# 创建日志和上传目录
RUN mkdir -p /app/logs /app/upload

# 创建IK分词器词库文件
RUN touch /app/upload/ik.dic

# 暴露端口：80(Nginx)、8080(后端API)、3306(MySQL)、6379(Redis)
EXPOSE 80 8080 3306 6379

# 启动命令
CMD ["/app/start.sh"]

LABEL maintainer="eleme-dev" \
      version="1.0.0" \
      description="饿了么外卖平台 - 包含MySQL、Redis、Nginx、Spring Boot后端和Vue前端"
