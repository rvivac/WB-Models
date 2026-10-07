# ==============================================================================
# WB SCOUTING API - DOCKERFILE MULTI-STAGE (JAVA 21 / SPRING BOOT 3)
# Otimizado para Render.com e ambientes PaaS com baixo consumo de memória
# ==============================================================================

# Estágio 1: Build da aplicação com JDK 21
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Copia arquivos do Maven Wrapper e POM para aproveitar cache de camadas
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x ./mvnw

# Baixa dependências para cache
RUN ./mvnw dependency:go-offline -B || true

# Copia código-fonte e gera o JAR executável (sem rodar testes pesados no build)
COPY src/ src/
RUN ./mvnw clean package -DskipTests -B

# Estágio 2: Imagem final enxuta com JRE 21
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Cria usuário seguro sem privilégios de root
RUN addgroup --system spring && adduser --system spring --ingroup spring
USER spring:spring

# Copia o JAR empacotado do estágio de build
COPY --from=builder /app/target/*.jar app.jar

# Configurações de porta e controle de memória para o limite do plano gratuito do Render (512MB)
ENV PORT=8080
ENV JAVA_OPTS="-Xms128m -Xmx350m -XX:+UseSerialGC"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE:-prod} -jar app.jar"]
