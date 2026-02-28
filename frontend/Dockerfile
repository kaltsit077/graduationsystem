# 前端 Dockerfile
# 多阶段构建：先构建，再用 Nginx 服务

# 阶段1: 构建阶段
FROM node:18-alpine AS build

WORKDIR /app

# 复制 package 文件
COPY package*.json ./

# 安装依赖（使用国内镜像加速）
RUN npm config set registry https://registry.npmmirror.com && \
    npm install --legacy-peer-deps

# 复制源代码
COPY . .

# 构建生产版本
RUN npm run build

# 阶段2: 运行阶段（使用 Nginx）
FROM nginx:alpine

# 复制构建产物到 Nginx
COPY --from=build /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 暴露端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]

