FROM maven:3.3-jdk-8 AS build

WORKDIR /WORKSPACE_JAVA

COPY pom.xml .

COPY src ./src

RUN mvn clean install

FROM openjdk:17-jdk-slim

WORKDIR /WORKSPACE_JAVA

COPY --from=build /WORKSPACE_JAVA/target/*.jar app.jar

CMD ["java", "-jar", "app.jar"]