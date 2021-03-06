FROM openjdk:8-jdk-alpine as build-stage

RUN apk --no-cache add git && git clone https://github.com/yt8492/SeihekiAnalyzer --depth 1 -b v1.09

WORKDIR /SeihekiAnalyzer
RUN chmod +x ./gradlew && ./gradlew shadowJar

FROM openjdk:8-jdk-alpine as exec-stage
COPY --from=build-stage /SeihekiAnalyzer/build/libs/SeihekiAnalyze-1.0.9-all.jar .

ENTRYPOINT ["java","-jar","SeihekiAnalyze-1.0.9-all.jar"]
