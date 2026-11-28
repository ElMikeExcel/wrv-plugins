package excelemfixes.timedevents;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

/**
 * The indicator pillar for an active timed event: a column of
 * light standing ON the event's boss, drawn per viewer.
 *
 * Same mechanism — and the same reasoning — as the
 * quest-beacon module's BeamGuide: a real beacon block's beam
 * needs a ticking block entity a per-player fake block cannot
 * provide, and an end crystal's beam renders only on Java, so
 * the parity-honest guide is a particle column sent through
 * Player.spawnParticle (Geyser translates the packets; both
 * particle types used here have Bedrock equivalents). Nothing
 * is placed in the world: no blocks, no entities, nothing
 * persists.
 *
 * The one difference from the quest beam: a quest beam is
 * private (it marks YOUR destination), while a timed event is
 * a shared happening — so this column is drawn for every
 * player in the boss's world within the configured view
 * distance, standing on the boss itself and following it as
 * it moves (the bridge re-anchors it every refresh).
 */
public final class EventBeacon {

    private int columnHeight = 40;
    private int columnStep = 2;

    public void configure(int columnHeight, int columnStep) {
        this.columnHeight = columnHeight;
        this.columnStep = columnStep;
    }

    /**
     * Draw the column at the anchor for one viewer: bright
     * END_ROD motes with a gold DUST core — the pack's
     * established guidance palette, so players read "go here"
     * the same way they already do from the quest beam.
     */
    public void draw(Player viewer, Location anchor) {
        Particle.DustOptions gold = new Particle.DustOptions(
                Color.fromRGB(255, 200, 60), 1.6f);
        double baseY = anchor.getY();
        int i = 0;
        for (double y = baseY; y < baseY + columnHeight; y += columnStep) {
            viewer.spawnParticle(Particle.END_ROD,
                    anchor.getX(), y, anchor.getZ(), 1, 0, 0, 0, 0);
            if (i % 2 == 0) {
                viewer.spawnParticle(Particle.DUST,
                        anchor.getX(), y, anchor.getZ(), 1, 0, 0, 0, 0, gold);
            }
            i++;
        }
    }
}
