# 从仓库根目录构建：docker build -f backend/Dockerfile .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY backend/pom.xml ./pom.xml
RUN mvn dependency:go-offline -B
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# 使用 glibc 镜像，兼容 pandas、AKShare 等 Python 依赖的 Linux wheel。
FROM eclipse-temurin:21-jre-noble
WORKDIR /app
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        curl ca-certificates python3 python3-venv fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/* \
    && python3 -m venv /opt/venv
ENV PATH="/opt/venv/bin:${PATH}" \
    PYTHONIOENCODING=utf-8 \
    PYTHONUNBUFFERED=1
COPY python-service/requirements.txt ./python-service/requirements.txt
RUN pip install --no-cache-dir -r python-service/requirements.txt
COPY python-service ./python-service
COPY --from=build /build/target/*.jar ./app.jar
RUN groupadd --gid 10001 metrix \
    && useradd --uid 10001 --gid metrix --no-create-home --shell /usr/sbin/nologin metrix \
    && mkdir -p /app/logs \
    && chown -R metrix:metrix /app
USER metrix
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=10 \
    CMD curl --fail --silent --max-time 4 http://127.0.0.1:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
