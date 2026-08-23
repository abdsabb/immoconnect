# Image backend ImmoConnect — construction en deux étapes (livrable 13, §3.3)
# Étape 1 : compilation Maven avec le JDK complet
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B --no-transfer-progress dependency:go-offline
COPY src ./src
RUN ./mvnw -B --no-transfer-progress -DskipTests package

# Étape 2 : image d'exécution allégée (JRE seul), utilisateur non-root (livrable 16)
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S immo && adduser -S immo -G immo
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER immo
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
HEALTHCHECK --interval=30s --timeout=5s --retries=5 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
