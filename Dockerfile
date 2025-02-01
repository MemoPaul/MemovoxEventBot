

    
    # 1. Use an official Maven + Java 17 image to build the code
FROM maven:3.8.8-eclipse-temurin-17 AS build

 # 2. Create a directory for our app
WORKDIR /app

 # 3. Copy our project files (including pom.xml and src folder)
COPY . /app

 # 4. Build the JAR
RUN mvn clean package -DskipTests

 # 5. Use a smaller runtime image (only Java, not Maven)
FROM eclipse-temurin:17-jre
WORKDIR /app

 # 6. Copy the final JAR from the build stage
COPY --from=build /app/target/MemovoxEventBot-1.0-SNAPSHOT.jar /app/bot.jar

 # 7. Command to run your bot
CMD ["java", "-jar", "bot.jar"]

  



