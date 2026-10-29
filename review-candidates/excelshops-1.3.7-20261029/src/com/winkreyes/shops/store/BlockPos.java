// Excel signature - WRV maintained source; original authorship and license notices preserved.
/* SPDX-License-Identifier: MIT */
package com.winkreyes.shops.store;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;

/** A world UUID and exact block coordinates. Resolving never loads a chunk. */
public record BlockPos(UUID worldId, int x, int y, int z) {
    public BlockPos { Objects.requireNonNull(worldId, "worldId"); }
    public static BlockPos of(Block block) {
        return new BlockPos(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }
    public Block resolve(Server server) throws RegistryException {
        World world = server.getWorld(worldId);
        if (world == null || !world.isChunkLoaded(x >> 4, z >> 4))
            throw new RegistryException("The shop chunk is not loaded. Visit the shop and try again.");
        if (y < world.getMinHeight() || y >= world.getMaxHeight())
            throw new RegistryException("The shop position is outside the world.");
        return world.getBlockAt(x, y, z);
    }
    public boolean adjacent(BlockPos other) {
        return worldId.equals(other.worldId) && Math.abs((long) x - other.x)
                + Math.abs((long) y - other.y) + Math.abs((long) z - other.z) == 1;
    }
    public BlockPos offset(int dx, int dy, int dz) {
        return new BlockPos(worldId, x + dx, y + dy, z + dz);
    }
}
