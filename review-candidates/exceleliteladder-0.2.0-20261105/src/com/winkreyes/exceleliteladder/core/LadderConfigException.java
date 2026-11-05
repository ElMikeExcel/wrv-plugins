package com.winkreyes.exceleliteladder.core;

/**
 * A ladder.yml defect, named exactly. Thrown by LadderConfig on ANY validation
 * failure; the plugin catches it, refuses the new table, keeps the last-good
 * table (or the built-in default on first boot) and logs the message verbatim
 * in a WARN. Extends LadderException so existing refusal handling applies.
 */
public class LadderConfigException extends LadderException {
    public LadderConfigException(String message) {
        super(message);
    }
}
