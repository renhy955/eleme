# ==============================================================================
# 多阶段构建Dockerfile - 饿了么外卖平台项目 (Ubuntu优化版)
# 工作目录: /app
# 基础镜像: ubuntu:22.04
# 使用预构建镜像解决网络问题
# ==============================================================================

# 启用BuildKit并行构建
# syntax=docker/dockerfile:1.4

# ------------------------------
# 阶段1: 构建管理后台前端 (Vue 2 + Webpack)
# 使用预构建的Node.js镜像
# ------------------------------
FROM node:16-bullseye-slim AS manage-build

WORKDIR /app

# 先复制package.json安装依赖，利用Docker缓存
COPY eleme-manage/package*.json ./

RUN npm install --legacy-peer-deps --registry=https://registry.npmmirror.com

# 再复制源代码
COPY eleme-manage/ .

RUN npm run build

# ------------------------------
# 阶段2: 构建客户端前端 (Vue 3 + Vite)
# 使用预构建的Node.js镜像
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
# 使用预构建的Maven镜像
# ------------------------------
FROM maven:3.8.6-openjdk-8 AS backend-build

WORKDIR /app

# 配置Maven国内镜像
RUN mkdir -p /root/.m2 && \
    echo '<settings><mirrors><mirror><id>aliyunmaven</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# 先复制pom.xml下载依赖，利用Docker缓存
COPY eleme-backend/pom.xml ./

RUN mvn dependency:go-offline -q

# 再复制源代码
COPY eleme-backend/ .

RUN mvn clean package -DskipTests -q

# ------------------------------
# 阶段4: 最终运行镜像
# ------------------------------
FROM ubuntu:22.04

WORKDIR /app

# 使用国内源加速
RUN sed -i 's/archive.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list && \
    sed -i 's/security.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list

RUN apt-get update && apt-get install -y --fix-missing --no-install-recommends \
    openjdk-8-jre \
    nginx && \
    rm -rf /var/lib/apt/lists/*

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64
ENV PATH=$JAVA_HOME/bin:$PATH

# 复制后端jar包
COPY --from=backend-build /app/target/eleme-backend-0.0.1-SNAPSHOT.jar app.jar

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

# 创建启动脚本
RUN echo '#!/bin/bash \
set -e \
echo "=== 启动饿了么外卖平台 ===" \
echo "启动Nginx..." \
nginx \
echo "启动后端服务..." \
java -jar app.jar' > /app/start.sh && \
    chmod +x /app/start.sh

RUN mkdir -p /app/logs /app/upload

EXPOSE 80 8080

CMD ["/app/start.sh"]

LABEL maintainer="eleme-dev" \
      version="1.0.0" \
      description="饿了么外卖平台 - 包含前端和后端服务"
