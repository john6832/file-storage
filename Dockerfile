FROM openjdk:11-jre-slim
VOLUME /tmp
VOLUME /database/files
ARG DEPENDENCY=target/dependency
COPY ${DEPENDENCY}/BOOT-INF/lib /app/lib
COPY ${DEPENDENCY}/META-INF /app/META-INF
COPY ${DEPENDENCY}/BOOT-INF/classes /app
ENTRYPOINT ["java","-cp","app:app/lib/*","io.metadata.filestorage.FileStorageApplication"]