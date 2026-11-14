package com.winkreyes.excelexpansions.core;
import java.io.*; import java.nio.charset.StandardCharsets; import java.util.*;
/** Minimal parser for the flat releases.yml subset this plugin ships (scalar fields + inline requires / content_files lists). */
public final class ManifestLoader {
 public static List<ManifestRow> parse(String text){
  List<ManifestRow> out=new ArrayList<>(); Map<String,String> cur=null;
  for(String raw: text.split("\n")){ String l=raw.strip(); if(l.startsWith("- id:")){ if(cur!=null) out.add(row(cur)); cur=new LinkedHashMap<>(); put(cur,l.substring(2)); }
   else if(cur!=null && l.contains(":")) put(cur,l); }
  if(cur!=null) out.add(row(cur)); return out; }
 private static void put(Map<String,String> m,String kv){int i=kv.indexOf(':'); if(i>0) m.put(kv.substring(0,i).trim(), unq(kv.substring(i+1).trim()));}
 private static String unq(String v){ if(v==null||v.equals("null")) return null; if(v.startsWith("\"")&&v.endsWith("\"")&&v.length()>=2) return v.substring(1,v.length()-1).replace("\\\"","\"").replace("\\\\","\\"); return v; }
 private static Integer num(String v){ return v==null?null:Integer.valueOf(v); }
 private static List<String> list(Map<String,String> m,String key){ List<String> out=new ArrayList<>(); String r=m.get(key);
   if(r!=null && r.length()>2) for(String s: r.substring(1,r.length()-1).split(",")){ String t=unq(s.trim()); if(t!=null&&!t.isBlank()) out.add(t); }
   return List.copyOf(out); }
 private static ManifestRow row(Map<String,String> m){
  return new ManifestRow(m.get("id"),m.get("slug"),m.get("display_name"),m.get("unit_type"),m.get("phase"),
   Integer.parseInt(m.get("phase_order")), num(m.get("band_min")), num(m.get("band_max")), m.get("band_kind"), m.get("band_source"),
   Boolean.parseBoolean(m.get("seasonal")), m.get("season_window_opens"), m.get("season_window_label"), m.get("home_phase"),
   list(m,"requires"), m.get("released_state"), m.get("parked_state"), m.get("band_note"), m.get("band_text"), list(m,"content_files")); }
 public static List<ManifestRow> load(InputStream in) throws IOException { return parse(new String(in.readAllBytes(),StandardCharsets.UTF_8)); }
}
