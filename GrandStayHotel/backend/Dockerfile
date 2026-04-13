# Use Java 17 JDK
FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy JAR dependencies
COPY lib/ lib/

# Copy Java source files
COPY src/ src/

# Create output directory and compile
RUN mkdir -p out && javac -cp "lib/*" -d out src/hotel/*.java

# Expose port (Railway uses $PORT env var)
EXPOSE 8080

# Start the server
CMD ["java", "-cp", "out:lib/*", "hotel.HotelServer"]
