package com.winkreyes.excelmagicgate;

import java.util.function.Consumer;

/**
 * The stage-apply sequence as a pure orchestrator (no Bukkit deps), so
 * the failure-atomicity contract is unit-testable with fake steps.
 *
 * Order: backup -> swapConfig -> reloadMagic -> runGrants -> persist ->
 * announce. Every step before persist is abort-on-failure: if one throws,
 * the sequence stops, appliedStage is NOT advanced (persist never runs),
 * and the caller logs the failed step and retries on the next watch tick.
 * Announce runs last and is best-effort: the stage is already fully
 * applied and persisted by then, so an announcement failure is logged as
 * a warning and does not turn the apply into a failure.
 */
public final class StageApplier {

    public interface Steps {
        void backup() throws Exception;

        void swapConfig() throws Exception;

        void reloadMagic() throws Exception;

        void runGrants() throws Exception;

        void persist() throws Exception;

        void announce() throws Exception;
    }

    public record Result(boolean success, String failedStep, String error) {
        static Result ok() {
            return new Result(true, null, null);
        }

        static Result failed(String step, Exception e) {
            String msg = e.getMessage();
            return new Result(false, step, msg == null ? e.toString() : msg);
        }
    }

    private StageApplier() {}

    public static Result apply(Steps steps, Consumer<String> warnLog) {
        String step = "backup";
        try {
            steps.backup();
            step = "swapConfig";
            steps.swapConfig();
            step = "reloadMagic";
            steps.reloadMagic();
            step = "runGrants";
            steps.runGrants();
            step = "persist";
            steps.persist();
        } catch (Exception e) {
            return Result.failed(step, e);
        }
        try {
            steps.announce();
        } catch (Exception e) {
            if (warnLog != null) {
                warnLog.accept("announcement failed after a successful apply: " + e.getMessage());
            }
        }
        return Result.ok();
    }
}
