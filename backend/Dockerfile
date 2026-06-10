# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk-jammy

# Set the working directory inside the container
WORKDIR /app

# Copy the Java source code into the container
COPY . /app

# Compile the Java code
RUN javac GradeServer.java

# Run the backend server
CMD ["java", "GradeServer"]
