package com.winkreyes.exceleitemobsabilitybedrockbridge;

import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Canned AcsGateway for the plain-JVM suite. Records fire calls. */
final class StubGateway implements AcsGateway {

    boolean ready = true;
    ClassView view = ClassView.noClass();
    FireOutcome nextOutcome = FireOutcome.success("stub.ability");
    final List<BridgeSlot> firedSlots = new ArrayList<>();

    @Override
    public boolean moduleReady() {
        return ready;
    }

    @Override
    public ClassView classView(Player player) {
        return view;
    }

    @Override
    public FireOutcome fire(Player player, BridgeSlot slot) {
        firedSlots.add(slot);
        return nextOutcome;
    }
}
