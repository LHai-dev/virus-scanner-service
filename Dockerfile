# Builder stage: Build the Java application
FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /app

# Install dependencies for Maven Wrapper
RUN apt-get update && apt-get install -y bash && rm -rf /var/lib/apt/lists/*

# Copy Maven Wrapper, source files, and POM
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Ensure the wrapper script is executable
RUN chmod +x mvnw

# Build the project
RUN ./mvnw clean package -DskipTests

# Final stage: Configure ClamAV and run the application
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Install ClamAV and dependencies
#RUN apt-get update && \
#    apt-get install -y \
#    clamav \
#    clamav-daemon \
#    curl \
#    unzip \
#    && rm -rf /var/lib/apt/lists/*

# Configure ClamAV
# Configure ClamAV with more permissive settings
# Configure ClamAV
#RUN sed -i 's|LogFile /var/log/clamav/clamav.log|LogFile /tmp/clamav.log|g' /etc/clamav/clamd.conf && \
#    sed -i 's|LocalSocket /var/run/clamav/clamd.ctl|LocalSocket /tmp/clamd.ctl|g' /etc/clamav/clamd.conf && \
#    sed -i "s|LocalSocketGroup clamav|LocalSocketGroup root|g" /etc/clamav/clamd.conf && \
#    sed -i "s|User clamav|User root|g" /etc/clamav/clamd.conf && \
#    sed -i "s|AllowSupplementaryGroups.*|AllowSupplementaryGroups true|g" /etc/clamav/clamd.conf && \
#    sed -i "s|#TCPSocket.*|TCPSocket 3310|g" /etc/clamav/clamd.conf && \
#    sed -i "s|#TCPAddr.*|TCPAddr 0.0.0.0|g" /etc/clamav/clamd.conf && \
#    mkdir -p /tmp/clamav && \
#    touch /tmp/clamav.log && \
#    chmod 666 /tmp/clamav.log

# Update virus definitions
RUN freshclam

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar /app/app.jar

#RUN echo '#!/bin/sh\n\
## Start ClamAV daemon\n\
#freshclam --daemon &\n\
#\n\
## Start ClamAV daemon with debug logging\n\
#clamd --debug &\n\
#\n\
## Wait for ClamAV to be ready and ensure proper permissions\n\
#while [ ! -S /tmp/clamd.ctl ]; do\n\
#  echo "Waiting for ClamAV socket..."\n\
#  sleep 1\n\
#done\n\
#\n\
## Set proper permissions for the socket\n\
#chmod 666 /tmp/clamd.ctl\n\
#\n\
## Start Spring Boot application\n\
#exec java -jar /app/app.jar\n\
#' > /app/start.sh && \
#chmod +x /app/start.sh


# Expose application port
EXPOSE 8080

# Entry point
ENTRYPOINT ["/app/start.sh"]
