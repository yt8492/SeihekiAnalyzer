FROM openjdk:8-jdk-alpine as build-stage

RUN apk --no-cache add git && git clone https://github.com/yt8492/SeihekiAnalyzer

WORKDIR /SeihekiAnalyzer
RUN chmod +x ./gradlew && ./gradlew jar

FROM openjdk:8-jdk-alpine as exec-stage
COPY --from=build-stage /SeihekiAnalyzer/build/libs/SeihekiAnalyze-1.0.8.jar .

ENTRYPOINT ["java","-jar","SeihekiAnalyze-1.0.8.jar"]
