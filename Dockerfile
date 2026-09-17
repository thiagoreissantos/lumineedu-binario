# =====================================================================
# lumineedu-binario - imagem otimizada (multi-estagio)
#
# Estagio 1 (builder): Maven + OpenJDK 21 (JDK) para compilar o jar.
# Estagio 2 (runtime):  JRE 21 Alpine (imagem leve) com a aplicacao
#                       rodando como um usuario nao-root.
#
# O perfil do Spring (dev/homol/prod) NAO e embutido aqui: e fornecido
# no runtime via variavel de ambiente SPRING_PROFILES_ACTIVE (ex.:
#   docker run -e SPRING_PROFILES_ACTIVE=prod ...
# =====================================================================

# ---------------------------------------------------------------------
# Estagio de build
# ---------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Resolve e cacheia as dependencias (camada cacheavel do Docker).
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Copia o codigo-fonte e empacota o jar "fat" (nao executa os testes).
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------------------------------------------------------------------
# Estagio de runtime
# ---------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Cria um usuario/grupo nao-root e o diretorio de armazenamento dos
# arquivos (criado e possuido pelo usuario da aplicacao).
RUN adduser -u 10001 -D -H -s /bin/sh lumineedu \
    && mkdir -p /var/app/lumineedu/binario/storage \
    && chown -R 10001:10001 /app /var/app/lumineedu

# Tuning de memoria Java (sobreescritavel no runtime via JAVA_OPTS).
ENV JAVA_OPTS="-Xmx512m -Xss64m"

# Porta HTTP do Spring Boot.
EXPOSE 8080

# Copia o jar empacotado pelo estagio de build.
COPY --from=builder /workspace/target/lumineedu-binario.jar /app/lumineedu-binario.jar

# Executa a aplicacao como o usuario nao-root.
USER lumineedu
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/lumineedu-binario.jar"]
