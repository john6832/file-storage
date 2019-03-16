package io.metadata.filestorage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PersistingFileException extends RuntimeException {
    public PersistingFileException(Throwable cause) {
        super("An error has occurred while persisting the file", cause);
    }
}
