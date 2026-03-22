# 后端 Dockerfile
# 多阶段构建：先编译，再运行
#
# 国内/阿里云 ECS 常无法直连 registry-1.docker.io，构建会卡在拉基础镜像。
# 默认通过 DaoCloud 代理拉取（与官方 hub 镜像名一致，仅加前缀）。
# 若代理不可用，可本机构建后 docker save 传到服务器 docker load，或改下面 ARG 为其它镜像源。
#
# 可选镜像前缀示例（自行测试哪个可用）：
#   docker.m.daocloud.io/library/...
#   docker.1ms.run/library/...
#   （直连 Docker Hub 时设为空字符串，FROM 需写完整官方名，见下方注释）

ARG DOCKERHUB_PROXY=docker.m.daocloud.io
# 构建阶段基础镜像（官方 library/maven）
FROM ${DOCKERHUB_PROXY}/library/maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# 关键修复：添加阿里云 Maven 镜像配置，解决依赖下载失败
RUN mkdir -p /root/.m2 && \
    echo "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" > /root/.m2/settings.xml && \
    echo "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.2.0\"" >> /root/.m2/settings.xml && \
    echo "          xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" >> /root/.m2/settings.xml && \
    echo "          xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd\">" >> /root/.m2/settings.xml && \
    echo "  <mirrors>" >> /root/.m2/settings.xml && \
    echo "    <mirror>" >> /root/.m2/settings.xml && \
    echo "      <id>aliyunmaven</id>" >> /root/.m2/settings.xml && \
    echo "      <mirrorOf>central</mirrorOf>" >> /root/.m2/settings.xml && \
    echo "      <url>https://maven.aliyun.com/repository/public</url>" >> /root/.m2/settings.xml && \
    echo "    </mirror>" >> /root/.m2/settings.xml && \
    echo "  </mirrors>" >> /root/.m2/settings.xml && \
    echo "</settings>" >> /root/.m2/settings.xml

# 优先复制 pom.xml（利用 Docker 缓存，依赖不变时无需重新下载）
COPY pom.xml .

# 预下载依赖（可选，进一步加速构建）
RUN mvn dependency:go-offline -DskipTests

# 复制源代码
COPY src ./src

# 构建应用（跳过测试）
RUN mvn clean package -DskipTests

# 阶段2: 运行阶段（与阶段1 使用同一代理前缀；此处再声明一次，兼容旧版 Docker 多阶段 ARG 作用域）
ARG DOCKERHUB_PROXY=docker.m.daocloud.io
FROM ${DOCKERHUB_PROXY}/library/eclipse-temurin:17-jre-alpine

WORKDIR /app

# 使用国内镜像并安装 wget（直接覆盖源，避免 dl-cdn TLS 超时）
RUN echo "http://mirrors.aliyun.com/alpine/v3.19/main" > /etc/apk/repositories \
    && echo "http://mirrors.aliyun.com/alpine/v3.19/community" >> /etc/apk/repositories \
    && apk add --no-cache wget

# 创建日志目录
RUN mkdir -p /app/logs

# 从构建阶段复制 JAR 文件
COPY --from=build /app/target/graduation-topic-backend-0.0.1-SNAPSHOT.jar app.jar

# 暴露端口（保留你的 9090 端口）
EXPOSE 9090

# 设置 JVM 参数（保留你的原有配置）
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 启动应用（保留你的原有启动逻辑，prod 环境）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]