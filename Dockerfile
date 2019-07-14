FROM openjdk:8-jdk-alpine as build-stage

RUN apk --no-cache add git && git clone https://github.com/yt8492/SeihekiAnalyzer --depth 1 -b v1.08

WORKDIR /SeihekiAnalyzer
RUN chmod +x ./gradlew && ./gradlew jar

FROM openjdk:8-jdk-alpine as exec-stage
COPY --from=build-stage /SeihekiAnalyzer/build/libs/SeihekiAnalyze-1.0.8-all.jar .

ENTRYPOINT ["java","-jar","SeihekiAnalyze-1.0.8-all.jar"]
