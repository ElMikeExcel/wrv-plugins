package com.winkreyes.rewardguard;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public final class ProvenanceTests {
    private static int assertions;
    private static void check(boolean condition) { assertions++; if (!condition) throw new AssertionError("check " + assertions); }
    private record Value(Object type,Object data) { }
    private static PersistentDataContainer container(Map<Object,Value> values) {
        return (PersistentDataContainer) Proxy.newProxyInstance(ProvenanceTests.class.getClassLoader(),
            new Class<?>[] {PersistentDataContainer.class}, (proxy,method,args)-> {
                String n=method.getName();
                if(n.equals("set")){values.put(args[0],new Value(args[1],args[2]));return null;}
                if(n.equals("get")){var v=values.get(args[0]);return v!=null&&v.type==args[1]?v.data:null;}
                if(n.equals("has")){var v=values.get(args[0]);return v!=null&&(args.length==1||v.type==args[1]);}
                if(n.equals("getKeys"))return values.keySet();
                if(n.equals("isEmpty"))return values.isEmpty();
                if(n.equals("remove")){values.remove(args[0]);return null;}
                if(n.equals("toString"))return "test PDC";
                throw new UnsupportedOperationException(n);
            });
    }
    public static void main(String[] args) {
        var store=new ProvenanceStore();
        for(String r:new String[]{"NATURAL","DEFAULT","CHUNK_GEN","TRIAL_SPAWNER","RAID","BREEDING","COMMAND","CUSTOM"}){
            Map<Object,Value> map=new HashMap<>();var pdc=container(map);store.recordSpawn(pdc,r);
            check(store.inspect(pdc).decision().equals("UNKNOWN"));
            if(r.equals("NATURAL")||r.equals("DEFAULT")||r.equals("CHUNK_GEN"))check(map.isEmpty());
        }
        for(String r:new String[]{"SPAWNER","SPAWNER_EGG","NETHER_PORTAL"}){
            Map<Object,Value> map=new HashMap<>();var pdc=container(map);store.recordSpawn(pdc,r);
            check(store.inspect(pdc).decision().equals("DENY"));
            var serializedCopy=container(new HashMap<>(map));check(store.inspect(serializedCopy).decision().equals("DENY"));
            for(int i=0;i<10;i++){
                var child=container(new HashMap<>());store.copy(serializedCopy,child);
                check(store.inspect(child).decision().equals("DENY"));
                store.recordSpawn(child,"NATURAL");check(store.inspect(child).decision().equals("DENY"));
                serializedCopy=child;
            }
        }
        var source=container(new HashMap<>());var target=container(new HashMap<>());
        store.recordSpawn(target,"SPAWNER");store.copy(source,target);check(store.inspect(target).decision().equals("DENY"));
        check(store.inspect(null).decision().equals("DENY"));
        Map<Object,Value> corrupt=new HashMap<>();corrupt.put(new NamespacedKey("excelrewardguard","denied_v1"),new Value(PersistentDataType.STRING,"unexpected"));
        check(store.inspect(container(corrupt)).decision().equals("DENY"));
        String[] result=FarmVerdict.unavailable().apiValue();check(result.length==3);check(result[0].equals("DENY"));check(result[1].equals("classification_unavailable"));check(result[2].equals("1"));
        check(!result.equals(FarmVerdict.unavailable().apiValue()));
        System.out.println("PASS "+assertions+" provenance policy/mock-PDC assertions (incl. NETHER_PORTAL denial); native runtime behavior is covered by the boot tests, reward vetoes by the signature and suppression suites");
    }
}
