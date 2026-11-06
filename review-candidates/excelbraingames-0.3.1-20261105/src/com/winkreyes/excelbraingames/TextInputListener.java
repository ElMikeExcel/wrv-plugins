package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Money;
import com.winkreyes.excelbraingames.games.text.QuickMathRound;
import com.winkreyes.excelbraingames.games.text.RiddleText;
import com.winkreyes.excelbraingames.games.text.ScrambleRound;
import com.winkreyes.excelbraingames.gui.BrainHolder;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;

/**
 * Chat answers for the text games (Phase 3). When a player is in
 * answer mode (a typed Quick Math / Scramble round, or the Riddle
 * answer door), their next chat messages are captured as answers —
 * never broadcast — and fed to the SAME shared question loop the GUI
 * card uses (TextLoop). Typing "stop" leaves answer mode without
 * spending anything; rounds save at question boundaries (SPEC §9.5).
 */
public final class TextInputListener implements Listener {

    private final ExcelBrainGames plugin;

    public TextInputListener(ExcelBrainGames plugin) { this.plugin = plugin; }

    @EventHandler
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        String game = plugin.service().pendingGame(p.getUniqueId());
        if (game == null) return;
        e.setCancelled(true);
        String msg = PlainTextComponentSerializer.plainText().serialize(e.message());
        plugin.getServer().getScheduler().runTask(plugin, () -> processAnswer(p, game, msg));
    }

    /** Score one answer through the shared loop; shared by chat and card taps. */
    public void processAnswer(Player p, String game, String response) {
        GameService svc = plugin.service();
        PlayerData pd = svc.data(p);
        if (response != null && (response.trim().equalsIgnoreCase("stop")
                || response.trim().equalsIgnoreCase("quit"))) {
            svc.clearInput(p.getUniqueId());
            p.sendMessage(GameService.text("&7Answer mode off — your round is saved. Reopen it from &f/games&7 any time."));
            return;
        }
        switch (game) {
            case "quickmath" -> answerQuickMath(p, pd, response);
            case "scramble" -> answerScramble(p, pd, response);
            case "riddle" -> answerRiddle(p, pd, response);
            default -> svc.clearInput(p.getUniqueId());
        }
    }

    private void answerQuickMath(Player p, PlayerData pd, String response) {
        GameService svc = plugin.service();
        QuickMathRound r = pd.quickMath;
        if (r == null || r.finished()) { svc.clearInput(p.getUniqueId()); return; }
        try { Integer.parseInt(response.trim()); }
        catch (NumberFormatException e) {
            p.sendMessage(GameService.text("&7That's not a number — no try spent. Type a number, or &fstop&7 to pause."));
            return;
        }
        int answerWas = r.currentAnswer();
        var verdict = plugin.textLoop().answer(p.getUniqueId().toString(), "quickmath", response);
        svc.store().save(pd);
        if (verdict.correct()) {
            p.sendMessage(GameService.text("&a✓ Right! &7(" + r.correct() + "/" + QuickMathRound.QUESTIONS + ")"));
        } else {
            p.sendMessage(GameService.text("&c✗ Not quite — it was &f" + answerWas + "&c."));
        }
        if (r.finished()) {
            svc.completeQuickMath(p, pd, r);
            closeCardIf(p, "quickmath");
        } else {
            refreshCard(p, "quickmath");
            if (!r.choices()) promptInChat(p, "quickmath");
        }
    }

    private void answerScramble(Player p, PlayerData pd, String response) {
        GameService svc = plugin.service();
        ScrambleRound r = pd.scramble;
        if (r == null || r.finished()) { svc.clearInput(p.getUniqueId()); return; }
        String wordWas = r.currentWord();
        long before = r.earnedCents();
        var verdict = plugin.textLoop().answer(p.getUniqueId().toString(), "scramble", response);
        svc.store().save(pd);
        if (verdict.correct()) {
            p.sendMessage(GameService.text("&a✓ &f" + wordWas + " &a— banked "
                    + Money.fmt(r.earnedCents() - before) + " &7(total " + Money.fmt(r.earnedCents()) + ")"));
        } else if (r.kid()) {
            p.sendMessage(GameService.text("&eNot that one — try again!"));
        } else {
            p.sendMessage(GameService.text("&c✗ It was &f" + wordWas + "&c."));
        }
        if (r.finished()) {
            svc.completeScramble(p, pd, r);
            closeCardIf(p, "scramble");
        } else {
            refreshCard(p, "scramble");
            if (!r.kid()) promptInChat(p, "scramble");
        }
    }

    private void answerRiddle(Player p, PlayerData pd, String response) {
        GameService svc = plugin.service();
        pd.riddleRoll(svc.today());
        var verdict = plugin.textLoop().answer(p.getUniqueId().toString(), "riddle", response);
        svc.store().save(pd);
        if (verdict.correct()) {
            svc.completeRiddle(p, pd);
        } else if (pd.riddleAttempts >= RiddleText.MAX_ATTEMPTS) {
            svc.clearInput(p.getUniqueId());
            p.sendMessage(GameService.text("&cOut of attempts for today — the answer stays a secret until tomorrow's riddle."));
        } else {
            p.sendMessage(GameService.text("&c✗ Not it — " + (RiddleText.MAX_ATTEMPTS - pd.riddleAttempts)
                    + " attempt(s) left today. Guess again, or type &fstop&7 to think it over."));
        }
    }

    /** Re-send the current prompt in chat for typed rounds (the card may be closed). */
    public void promptInChat(Player p, String game) {
        var prompt = plugin.textLoop().nextPrompt(p.getUniqueId().toString(), game, "");
        if (prompt.question().isEmpty()) return;
        String label = game.equals("quickmath") ? "&c&lQuick Math: &r&f" : "&e&lUnscramble: &r&f";
        p.sendMessage(GameService.text(label + prompt.question()));
    }

    private void refreshCard(Player p, String game) {
        Inventory top = p.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof BrainHolder h && h.view == BrainHolder.View.QUESTION
                && game.equals(h.game)) {
            plugin.views().fillQuestion(top, h, p);
        }
    }

    private void closeCardIf(Player p, String game) {
        Inventory top = p.getOpenInventory().getTopInventory();
        if (top.getHolder() instanceof BrainHolder h && h.view == BrainHolder.View.QUESTION
                && game.equals(h.game)) {
            h.done = true;
            p.closeInventory();
        }
    }
}
