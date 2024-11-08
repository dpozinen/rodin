FROM gradle:jdk21 AS build

WORKDIR /usr/app/
COPY . .
RUN gradle build -x test

FROM ubuntu:latest
LABEL authors="dpozinen"

ARG PROJECT_VERSION

RUN apt -y update
RUN apt -y install openjdk-21-jre

WORKDIR /opt/app/

COPY --from=build "/usr/app//build/distributions/rodin-$PROJECT_VERSION.tar" .

RUN tar -xvf rodin-$PROJECT_VERSION.tar

WORKDIR /opt/app/rodin-$PROJECT_VERSION/bin

EXPOSE 8080
EXPOSE 8081

ENTRYPOINT ["./rodin"]