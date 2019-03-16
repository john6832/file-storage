package io.metadata.filestorage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FileNotFoundException extends RuntimeException {

    public FileNotFoundException(String fileName) {
        super("The file "+fileName+" cannot be found");
    }

    public FileNotFoundException(String fileName, Throwable cause) {
        super("The file "+fileName+" cannot be found", cause);
    }
}
