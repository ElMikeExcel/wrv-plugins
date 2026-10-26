package com.winkreyes.exceleliteladder.core;

/** A refusal with a reason that goes straight into the log and /eliteladder status. */
public class LadderException extends RuntimeException {
    public LadderException(String message) {
        super(message);
    }
}
