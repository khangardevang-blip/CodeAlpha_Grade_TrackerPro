FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app
COPY . .

# Download the MySQL JDBC Driver so the server can talk to the database
RUN curl -o mysql-connector.jar https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar

# Compile the Java file
RUN javac GradeServer.java

# Run the server with the MySQL driver added to the classpath
CMD ["java", "-cp", ".:mysql-connector.jar", "GradeServer"]
