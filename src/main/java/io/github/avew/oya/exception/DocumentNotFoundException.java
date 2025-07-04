package io.github.avew.oya.exception;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

import java.net.URI;

public class DocumentNotFoundException extends AbstractThrowableProblem {

    private static final URI TYPE = URI.create("https://oya.github.io/problems/document-not-found");

    public DocumentNotFoundException(String documentId) {
        super(TYPE, "Document Not Found", Status.NOT_FOUND,
              String.format("Document with ID '%s' was not found", documentId));
    }
}
