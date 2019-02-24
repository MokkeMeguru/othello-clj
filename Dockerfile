FROM openjdk:8-alpine

COPY target/uberjar/othello.jar /othello/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/othello/app.jar"]
