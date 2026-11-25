/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.dragon;

import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * The phase-flip theatre, extracted from ExcelDragonTrial.runFlip in
 * 0.1.2 so the test suite can drive this exact code path
 * (FlipEffectsTest) and so an effects failure can be fenced off from
 * the flip's state changes by the caller.
 *
 * HOTFIX NOTE (0.1.2): on Paper 26.2, Particle.DRAGON_BREATH carries
 * a required Float data argument (its particle options). The old
 * no-data spawnParticle overload makes CraftParticle throw
 * IllegalArgumentException ("missing required data class
 * java.lang.Float"), which in 0.1.0/0.1.1 aborted runFlip
 * mid-transition and stranded the dragon (and the fight) in the
 * invulnerable TRANSITION state. The call below passes the Float
 * data explicitly: 1.0f is the particle's vanilla scale (the same
 * value the dragon's own breath attack uses). The compile-classpath
 * Paper API predates the Float requirement, so this contract is
 * pinned by FlipEffectsTest against the documented 26.2 runtime
 * behaviour and proven live on the 26.2 rig.
 */
final class FlipEffects {

    private FlipEffects() {
    }

    static void play(World world, Location fx, Location fountain,
            List<Player> audience, String title, String subtitle, String chat) {
        // The particle burst goes FIRST: it is the historically
        // fragile call (the 0.1.2 hotfix), and leading with it lets
        // FlipEffectsTest reach it in a bare JVM, where the Sound
        // constants cannot initialise (no server registry). Same
        // tick in game — the order is invisible to players.
        world.spawnParticle(Particle.DRAGON_BREATH, fx, 200, 4.0, 2.0, 4.0, 0.05,
                Float.valueOf(1.0f));
        world.playSound(fx, Sound.ENTITY_ENDER_DRAGON_GROWL, 10.0f, 0.8f);
        world.strikeLightningEffect(fountain);
        for (Player p : audience) {
            p.sendTitle(title, subtitle, 10, 70, 20);
            p.sendMessage(chat);
        }
    }
}

