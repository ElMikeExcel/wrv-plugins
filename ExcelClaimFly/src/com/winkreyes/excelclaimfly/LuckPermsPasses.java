package com.winkreyes.excelclaimfly;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.data.DataMutateResult;
import net.luckperms.api.model.data.TemporaryNodeMergeStrategy;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * The pass IS a LuckPerms temporary permission node (default
 * excelclaimfly.pass). LuckPerms owns the clock: the node expires on
 * real time, even while the player is offline, and survives restarts.
 * Re-buys use ADD_NEW_DURATION_TO_EXISTING, so time stacks.
 */
final class LuckPermsPasses {
    private final String nodeKey;
    LuckPermsPasses(String nodeKey) { this.nodeKey = nodeKey; }

    private LuckPerms lp() { return LuckPermsProvider.get(); }

    /** Remaining pass time in ms for a LOADED (online) user, 0 if none. */
    long remainingMs(UUID id) {
        User u = lp().getUserManager().getUser(id);
        if (u == null) return 0;
        long best = 0, now = System.currentTimeMillis();
        for (PermissionNode n : u.getNodes(NodeType.PERMISSION)) {
            if (!n.getPermission().equalsIgnoreCase(nodeKey) || !n.getValue()) continue;
            if (!n.getContexts().isEmpty()) continue; // global only
            Instant exp = n.getExpiry();
            if (exp == null) return Long.MAX_VALUE; // permanent grant (admin/testing)
            best = Math.max(best, exp.toEpochMilli() - now);
        }
        return Math.max(0, best);
    }

    /** Adds time (stacking). Returns new remaining ms, or -1 on failure. */
    long add(UUID id, Duration d) {
        User u = lp().getUserManager().getUser(id);
        if (u == null) return -1;
        Node node = PermissionNode.builder(nodeKey).value(true).expiry(d).build();
        DataMutateResult.WithMergedNode r = u.data().add(node, TemporaryNodeMergeStrategy.ADD_NEW_DURATION_TO_EXISTING);
        if (!r.getResult().wasSuccessful()) return -1;
        lp().getUserManager().saveUser(u);
        return remainingMs(id);
    }

    /** Removes every temporary pass node. Returns true if anything was removed. */
    boolean end(UUID id) {
        User u = lp().getUserManager().getUser(id);
        if (u == null) return false;
        boolean any = false;
        for (PermissionNode n : u.getNodes(NodeType.PERMISSION)) {
            if (n.getPermission().equalsIgnoreCase(nodeKey) && n.hasExpiry()) {
                any |= u.data().remove(n).wasSuccessful();
            }
        }
        if (any) lp().getUserManager().saveUser(u);
        return any;
    }
}
