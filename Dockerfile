FROM amazoncorretto:17-alpine-jdk
WORKDIR /app
COPY target/nginxloghub.jar ./
ENTRYPOINT ["java", "-jar", "./nginxloghub.jar"]
