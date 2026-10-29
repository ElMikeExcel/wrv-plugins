/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark.history;
import java.io.IOException;
import java.util.*;
import org.bukkit.inventory.ItemStack;
/** Post-vanilla-result adapter. Never used by PrepareAnvil/PrepareSmithing/recipe preview events. */
public final class HistoryMergeTransaction {
    public interface CommittedResult extends HistorySlotTransaction.Slot {
        /** Re-evaluate immutable pre-event + fresh post-event observations through MergeEvidence. */
        boolean consumptionProven(HistoryLedger.Plan plan);
    }
    private final HistoryLedger ledger;private final HistoryItemTokens codec;
    public HistoryMergeTransaction(HistoryLedger ledger,HistoryItemTokens codec){this.ledger=ledger;this.codec=codec;}
    public void apply(CommittedResult result,ItemStack expectedActualResult,HistoryLedger.Plan plan)throws IOException {
        if(ledger.status(plan.receipt())==HistoryLedger.Status.COMMITTED){ledger.prepare(plan);return;}
        if(plan.inputs().isEmpty()||plan.inputs().size()>2)throw new IllegalArgumentException("Expected one or two tracked gear inputs");
        if(!result.consumptionProven(plan))throw new IllegalStateException("Input consumption/result transfer not proven");
        ItemStack before=result.snapshot();
        if(before==null||!before.equals(expectedActualResult))throw new IllegalStateException("Actual result changed");
        ItemStack replacement=codec.resultCopy(before,plan);
        ledger.prepare(plan);
        if(!result.compareAndSet(before,replacement))throw new IllegalStateException("Result moved; pending receipt retained");
        ItemStack after=result.snapshot();var token=codec.read(after).orElseThrow(()->new IllegalStateException("Result token lost"));
        if(!replacement.equals(after)||!token.item().equals(plan.output().item())||token.revision()!=plan.output().revision()||!token.receipt().equals(plan.receipt()))throw new IllegalStateException("Wrong result; pending receipt retained");
        int copies=result.observedCopies(token.item());
        if(copies!=1){ledger.reconcile(token.item(),token.revision(),token.receipt(),copies);throw new IllegalStateException("Ambiguous merge result");}
        if(!result.consumptionProven(plan))throw new IllegalStateException("Consumption evidence changed; pending receipt retained");
        ledger.markPhysicalVerified(plan.receipt(),token.item(),token.revision(),RecoveryContext.fingerprint(after),1,true);
        ledger.commit(plan.receipt(),token.item(),token.revision(),true,true);
    }
}
