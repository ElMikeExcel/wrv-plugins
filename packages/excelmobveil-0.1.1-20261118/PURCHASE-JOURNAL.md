# Purchase journal

An append-only force-to-disk purchases.journal records transaction/player UUID, VEIL product, price, phase and CRC. Phases include WITHDRAW_INTENT, WITHDRAWN, REFUND_INTENT, RECONCILE, COMPLETED, REFUNDED and DECLINED. Unfinished transactions lock that player; corrupt records fail purchases closed. No automatic replay occurs.

This is not an atomic transaction between Vault and playerdata. Player.saveData is void and may swallow disk errors. Stop purchases, preserve journal/playerdata/provider evidence, and have an authorized operator reconcile ambiguous charges, delivery and refunds. Never blindly charge/refund again or delete the only audit copy to clear locks.
