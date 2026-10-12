# Excel XP: optional Jobs integration candidate

**Off-server review candidate. Not installed or approved for installation.**

The paired source changes extend Together/weekend bonuses to Jobs XP while retaining the existing mcMMO and Jobs plugin names, commands, permissions, configuration locations and saved data. They do not add a new XP currency or convert balances.

## Paired versions

- mcMMO: `2.3.003-WRV-XP-3`, the existing pinned upstream base plus patches 0001–0005.
- Jobs: `5.2.6.3-Excel-XP-1`, a narrow source repair on upstream tag `v5.2.6.3` (commit `6fe3c272c3c970d4a94fbf7615953803eb722503`). This is not an upgrade to a newer Jobs release.
- WeekendXP: unchanged `1.0.2`.

Do not install either candidate until the complete paired review and staging checks pass and the owner approves installation. Do not replace a plugin with a second JAR beside it. Preserve current server configuration; this candidate must not restore previously disabled points settings.

## Rules

Jobs uses the same Together eligibility as mcMMO: enabled party bonus, the existing perk permission, and enough other online party members strictly within the configured same-world range. The current configured multiplier is used; the familiar defaults are 1.25× Together, 1.20× weekend, and 1.50× together on weekends.

The weekend value comes from the same runtime factor set by WeekendXP. No second calendar or independent scheduler is introduced. `/mcmmo:xprate` skill-only overrides are not silently copied into Jobs.

Only a positive, accepted Jobs gameplay award receives this factor. Jobs computes its own job/action formula, its own existing boosts, and its cancellable events first. The integration changes Jobs XP only. It does not multiply job money, points, Minecraft green-bar XP, mcMMO skill XP, or party-leveling XP a second time. Each awarded job advances independently through its existing level formula; unrelated jobs and skills are not awarded XP.

Fractional Jobs XP remains a `double`; no integer rounding or remainder currency is introduced. Nonfinite, zero, negative or overflowing bonus calculations fail closed. Offline players, Folia/async execution, Creative/Spectator, unloaded mcMMO profiles, blocked worlds and denied gameplay-XP regions do not receive the extension.

Normal positive job progress refreshes the existing temporary XP bossbar, even at the base 1.00× state. Jobs calls the notification only after the stored XP or job level actually increases. Cancellation, zero gain, or an award clamped at the job-level cap does not flash the indicator. Existing three-second duration, opt-out, world-change and disconnect cleanup continue to apply.

`/xpbonus status` includes a separate Jobs XP factor. The bar includes Jobs when the supported bridge is available. Numbers are bonus factors, not a promise that every action/job pays, nor a multiplier for levels, money or points.

## Why a paired Jobs repair is required

The exact upstream 5.2.6.3 normal action path honors `JobsExpGainEvent.getExp()`, but its fast `perform` path stores an earlier cached amount in actual job progression. A listener-only extension can therefore change the displayed/accounted XP without changing actual XP. The existing fast cache also contains prior event-modified XP, so blindly multiplying every event would reapply modifiers.

The candidate repairs the fast path to compute fresh Jobs XP through its existing formula/boost/event stages and uses that final amount for both accounting and progression. The Excel multiplier is applied once outside the fast cache. Cached money/points behavior is intentionally unchanged. Jobs' built-in level-up and maximum-level handling remain authoritative.

Existing Jobs currency-limit ordering is preserved, including its preexisting fast-path behavior. Limit counters are charged before final XP-event/Excel changes; an enabled XP allowance therefore measures the original pre-final-bonus amount, not the final awarded bonus XP. This change is not a redesign of Jobs currency caps. Review that policy before enabling caps. Never call the mutating Jobs limit check a second time merely to test eligibility.

## Optional API and failure behavior

The plugins locate one another lazily by their existing runtime names. No hard dependency, new load-order cycle, direct linkage to Jobs classes, or startup failure is introduced when either integration is absent. The Jobs build exposes an instance capability method `isExcelXpBonusSupported()`; old Jobs builds fail the capability check instead of claiming support for their broken fast-award path.

The mcMMO build exposes public instance methods:

```java
boolean isJobsXpBonusSupported();
double getJobsXpBonusMultiplier(Player player);
void notifyJobsXpGain(Player player);
```

The factor query does not award XP or show a bar. The notification API is for the paired Jobs post-storage hook only, not an arbitrary gameplay grant. Unsupported or incompatible pairings keep normal Jobs XP and existing mcMMO/green-bar behavior; the status reports Jobs unavailable. Hot plugin reload is not a supported deployment method.

## Required staging checks

1. Back up both plugin JARs, data and configuration. Keep one copy of each plugin and retain current points settings.
2. Confirm the exact candidate versions, hashes and supported bridge on an isolated server matching production.
3. Measure job XP for neither factor (1.00), Together only (1.25), weekend only (1.20), and both (1.50), using an action with a known base reward.
4. Test tiny fractional rewards, repeated same-block actions, tall crop/plant rewards, existing Jobs and third-party XP boosts, cancellation, zero/negative XP, level crossing and maximum-level caps.
5. Verify all configured jobs retain independent XP/levels; confirm unrelated jobs and mcMMO skills are unchanged by each award. Compare money and points before/after the XP-only change.
6. Check actual Java and Geyser/Bedrock bossbar rendering, three-second expiry, repeated-gain refresh, hide/show, world changes and disconnects. Status alone must not create a bar.
7. Test either plugin absent/unsupported, normal restart/load order, and existing plugin-stack compatibility. Approve an installation/restart separately only after review.

Source changes and automated test results do not constitute live gameplay or client acceptance.
