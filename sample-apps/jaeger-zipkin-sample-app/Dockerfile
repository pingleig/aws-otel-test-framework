FROM gradle:6.8.3 as builder

WORKDIR /app
COPY ./build.gradle ./build.gradle
COPY ./src ./src
COPY ./build/ ./build/

# binary version variable  --build-arg <VER>=<1.x>
# default 1.0
ARG VER=1.0

RUN gradle build
RUN tar -xvf build/distributions/jaeger-zipkin-sample-app-${VER}.tar

FROM amazoncorretto:11

WORKDIR /app
COPY --from=builder /app/jaeger-zipkin-sample-app-${VER} .

ENV HOME=/root

CMD ["/app/bin/jaeger-zipkin-sample-app"]






