package com.winkreyes.makersmark.history;
import java.io.IOException;
import java.util.*;
import org.bukkit.inventory.ItemStack;
/** Single-item compare-and-set adapter. No event registration, recipe merges or Magic item support. */
public final class HistorySlotTransaction {
    public interface Slot {
        /** Clone of the authoritative current stack, not a cached event stack. */
        ItemStack snapshot();
        /** On the server thread, replace only if current stack is still exactly expected. */
        boolean compareAndSet(ItemStack expected,ItemStack replacement);
        /** Count actual observed copies in the authoritative ownership scope; not global absence proof. */
        int observedCopies(UUID item);
    }
    private final HistoryLedger ledger;private final HistoryItemTokens codec;
    public HistorySlotTransaction(HistoryLedger ledger,HistoryItemTokens codec){this.ledger=ledger;this.codec=codec;}
    /** Preview remains entirely outside this method. Call only for a verified committed action. */
    public void apply(Slot slot,HistoryLedger.Plan plan)throws IOException {
        Objects.requireNonNull(slot);Objects.requireNonNull(plan);
        if(ledger.status(plan.receipt())==HistoryLedger.Status.COMMITTED){ledger.prepare(plan);return;} // Replay never writes an old item snapshot.
        if(plan.inputs().size()>1)throw new IllegalArgumentException("Multi-item merges need a separate consumption adapter");
        ItemStack before=slot.snapshot();var token=codec.read(before);
        if(plan.inputs().isEmpty()){
            if(token.isPresent())throw new IllegalStateException("Cannot re-register archived item");
        }else{
            var expected=plan.inputs().getFirst();var actual=token.orElseThrow(()->new IllegalStateException("Missing input token"));
            if(!expected.item().equals(actual.item())||expected.revision()!=actual.revision()||!plan.output().item().equals(actual.item()))throw new IllegalStateException("Wrong single-item input");
            ledger.reconcile(actual.item(),actual.revision(),actual.receipt(),slot.observedCopies(actual.item()));
        }
        ItemStack replacement=codec.resultCopy(before,plan);
        ledger.prepare(plan); // Never mutate item before durable preparation.
        if(!slot.compareAndSet(before,replacement))throw new IllegalStateException("Item changed; prepared receipt needs reconciliation");
        ItemStack after=slot.snapshot();var result=codec.read(after).orElseThrow(()->new IllegalStateException("Result token missing"));
        if(!replacement.equals(after)||!result.item().equals(plan.output().item())||result.revision()!=plan.output().revision()||!result.receipt().equals(plan.receipt()))throw new IllegalStateException("Result changed; prepared receipt needs reconciliation");
        int copies=slot.observedCopies(result.item());
        if(copies!=1){ledger.reconcile(result.item(),result.revision(),result.receipt(),copies);throw new IllegalStateException("Ambiguous result");}
        ledger.markPhysicalVerified(plan.receipt(),result.item(),result.revision(),RecoveryContext.fingerprint(after),1,true);
        ledger.commit(plan.receipt(),result.item(),result.revision(),true,true);
    }
    /** Resume only a proven single-item result after a crash. Never replay an inventory snapshot. */
    public void finishPending(Slot slot,HistoryLedger.Plan plan)throws IOException {
        if(plan.inputs().size()>1||plan.inputs().stream().anyMatch(e->!e.item().equals(plan.output().item())))throw new IllegalArgumentException("Not an in-place single-item transaction");
        if(ledger.status(plan.receipt())==null)throw new IllegalStateException("Unknown pending receipt");
        ledger.prepare(plan); // Conflicting caller plan cannot reuse an existing receipt.
        ItemStack current=slot.snapshot();
        if(current==null||current.getType().isAir()||current.getAmount()!=1)throw new IllegalStateException("Invalid pending item stack");
        var result=codec.read(current).orElseThrow(()->new IllegalStateException("Missing pending result"));
        if(!result.item().equals(plan.output().item())||result.revision()!=plan.output().revision()||!result.receipt().equals(plan.receipt()))throw new IllegalStateException("Pending result does not match");
        int copies=slot.observedCopies(result.item());
        if(copies!=1){ledger.reconcile(result.item(),result.revision(),result.receipt(),copies);throw new IllegalStateException("Ambiguous pending result");}
        ledger.commit(plan.receipt(),result.item(),result.revision(),true,true);
    }

}
