package io.github.avew.oya.exception;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

import java.net.URI;

public class FileValidationException extends AbstractThrowableProblem {

    private static final URI TYPE = URI.create("https://oya.github.io/problems/file-validation-error");

    public FileValidationException(String message) {
        super(TYPE, "File Validation Error", Status.BAD_REQUEST, message);
    }
}
