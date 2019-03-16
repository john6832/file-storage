package io.metadata.filestorage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InvalidPathException extends RuntimeException {

    public InvalidPathException() {
        super("The file you are you are trying to upload has an invalid path");
    }
}
