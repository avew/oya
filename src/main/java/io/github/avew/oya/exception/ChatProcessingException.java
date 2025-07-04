package io.github.avew.oya.exception;

import org.zalando.problem.AbstractThrowableProblem;
import org.zalando.problem.Status;

import java.net.URI;

public class ChatProcessingException extends AbstractThrowableProblem {

    private static final URI TYPE = URI.create("https://oya.github.io/problems/chat-processing-error");

    public ChatProcessingException(String message) {
        super(TYPE, "Chat Processing Error", Status.INTERNAL_SERVER_ERROR, message);
    }

    public ChatProcessingException(String message, Throwable cause) {
        super(TYPE, "Chat Processing Error", Status.INTERNAL_SERVER_ERROR, message);
        initCause(cause);
    }
}
