# 后端 Dockerfile
# 多阶段构建：先编译，再运行

# 阶段1: 构建阶段
FROM maven:3.9.9-eclipse-temurin-17 AS build

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

# 阶段2: 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

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