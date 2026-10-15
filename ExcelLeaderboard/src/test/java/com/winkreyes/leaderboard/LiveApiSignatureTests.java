package com.winkreyes.leaderboard;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Regression suite for the Oct 14, 2026 live failure: ExcelLeaderboard
 * 1.0's reflective probes named API entry points that do not exist in the
 * installed plugins (mcMMO DatabaseAPI has no getPowerLevel(String); Jobs
 * 5.2.6.3 has no com.gamingmesh.jobs.api.JobsAPI class at all), so the
 * mcMMO Power and Jobs Levels boards hid themselves at the first live
 * boot. This suite checks the probe targets against the ACTUAL installed
 * jars: when a jar is present at its known path, every signature the
 * probes rely on must exist, or the build fails. When a jar is absent,
 * its checks are skipped (and say so) so the suite stays portable. */
public final class LiveApiSignatureTests {

    private static int checks = 0;

    public static void main(String[] args) throws Exception {
        checkMcMMO(new File("/home/hatch/workspace/mcmmo-edit/repo/target/mcMMO.jar"));
        checkJobs(new File("/home/hatch/workspace/economy-jars/Jobs.jar"));
        System.out.println("LiveApiSignatureTests: " + checks + " assertions passed");
    }

    private static void checkMcMMO(File jar) throws Exception {
        if (!jar.isFile()) {
            System.out.println("LiveApiSignatureTests: mcMMO jar absent, mcMMO checks SKIPPED");
            return;
        }
        try (URLClassLoader loader = loaderFor(jar)) {
            Class<?> experienceApi = Class.forName("com.gmail.nossr50.api.ExperienceAPI", false, loader);
            Method m = experienceApi.getMethod("getPowerLevelOffline", UUID.class);
            eq(m.getReturnType() == int.class, true, "ExperienceAPI.getPowerLevelOffline(UUID) returns int");
        }
    }

    private static void checkJobs(File jar) throws Exception {
        if (!jar.isFile()) {
            System.out.println("LiveApiSignatureTests: Jobs jar absent, Jobs checks SKIPPED");
            return;
        }
        // Jobs class signatures reference CMILib types (its required
        // companion library, installed alongside Jobs) and Vault types
        // (Jobs hooks Vault permissions), so the loader gets the same
        // companion set the live server runs: Jobs + CMILib + Vault.
        File cmiLib = new File("/home/hatch/workspace/economy-jars/CMILib.jar");
        File vault = new File("/home/hatch/workspace/vault-build/Vault-2.0.0.jar");
        List<File> jars = new ArrayList<>();
        jars.add(jar);
        if (cmiLib.isFile()) {
            jars.add(cmiLib);
        }
        if (vault.isFile()) {
            jars.add(vault);
        }
        try (URLClassLoader loader = loaderFor(jars.toArray(new File[0]))) {
            Class<?> jobsMain = Class.forName("com.gamingmesh.jobs.Jobs", false, loader);
            Method getPlayerManager = jobsMain.getMethod("getPlayerManager");
            eq(getPlayerManager.getReturnType().getName(),
                    "com.gamingmesh.jobs.PlayerManager", "Jobs.getPlayerManager() return type");
            Class<?> playerManager = Class.forName("com.gamingmesh.jobs.PlayerManager", false, loader);
            Method getJobsPlayer = playerManager.getMethod("getJobsPlayer", UUID.class);
            eq(getJobsPlayer.getReturnType().getName(),
                    "com.gamingmesh.jobs.container.JobsPlayer", "PlayerManager.getJobsPlayer(UUID) return type");
            Class<?> jobsPlayer = Class.forName("com.gamingmesh.jobs.container.JobsPlayer", false, loader);
            Method getTotalLevels = jobsPlayer.getMethod("getTotalLevels");
            eq(getTotalLevels.getReturnType() == int.class, true, "JobsPlayer.getTotalLevels() returns int");
            jobsPlayer.getMethod("getJobProgression");
            checks++; // fallback path used by jobsTotalLevel() exists
        }
    }

    private static URLClassLoader loaderFor(File... jars) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (File jar : jars) {
            urls.add(jar.toURI().toURL());
        }
        return new URLClassLoader(urls.toArray(new URL[0]),
                LiveApiSignatureTests.class.getClassLoader());
    }

    private static void eq(Object actual, Object expected, String label) {
        checks++;
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }
}
