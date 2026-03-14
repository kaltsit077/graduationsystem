# 阶段1: 构建阶段（用轻量 node 镜像，无需编译工具）
FROM node:18-alpine AS build

WORKDIR /app

# 复制包管理文件
COPY package*.json ./

# 配置国内源 + 安装依赖（仅安装运行/构建依赖，无编译）
RUN npm config set registry https://registry.npmmirror.com && \
    npm install --legacy-peer-deps && \
    npm cache clean --force

# 复制源代码
COPY . .

# 构建生产版本（已跳过 vue-tsc）
RUN npm run build

# 阶段2: 运行阶段
FROM nginx:alpine

# 复制构建产物
COPY --from=build /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]