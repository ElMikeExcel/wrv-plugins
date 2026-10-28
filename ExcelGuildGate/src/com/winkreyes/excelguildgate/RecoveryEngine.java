package com.winkreyes.excelguildgate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Reviewed administrative recovery only. Never constructs loot or a synthetic death event. */
public final class RecoveryEngine {
    public interface Host {
        Map<String,Object> state();
        Map<String,Map<String,Object>> journal();
        boolean available();
        boolean knownEnabled(String seal);
        boolean broken(String seal);
        Map<String,Object> bossSnapshot(String seal) throws Exception;
        Map<String,Object> bossApply(String operationId,String digest,String seal,Map<String,Object> expected) throws Exception;
        /** Must durably commit receipt and optional seal grant together BEFORE publishing memory. */
        void commit(Map<String,Map<String,Object>> journal,String grantSeal,long now) throws IOException;
    }
    private record Plan(String actor,String boot,long expires,Map<String,Object> receipt) {}
    private final Host host;
    private final String boot=UUID.randomUUID().toString();
    private final Map<String,Plan> plans=new HashMap<>();
    public RecoveryEngine(Host host){this.host=host;}
    private static Map<String,Object> out(String status,String reason){
        Map<String,Object> m=new LinkedHashMap<>();m.put("status",status);m.put("reason",reason);return m;
    }
    @SuppressWarnings("unchecked")
    public static Object copy(Object x){
        if(x instanceof Map<?,?> m){Map<String,Object> c=new LinkedHashMap<>();m.forEach((k,v)->c.put(String.valueOf(k),copy(v)));return c;}
        if(x instanceof List<?> l){List<Object> c=new ArrayList<>();l.forEach(v->c.add(copy(v)));return c;}
        if(x==null||x instanceof String||x instanceof Number||x instanceof Boolean)return x;
        throw new IllegalArgumentException("unsupported persisted value");
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> map(Object x){return (Map<String,Object>)copy(x);}
    public static String canonical(Object x){
        if(x instanceof Map<?,?> m){TreeMap<String,Object> s=new TreeMap<>();m.forEach((k,v)->s.put(String.valueOf(k),v));StringBuilder b=new StringBuilder("{");s.forEach((k,v)->b.append(canonical(k)).append(canonical(v)));return b.append('}').toString();}
        if(x instanceof List<?> l){StringBuilder b=new StringBuilder("[");l.forEach(v->b.append(canonical(v)));return b.append(']').toString();}
        if(x==null)return "null;";
        String s=String.valueOf(x);return x.getClass().getSimpleName().equals("String")?"s"+s.length()+":"+s:"v"+s.length()+":"+s;
    }
    public static String digest(Object value){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical(value).getBytes(StandardCharsets.UTF_8)));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    private static String manifestDigest(Map<String,Object> r){
        Map<String,Object> m=new LinkedHashMap<>(r);m.remove("digest");m.remove("status");m.remove("completedAt");return digest(m);
    }
    private static boolean uuid(Object value){try{UUID.fromString(String.valueOf(value));return true;}catch(Exception e){return false;}}
    public static boolean validReceipt(String key,Map<String,Object> r){
        return key.matches("[A-Za-z0-9_-]{1,64}") && key.equals(r.get("caseId")) && r.get("seal") instanceof String && uuid(r.get("operationId"))
            && r.get("reason") instanceof String reason && !reason.isBlank() && reason.length()<=500
            && r.get("creator") instanceof String creator && !creator.isBlank() && r.get("createdAt") instanceof Number
            && r.get("beforeState") instanceof String && r.get("expectedBoss") instanceof Map
            && ("GRANT".equals(r.get("globalAction"))||"PRESERVE".equals(r.get("globalAction")))
            && ("INTENT".equals(r.get("status"))||"COMPLETED".equals(r.get("status")))
            && manifestDigest(r).equals(r.get("digest"));
    }
    private boolean usable(Map<String,Object> s,String seal){
        return s!=null && s.get("protocol") instanceof Number n && n.doubleValue()==1.0
            && seal.equals(s.get("seal")) && Boolean.TRUE.equals(s.get("enabled"))
            && Boolean.FALSE.equals(s.get("active")) && s.get("deadline") instanceof Number d && d.doubleValue()==d.longValue() && d.longValue()>=0
            && s.get("generation") instanceof String g && !g.isBlank()
            && s.get("stateToken") instanceof String t && t.matches("[0-9a-f]{64}") && s.get("reason") instanceof String;
    }
    public Map<String,Object> preview(String actor,boolean authorized,String seal,String caseId,String reason,long now){
        if(!authorized)return out("DENIED","Dedicated recovery permission required.");
        if(!host.available())return out("BLOCKED","Persistence uncertain or recovery state invalid; reload/reconcile first.");
        if(actor==null||actor.isBlank()||caseId==null||!caseId.matches("[A-Za-z0-9_-]{1,64}")||reason==null||reason.isBlank()||reason.length()>500)
            return out("INVALID","Case ID and explicit reason required; case ID max 64 safe characters, reason max 500.");
        if(!host.knownEnabled(seal))return out("INVALID","Unknown or disabled seal.");
        Map<String,Object> r=host.journal().get(caseId);
        if(r!=null){
            if(!validReceipt(caseId,r))return out("BLOCKED","Invalid persisted receipt.");
            if(!seal.equals(r.get("seal"))||!reason.equals(r.get("reason")))return out("CONFLICT","Case is bound to its original seal and reason.");
            if("COMPLETED".equals(r.get("status")))return out("COMPLETED_HISTORY","Already completed; later revoke does not authorize regrant.");
            if(!digest(host.state()).equals(r.get("beforeState")))return out("STALE","Global seal state changed since intent; manual review required.");
            r=map(r);
        }else{
            if(host.broken(seal)&&"raid".equals(seal))return out("ALREADY_BROKEN","Raid already complete; no BossTrial cooldown to reconcile.");
            Map<String,Object> expected=new LinkedHashMap<>();
            if(!"raid".equals(seal)){
                try{expected=host.bossSnapshot(seal);}catch(Exception e){return out("UNAVAILABLE","BossTrial recovery API unavailable.");}
                if(!usable(expected,seal))return out("BLOCKED","BossTrial not ready: "+expected.getOrDefault("reason","invalid protocol/active encounter"));
            }
            r=new LinkedHashMap<>();r.put("caseId",caseId);r.put("seal",seal);r.put("reason",reason);r.put("operationId",UUID.randomUUID().toString());
            r.put("creator",actor);r.put("globalAction",host.broken(seal)?"PRESERVE":"GRANT");r.put("createdAt",now);r.put("beforeState",digest(host.state()));r.put("expectedBoss",map(expected));r.put("status","INTENT");r.put("digest",manifestDigest(r));
        }
        // One fresh plan per case; older unconfirmed tokens cannot race different operation IDs.
        final String chosenCase=caseId;plans.entrySet().removeIf(e->e.getValue().expires<now||chosenCase.equals(e.getValue().receipt.get("caseId")));
        if(plans.size()>=128)return out("BLOCKED","Too many outstanding previews.");
        String token=UUID.randomUUID().toString();plans.put(token,new Plan(actor,boot,now+300000,map(r)));
        Map<String,Object> result=out("PREVIEW","Reviewed global win only; selected boss cooldown only; no items, XP, rewards, narration or fight replay.");
        result.put("token",token);result.put("expiresAt",now+300000);result.put("seal",seal);result.put("caseId",caseId);result.put("digest",r.get("digest"));result.put("resume",host.journal().containsKey(caseId));
        result.put("reviewReason",r.get("reason"));result.put("grantWillChange","GRANT".equals(r.get("globalAction")));result.put("currentlyBroken",host.broken(seal));result.put("globalAction",r.get("globalAction"));result.put("bossExpected",copy(r.get("expectedBoss")));return result;
    }
    public Map<String,Object> confirm(String actor,boolean authorized,String token,long now){
        if(!authorized)return out("DENIED","Dedicated recovery permission required.");
        Plan p=plans.get(token);
        if(p==null||!p.boot.equals(boot)||now>=p.expires)return out("EXPIRED","Preview again; tokens do not survive reload/restart.");
        if(!p.actor.equals(actor))return out("DENIED","Token belongs to another administrator.");
        plans.remove(token);
        if(!host.available())return out("BLOCKED","Persistence uncertain; reload/reconcile first.");
        Map<String,Object> r=map(p.receipt);String id=(String)r.get("caseId"),seal=(String)r.get("seal");
        if(!validReceipt(id,r))return out("INVALID","Manifest digest changed.");
        if(!digest(host.state()).equals(r.get("beforeState")))return out("STALE","Global seal state changed; no automatic regrant.");
        Map<String,Map<String,Object>> journal=host.journal();Map<String,Object> old=journal.get(id);
        if(old!=null && (!r.get("digest").equals(old.get("digest"))||!r.get("operationId").equals(old.get("operationId"))))return out("CONFLICT","Case journal changed.");
        if(old!=null && "COMPLETED".equals(old.get("status")))return out("COMPLETED_HISTORY","Already completed; no effects repeated.");
        // Recheck live Boss state before journaling a NEW intent. Persisted intent must use
        // original expectation so BossTrial can return receipt-based REPLAYED, never rebase.
        if(old==null && !"raid".equals(seal)){
            try{Map<String,Object> current=host.bossSnapshot(seal);
                if(!usable(current,seal)||!digest(current).equals(digest(r.get("expectedBoss"))))return out("STALE","BossTrial state changed after preview.");
            }catch(Exception e){return out("UNAVAILABLE","BossTrial unavailable; no intent applied.");}
        }
        if(old==null){
            journal.put(id,map(r));
            try{host.commit(journal,null,now);}catch(IOException e){return out("IO_FAILURE","Intent persistence not acknowledged; no Boss call. Reload/reconcile.");}
        }
        if(!"raid".equals(seal)){
            Map<String,Object> result;
            try{result=host.bossApply((String)r.get("operationId"),(String)r.get("digest"),seal,map(r.get("expectedBoss")));}
            catch(Exception e){return out("PENDING","Boss result unknown; retain intent and preview again, never blindly replay a different case.");}
            String status=String.valueOf(result.get("status"));
            if(!"APPLIED".equals(status)&&!"REPLAYED".equals(status))return out("PENDING","BossTrial "+status+": "+result.get("reason"));
            if(!r.get("operationId").equals(result.get("operationId")) || !usable(result,seal) || ((Number)result.get("deadline")).longValue()!=0)
                return out("PENDING","Boss response identity/protocol/current state mismatch; no global grant.");
        }
        if(!host.available()||!digest(host.state()).equals(r.get("beforeState")))return out("PENDING","Global state changed after Boss result; explicit review required.");
        Map<String,Object> done=map(r);done.put("status","COMPLETED");done.put("completedAt",now);journal=host.journal();journal.put(id,done);
        try{host.commit(journal,"GRANT".equals(r.get("globalAction"))?seal:null,now);}catch(IOException e){return out("IO_FAILURE","Completion persistence uncertain; no success claimed. Reload/reconcile.");}
        return out("COMPLETED","Reviewed global seal state and selected boss cooldown reconciled. No rewards delivered.");
    }
}
