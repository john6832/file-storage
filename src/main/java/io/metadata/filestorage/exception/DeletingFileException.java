package io.metadata.filestorage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DeletingFileException extends RuntimeException {
    public DeletingFileException(Throwable cause) {
        super("An error has occurred while deleting the file", cause);
    }
}
