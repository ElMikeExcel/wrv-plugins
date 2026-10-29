package com.winkreyes.excelwandbars;

import com.winkreyes.excelwandbars.core.CrouchHoldGate;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.ItemUseTransaction;
import org.cloudburstmc.protocol.bedrock.data.inventory.transaction.InventoryTransactionType;
import org.cloudburstmc.protocol.bedrock.packet.InventoryTransactionPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayerAuthInputPacket;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import java.util.UUID;
import java.util.function.BiConsumer;

/** Observes protocol-qualified held item use without consuming or modifying any Geyser packet. */
public final class GeyserHoldHook {
    private static AuthWrapper auth;
    private static UseWrapper use;
    private static final CrouchHoldGate gate = new CrouchHoldGate();
    private GeyserHoldHook() {}
    @SuppressWarnings({"unchecked","rawtypes"})
    public static synchronized String install(BiConsumer<UUID,Integer> callback) {
        if (auth != null || use != null) {
            if(auth!=null&&use!=null&&Registries.BEDROCK_PACKET_TRANSLATORS.get(PlayerAuthInputPacket.class)==auth&&Registries.BEDROCK_PACKET_TRANSLATORS.get(InventoryTransactionPacket.class)==use){auth.callback=callback;use.callback=callback;return "already";}
            if(auth!=null)auth.callback=null;if(use!=null)use.callback=null;gate.clear();
            return "replaced by another plugin; left alone";
        }
        PacketTranslator a=Registries.BEDROCK_PACKET_TRANSLATORS.get(PlayerAuthInputPacket.class);
        PacketTranslator u=Registries.BEDROCK_PACKET_TRANSLATORS.get(InventoryTransactionPacket.class);
        if(a==null||u==null)return "no Geyser translator";
        if(a.getClass().getName().equals(AuthWrapper.class.getName())||u.getClass().getName().equals(UseWrapper.class.getName()))return "wrapped by an older copy";
        AuthWrapper nextAuth=new AuthWrapper(a,callback);UseWrapper nextUse=new UseWrapper(u,callback);
        try {
            ((org.geysermc.geyser.registry.AbstractMappedRegistry)Registries.BEDROCK_PACKET_TRANSLATORS).register(PlayerAuthInputPacket.class,nextAuth);
            ((org.geysermc.geyser.registry.AbstractMappedRegistry)Registries.BEDROCK_PACKET_TRANSLATORS).register(InventoryTransactionPacket.class,nextUse);
        } catch(Throwable failed) {
            nextAuth.callback=null;nextUse.callback=null;
            if(Registries.BEDROCK_PACKET_TRANSLATORS.get(PlayerAuthInputPacket.class)==nextAuth)((org.geysermc.geyser.registry.AbstractMappedRegistry)Registries.BEDROCK_PACKET_TRANSLATORS).register(PlayerAuthInputPacket.class,a);
            if(Registries.BEDROCK_PACKET_TRANSLATORS.get(InventoryTransactionPacket.class)==nextUse)((org.geysermc.geyser.registry.AbstractMappedRegistry)Registries.BEDROCK_PACKET_TRANSLATORS).register(InventoryTransactionPacket.class,u);
            throw failed;
        }
        auth=nextAuth;use=nextUse;return "installed";
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    public static synchronized String uninstall() {
        if(auth!=null){auth.callback=null;if(Registries.BEDROCK_PACKET_TRANSLATORS.get(PlayerAuthInputPacket.class)==auth)((org.geysermc.geyser.registry.AbstractMappedRegistry)Registries.BEDROCK_PACKET_TRANSLATORS).register(PlayerAuthInputPacket.class,auth.original);}
        if(use!=null){use.callback=null;if(Registries.BEDROCK_PACKET_TRANSLATORS.get(InventoryTransactionPacket.class)==use)((org.geysermc.geyser.registry.AbstractMappedRegistry)Registries.BEDROCK_PACKET_TRANSLATORS).register(InventoryTransactionPacket.class,use.original);}
        auth=null;use=null;gate.clear();return "restored if still owned";
    }
    public static void reset(UUID id) { gate.reset(id); }
    public static boolean consume(UUID id, int slot) { return gate.consume(id,slot,System.nanoTime()/1_000_000L); }
    static void note(BiConsumer<UUID,Integer> cb, UUID id, int action, int slot, boolean crouched, ItemUseTransaction.TriggerType trigger) {
        // Only air-use, never mining, interacting with containers or attacks.
        if(action!=1){gate.reset(id);return;}
        long now=System.nanoTime()/1_000_000L;
        boolean fire=gate.accept(id,slot,crouched,trigger==ItemUseTransaction.TriggerType.PLAYER_INPUT,trigger==ItemUseTransaction.TriggerType.SIMULATION_TICK,now);
        if(fire&&cb!=null)cb.accept(id,slot);
    }
    static final class AuthWrapper extends PacketTranslator<PlayerAuthInputPacket> {
        final PacketTranslator<PlayerAuthInputPacket> original;volatile BiConsumer<UUID,Integer> callback;
        AuthWrapper(PacketTranslator<PlayerAuthInputPacket> original,BiConsumer<UUID,Integer> callback){this.original=original;this.callback=callback;}
        public void translate(GeyserSession s,PlayerAuthInputPacket p){
            // Let Geyser apply its toggle/START/STOP/input-mode rules first. Missing
            // SNEAKING/SNEAK_DOWN flags do not mean release on a touch toggle client.
            original.translate(s,p);
            try{BiConsumer<UUID,Integer> cb=callback;if(cb!=null){UUID id=s.javaUuid();boolean crouched=s.isSneaking();
                if(!crouched)gate.reset(id);
                ItemUseTransaction t=p.getItemUseTransaction();
                if(t!=null&&p.getInputData().contains(PlayerAuthInputData.PERFORM_ITEM_INTERACTION))note(cb,id,t.getActionType(),t.getHotbarSlot(),crouched,t.getTriggerType());
            }}catch(Throwable ignored){}
        }
        public boolean shouldExecuteInEventLoop(){return original.shouldExecuteInEventLoop();}
    }
    static final class UseWrapper extends PacketTranslator<InventoryTransactionPacket> {
        final PacketTranslator<InventoryTransactionPacket> original;volatile BiConsumer<UUID,Integer> callback;
        UseWrapper(PacketTranslator<InventoryTransactionPacket> original,BiConsumer<UUID,Integer> callback){this.original=original;this.callback=callback;}
        public void translate(GeyserSession s,InventoryTransactionPacket p){
            try{BiConsumer<UUID,Integer> cb=callback;if(cb!=null){UUID id=s.javaUuid();if(p.getTransactionType()==InventoryTransactionType.ITEM_USE)note(cb,id,p.getActionType(),p.getHotbarSlot(),s.isSneaking(),p.getTriggerType());else gate.reset(id);}}
            catch(Throwable ignored){}finally{original.translate(s,p);}
        }
        public boolean shouldExecuteInEventLoop(){return original.shouldExecuteInEventLoop();}
    }
}
