/* SPDX-License-Identifier: MIT */
package com.winkreyes.shops.store;

/** A safe, player-readable refusal. It does not expose underlying file paths. */
public class RegistryException extends Exception {
    public RegistryException(String message) { super(message); }
    public RegistryException(String message, Throwable cause) { super(message, cause); }
}
