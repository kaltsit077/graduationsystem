# 后端 Dockerfile
# 多阶段构建：先编译，再运行

# 阶段1: 构建阶段
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# 复制 pom.xml 和源代码
COPY pom.xml .
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

# 暴露端口
EXPOSE 9090

# 设置 JVM 参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# 启动应用
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=prod -jar app.jar"]


