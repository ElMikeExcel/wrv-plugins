package com.winkreyes.excelsummons;

import java.util.Map;

/**
 * The emitter seam in plain JDK types (0.1.12): the listener and
 * the plugin speak this interface; the ONE implementation that
 * touches ExcelProtect's API lives in the protect subpackage
 * (ProtectGameplayGateway) and is bound by GameplayDoor only when
 * the ExcelProtect plugin is present and enabled — the house
 * isolation pattern (cf. BossTrial's ProtectEmitSink). With
 * Protect absent the sink is NOOP and Summons runs exactly as
 * before: the door is fire-and-forget by contract.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public interface GameplaySink {

    void emit(String kind, Map<String, Object> fields);

    GameplaySink NOOP = (kind, fields) -> { };
}
