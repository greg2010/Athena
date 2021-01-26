FROM alpine:3

RUN apk add --no-cache bash inotify-tools openjdk15 nodejs sbt npm --repository=http://dl-cdn.alpinelinux.org/alpine/edge/testing

RUN mkdir -p /workspace/bindpoint

COPY entrypoint.sh /workspace
RUN chmod a+x /workspace/entrypoint.sh

ENTRYPOINT /workspace/entrypoint.sh