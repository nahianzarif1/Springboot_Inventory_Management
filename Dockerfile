FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./
COPY src/ src/

RUN chmod +x mvnw \
    && ./mvnw -q -DskipTests clean package

RUN mkdir -p /app/uploads/products

EXPOSE 8080

CMD ["java", "-jar", "target/Inventory_Management-0.0.1-SNAPSHOT.jar"]
