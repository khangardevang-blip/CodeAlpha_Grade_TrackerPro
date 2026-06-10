# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the Java source code into the container
COPY . /app

# Compile the Java code
RUN javac GradeServer.java

# Run the backend server
CMD ["java", "GradeServer"]
