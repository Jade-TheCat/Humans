package dev.jadethecat.humans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;

@Config(name = "humans")
public class HumansConfig implements ConfigData{
    /**
     * Names which Humans can spawn with.
     */
    public List<PlayerSkin> spawnableNames = new ArrayList<>(Arrays.asList(
        new PlayerSkin("Jade_TheCat", true), 
        new PlayerSkin("KitsuneAlex", false),
        new PlayerSkin("LemmaEOF", false),
        new PlayerSkin("UpcraftLP", false),
        new PlayerSkin("shedaniel", false)
    ));

    /**
     * Mojangstas Humans can spawn as.
     */
    public List<PlayerSkin> spawnableMojangstas = new ArrayList<>(Arrays.asList(
        new PlayerSkin("Dinnerbone", false),
        new PlayerSkin("jeb_", false),
        new PlayerSkin("slamp00", false),
        new PlayerSkin("kingbdogz", false),
        new PlayerSkin("LadyAgnes", false),
        new PlayerSkin("C418", false),
        new PlayerSkin("Jappaa", false),
        new PlayerSkin("ProfMobius", false),
        new PlayerSkin("Marc_IRL", false),
        new PlayerSkin("slicedlime", false),
        new PlayerSkin("Grumm", false)
    ));

    /**
     * Should Humans exclusively spawn with names from the spawnableNames option?
     */
    public boolean exclusivelySpawnNamed = false;

    /**
     * Chance for a human to spawn named, does not do anything if exclusivelySpawnNamed is true.
     * Human will spawn named when a value less than this is chosen randomly.
     */
    public float chanceToSpawnNamed = 0.1f;

    /**
     * Chance for a human to spawn using a slim skin (Alex). Ignored if exclusivelySpawnNamed is true.
     */
    public float chanceToSpawnSlim = 0.5f;

    /**
     * Chance for a human to spawn using legacy sounds (OOF). Ignored if exclusivelySpawnNamed is true.
     */
    public float chanceToSpawnWithLegacySounds = 0.01f;

    /**
     * Chance for a human to spawn using the legacy flaily-arms animation. Ignored if exclusivelySpawnNamed is true.
     */
    public float chanceToSpawnWithLegacyAnimation = 0.01f;

    /**
     * Chance for a human to spawn as a Mojangsta. Ignored if exclusivelySpawnNamed is true.
     */
    public float chanceToSpawnMojangsta = 0.001f;

    /**
     * How far a Human will wander when in sentry mode.
     */
    public int humanSentryWanderRange = 50;

    public static class PlayerSkin {
        public String name;
        public boolean slimSkin;

        public PlayerSkin(String name, boolean slimSkin) {
            this.name = name;
            this.slimSkin = slimSkin;
        }
        public PlayerSkin() {
            this.name = "Steve";
            this.slimSkin = false;
        }
    }
}
