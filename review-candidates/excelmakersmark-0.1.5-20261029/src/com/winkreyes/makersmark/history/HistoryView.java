/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark.history;
import java.util.*;
/** Plain-text view models. Never pass returned player-authored names to a markup or command parser. */
public final class HistoryView {
    private HistoryView(){}
    public record Entry(String type,String fullName,long victories){}
    public record Page(int index,int pages,int distinct,List<Entry> entries){public Page{entries=List.copyOf(entries);}}
    public static Page page(HistoryLedger.State state,int index,int size){
        if(index<0||size<1||size>20)throw new IllegalArgumentException("Invalid page request");
        int total=state.names().size(),pages=Math.max(1,(total+size-1)/size);
        if(index>=pages)throw new IllegalArgumentException("Page no longer exists");
        List<Entry> result=state.names().entrySet().stream().skip((long)index*size).limit(size)
            .map(e->new Entry(e.getKey().kind(),e.getKey().name(),e.getValue())).toList();
        return new Page(index,pages,total,result);
    }
    /** Bounded tooltip is display only. Full names/counts remain in archive and paginated detail. */
    public static List<String> tooltip(HistoryLedger.State state){
        List<String> lines=new ArrayList<>();int shown=0;
        for(var e:state.names().entrySet()){
            if(shown==3)break;
            lines.add("Slain: "+shorten(e.getKey().name(),36)+" ×"+e.getValue());shown++;
        }
        int rest=state.names().size()-shown;if(rest>0)lines.add("+"+rest+" more named accomplishments");
        return List.copyOf(lines);
    }
    private static String shorten(String text,int points){int n=text.codePointCount(0,text.length());return n<=points?text:text.substring(0,text.offsetByCodePoints(0,points-1))+"…";}
}
