FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -q -DskipTests package || true

COPY src/ src/
RUN ./mvnw -q -DskipTests package

EXPOSE 8080

CMD ["java", "-jar", "target/Inventory_Management-0.0.1-SNAPSHOT.jar"]
