/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.ui;
import java.util.*;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
/** Deterministic item surrogate tests allocation, not Paper item serialization. */
public final class InventoryPlanTest {
 static int checks;
 static final class Stack extends ItemStack {
  final String metadata;final int limit;int count;
  Stack(String metadata,int count,int limit){super();this.metadata=metadata;this.count=count;this.limit=limit;}
  @Override public Material getType(){return Material.STONE;}
  @Override public int getAmount(){return count;}
  @Override public void setAmount(int n){count=n;}
  @Override public int getMaxStackSize(){return limit;}
  @Override public boolean isSimilar(ItemStack other){return other instanceof Stack s&&s.metadata.equals(metadata)&&s.limit==limit;}
  @Override public Stack clone(){return new Stack(metadata,count,limit);}
 }
 static void c(boolean b){if(!b)throw new AssertionError("check "+checks);checks++;}
 public static void main(String[] args){
  var old=new Stack("signed",60,64);var reward=new Stack("signed",9,64);
  var plan=LedgerClaimGateway.plan(new ItemStack[]{old,null},List.of(reward));
  c(plan[0].getAmount()==64&&plan[1].getAmount()==5);c(old.getAmount()==60&&reward.getAmount()==9);
  c(LedgerClaimGateway.plan(new ItemStack[]{old},List.of(reward))==null);c(old.getAmount()==60);
  c(LedgerClaimGateway.plan(new ItemStack[]{old},List.of(new Stack("different-owner",1,64)))==null);
  var unique=LedgerClaimGateway.plan(new ItemStack[]{null,null},List.of(new Stack("weapon",2,1)));c(unique[0].getAmount()==1&&unique[1].getAmount()==1);
  c(LedgerClaimGateway.plan(new ItemStack[]{null},List.of(new Stack("weapon",2,1)))==null);
  var combined=LedgerClaimGateway.plan(new ItemStack[]{null},List.of(new Stack("same",30,64),new Stack("same",34,64)));c(combined[0].getAmount()==64);
  c(LedgerClaimGateway.plan(new ItemStack[]{null},List.of(new Stack("same",30,64),new Stack("same",35,64)))==null);
  for(int space=0;space<130;space++)for(int amount=1;amount<130;amount++){
   ItemStack[] inv={new Stack("same",64-Math.min(space,64),64),new Stack("same",64-Math.max(space-64,0),64)};
   if(space>128)continue;
   var out=LedgerClaimGateway.plan(inv,List.of(new Stack("same",amount,64)));
   c((out!=null)==(amount<=space));
   if(out!=null)c(out[0].getAmount()+out[1].getAmount()==128-space+amount);
  }
  System.out.println("InventoryPlanTest: "+checks+" checks passed (surrogate stacks)");
 }
}
