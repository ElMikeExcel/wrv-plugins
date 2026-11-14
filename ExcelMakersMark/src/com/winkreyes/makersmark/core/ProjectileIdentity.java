package com.winkreyes.makersmark.core;
import java.util.*;
/** Capture a preexisting identity only; material, slot and later held items are not identity evidence. */
public final class ProjectileIdentity {
 private ProjectileIdentity(){}
 public static Optional<String> capture(String sourceId){
  if(sourceId==null)return Optional.empty();
  try {UUID id=UUID.fromString(sourceId);return id.toString().equals(sourceId)?Optional.of(sourceId):Optional.empty();}
  catch(IllegalArgumentException bad){return Optional.empty();}
 }
 public static int uniqueIndex(List<String> ids,String expected){
  if(capture(expected).isEmpty())return -1;int result=-1;
  for(int i=0;i<ids.size();i++)if(expected.equals(ids.get(i))){if(result>=0)return -1;result=i;}return result;
 }
}
