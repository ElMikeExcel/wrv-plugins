package com.winkreyes.excelexpansions.core;
import java.util.List;
public record ManifestRow(String id,String slug,String displayName,String unitType,String phase,int phaseOrder,
 Integer bandMin,Integer bandMax,String bandKind,String bandSource,boolean seasonal,String seasonWindowOpens,
 String seasonWindowLabel,String homePhase,List<String> requires,String releasedState,
 String parkedState,String bandNote,String bandText,List<String> contentFiles){
 public boolean support(){return "support".equals(unitType);}
 public boolean unassigned(){return "unassigned".equals(phase);}
 public boolean evaluable(){return bandSource!=null && !bandSource.contains("PENDING-CONTENTS-CHECK");}
 public boolean dynamic(){return "dynamic".equals(bandKind);}
}
