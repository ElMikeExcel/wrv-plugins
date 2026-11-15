/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.ui;
import java.util.*;
public final class ClaimSessionTest {
 static int n;static void check(boolean ok){if(!ok)throw new AssertionError("check "+n);n++;}
 public static void main(String[] args){
 UUID owner=UUID.randomUUID(),stranger=UUID.randomUUID();var entries=new ArrayList<ClaimGateway.Pending>();
 for(int i=0;i<100;i++)entries.add(new ClaimGateway.Pending("claim-"+i,"Reward"));
 var s=new ClaimSession(owner,entries,0);check(s.visible().size()==45);
 for(int slot=-10;slot<100;slot++)check(s.request(stranger,slot,true).isEmpty());
 for(int slot=-10;slot<100;slot++)check(s.request(owner,slot,false).isEmpty());
 for(int slot=45;slot<100;slot++)check(s.request(owner,slot,true).isEmpty());
 check(s.request(owner,-1,true).isEmpty());check(s.request(owner,3,true).orElseThrow().equals("claim-3"));
 check(s.request(owner,3,true).isEmpty());check(s.request(owner,4,true).isEmpty());
 var page=new ClaimSession(owner,entries,1);check(page.visible().get(0).id().equals("claim-45"));
 var last=new ClaimSession(owner,entries,999);check(last.page()==2&&last.visible().size()==10);check(last.request(owner,10,true).isEmpty());
 check(last.request(owner,9,true).orElseThrow().equals("claim-99"));
 var closed=new ClaimSession(owner,entries,0);closed.close();check(closed.request(owner,0,true).isEmpty());
 var empty=new ClaimSession(owner,List.of(),-1);check(empty.page()==0&&empty.visible().isEmpty()&&empty.request(owner,0,true).isEmpty());
 entries.clear();check(s.visible().size()==45);
 try {new ClaimSession(owner,List.of(new ClaimGateway.Pending("same","A"),new ClaimGateway.Pending("same","B")),0);throw new AssertionError();}catch(IllegalArgumentException expected){n++;}
 System.out.println("ClaimSessionTest: "+n+" checks passed");
 }
}
