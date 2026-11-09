package com.winkreyes.auditlog;

import org.bukkit.Location;

/**
 * LINK-CHECK STUB — compile-only stand-in for ExcelAuditLog's own
 * EventLog, carrying just the members its GpBridge calls. Exists so
 * the UNCHANGED GpBridge.java can be compile-checked against the
 * ExcelClaims facade classes; it is not shipped anywhere.
 */
public class EventLog {
    public Json.Obj base(String type, Location loc) { return new Json.Obj(); }
    public void emit(Json.Obj obj) { }
}
