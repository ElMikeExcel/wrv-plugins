package com.winkreyes.auditlog;

/** One merged audit row from any source. */
public record Entry(long epochMs, String type, String player, String details) { }
