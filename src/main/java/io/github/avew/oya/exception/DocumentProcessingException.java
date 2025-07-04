package io.github.avew.oya.exception;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

import java.net.URI;

public class DocumentProcessingException extends AbstractThrowableProblem {

    private static final URI TYPE = URI.create("https://oya.github.io/problems/document-processing-error");

    public DocumentProcessingException(String message) {
        super(TYPE, "Document Processing Error", Status.INTERNAL_SERVER_ERROR, message);
    }

    public DocumentProcessingException(String message, Throwable cause) {
        super(TYPE, "Document Processing Error", Status.INTERNAL_SERVER_ERROR, message);
        initCause(cause);
    }
}
