FROM gradle:jdk21 AS build

WORKDIR /usr/app/
COPY . .
RUN gradle build -x test

FROM ubuntu:latest
LABEL authors="dpozinen"

ARG PROJECT_VERSION

RUN apt -y update
RUN apt -y install openjdk-21-jdk

WORKDIR /opt/app/

COPY --from=build "/usr/app/build/libs/rodin-$PROJECT_VERSION.jar" "rodin.jar"

EXPOSE 8080
EXPOSE 8081

CMD ["/usr/bin/java", "-jar", "/opt/app/rodin.jar"]
