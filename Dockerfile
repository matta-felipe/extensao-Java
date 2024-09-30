FROM maven:3.8.6 AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-alpine

WORKDIR /app

COPY --from=build /app/target/*.jar /app/

EXPOSE 8080

CMD ["java", "-jar", "para-onde-foi.jar"]