FROM openjdk:15-alpine

RUN apk update
RUN apk add supervisor

RUN mkdir /kys
WORKDIR /kys

COPY ./target/scala-2.13/athena-backend.jar  athena.jar

ADD athena.sv.conf /etc/supervisor/conf.d/

# Link image source
LABEL org.opencontainers.image.source https://github.com/greg2010/Athena

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/athena.sv.conf"]
