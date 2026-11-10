package com.winkreyes.excelwandbars;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Excel: durable original-item provenance BEFORE attaching an independent practice identity.
 * No Bukkit calls, rank grants, player writes or claims that inherited levels were earned. */
final class PracticeIdentityJournal implements AutoCloseable {
 record Entry(String identity, String owner, String template, String raw, String provenance,
              Map<String,Integer> baseline,Set<String> allowedTemplates) {
  Entry { baseline=Map.copyOf(baseline);allowedTemplates=Set.copyOf(allowedTemplates); }
 }
 interface Fault { void at(String point)throws IOException; }
 private final Path path; private final int capacity; private final Fault fault;
 private final Thread thread=Thread.currentThread();
 private final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>();
 private FileChannel channel; private FileLock lock; private byte[] disk;
 private boolean closed,uncertain;
 PracticeIdentityJournal(Path path,int capacity)throws IOException {this(path,capacity,p->{});}
 PracticeIdentityJournal(Path path,int capacity,Fault fault)throws IOException {
  if(capacity<1)throw new IllegalArgumentException("capacity");
  this.path=path.toAbsolutePath();this.capacity=capacity;this.fault=Objects.requireNonNull(fault);
  Files.createDirectories(this.path.getParent());
  try {
   channel=FileChannel.open(this.path.resolveSibling(this.path.getFileName()+".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
   lock=channel.tryLock(); if(lock==null)throw new IOException("identity journal already owned");
   disk=Files.exists(this.path)?Files.readAllBytes(this.path):null;
   if(disk!=null)read(disk);
  }catch(Exception e){close();throw new IOException("identity journal unavailable",e);}
 }
 private void ready()throws IOException {
  if(closed||uncertain||Thread.currentThread()!=thread)throw new IOException("identity journal fenced");
  if(!Arrays.equals(disk,Files.exists(path)?Files.readAllBytes(path):null)){uncertain=true;throw new IOException("identity journal externally changed");}
 }
 Entry get(String identity)throws IOException {ready();return entries.get(identity);}
 /** Existing original provenance is immutable even if ordinary later item state changes. */
 Entry prepare(String identity,String owner,String template,String raw,String provenance,Map<String,Integer> baseline)throws IOException {
  ready();
  if(identity==null)identity=UUID.randomUUID().toString();
  Entry next=new Entry(identity,owner,template,raw,provenance,baseline,Set.of(template)); validate(next);
  Entry old=entries.get(identity);
  if(old!=null){
   if(old.owner()!=null&&!old.owner().equals(owner))throw new IOException("practice identity owner mismatch");
   return old;
  }
  if(entries.size()>=capacity)throw new IOException("identity journal capacity");
  var proposed=new LinkedHashMap<>(entries);proposed.put(identity,next);persist(proposed);return next;
 }
 /** A formerly unowned original can be claimed once, after exact native bound-owner verification. */
 Entry claimOwner(String identity,String owner)throws IOException {
  ready();uuid(owner);Entry old=entries.get(identity);if(old==null)throw new IOException("original provenance missing");
  if(old.owner()!=null){if(!old.owner().equals(owner))throw new IOException("practice identity owner mismatch");return old;}
  Entry next=new Entry(old.identity(),owner,old.template(),old.raw(),old.provenance(),old.baseline(),old.allowedTemplates());
  var proposed=new LinkedHashMap<>(entries);proposed.put(identity,next);persist(proposed);return next;
 }
 Entry authorizeTemplate(String identity,String current,String next)throws IOException {
  ready();Entry old=entries.get(identity);if(old==null||!old.allowedTemplates().contains(current)||next==null||!next.matches("[a-zA-Z0-9_.-]{1,200}"))throw new IOException("template transition authority unavailable");
  if(old.allowedTemplates().contains(next))return old;
  var allowed=new TreeSet<>(old.allowedTemplates());allowed.add(next);
  Entry updated=new Entry(old.identity(),old.owner(),old.template(),old.raw(),old.provenance(),old.baseline(),allowed);validate(updated);
  var proposed=new LinkedHashMap<>(entries);proposed.put(identity,updated);persist(proposed);return updated;
 }
 private static void uuid(String text)throws IOException {try{if(text==null||!UUID.fromString(text).toString().equalsIgnoreCase(text))throw new IllegalArgumentException();}catch(IllegalArgumentException e){throw new IOException("invalid owner",e);}}
 private static void validate(Entry e)throws IOException {
  if(e.identity()==null||e.identity().isBlank()||e.identity().length()>200||e.template()==null||!e.template().matches("[a-zA-Z0-9_.-]{1,200}")||e.raw()==null||e.raw().getBytes(StandardCharsets.UTF_8).length>1048576||e.provenance()==null||e.provenance().length()>300||e.baseline().size()>128)throw new IOException("invalid original provenance");
  uuid(e.identity());
  if(e.owner()!=null)uuid(e.owner());
  if(e.allowedTemplates().size()>16||!e.allowedTemplates().contains(e.template()))throw new IOException("invalid template lineage");for(String t:e.allowedTemplates())if(!t.matches("[a-zA-Z0-9_.-]{1,200}"))throw new IOException("invalid template lineage");
  for(var r:e.baseline().entrySet())if(!r.getKey().matches("[a-z0-9_.-]{1,200}")||r.getValue()==null||r.getValue()<1||r.getValue()>64)throw new IOException("invalid reviewed baseline");
 }
 private static void string(DataOutputStream out,String s)throws IOException {byte[] b=s.getBytes(StandardCharsets.UTF_8);out.writeInt(b.length);out.write(b);}
 private static String string(DataInputStream in,int max)throws IOException {int n=in.readInt();if(n<0||n>max)throw new IOException("invalid string length");byte[] b=in.readNBytes(n);if(b.length!=n)throw new EOFException();return new String(b,StandardCharsets.UTF_8);}
 private void persist(LinkedHashMap<String,Entry> proposed)throws IOException {
  ready();Path temp=null;
  try{
   fault.at("before-write");var bytes=new ByteArrayOutputStream();
   try(var out=new DataOutputStream(bytes)){
    out.writeUTF("EXCEL-PRACTICE-ORIGINAL-1");out.writeInt(proposed.size());
    for(var e:proposed.values()){
     validate(e);string(out,e.identity());out.writeBoolean(e.owner()!=null);if(e.owner()!=null)string(out,e.owner());
     string(out,e.template());string(out,e.raw());string(out,e.provenance());out.writeInt(e.allowedTemplates().size());for(String t:new TreeSet<>(e.allowedTemplates()))string(out,t);out.writeInt(e.baseline().size());
     for(var r:new TreeMap<>(e.baseline()).entrySet()){string(out,r.getKey());out.writeInt(r.getValue());}
    }
   }
   byte[] body=bytes.toByteArray();var all=new ByteArrayOutputStream();
   try(var out=new DataOutputStream(all)){out.writeInt(body.length);out.write(body);out.write(hash(body));}
   byte[] written=all.toByteArray();temp=Files.createTempFile(path.getParent(),".excel-practice-original-",".tmp");
   try(var ch=FileChannel.open(temp,StandardOpenOption.WRITE)){var buffer=ByteBuffer.wrap(written);while(buffer.hasRemaining())ch.write(buffer);ch.force(true);}
   fault.at("before-move");ready();Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
   try(var dir=FileChannel.open(path.getParent(),StandardOpenOption.READ)){dir.force(true);}
   fault.at("after-move");if(!Arrays.equals(written,Files.readAllBytes(path)))throw new IOException("identity journal readback mismatch");
   disk=written;entries.clear();entries.putAll(proposed);
  }catch(IOException e){uncertain=true;throw e;}finally{if(temp!=null)Files.deleteIfExists(temp);}
 }
 private static byte[] hash(byte[] data)throws IOException {try{return MessageDigest.getInstance("SHA-256").digest(data);}catch(NoSuchAlgorithmException e){throw new IOException(e);}}
 private void read(byte[] all)throws IOException {
  try(var envelope=new DataInputStream(new ByteArrayInputStream(all))){int n=envelope.readInt();if(n<0||n!=all.length-36)throw new IOException("identity journal envelope");byte[] body=envelope.readNBytes(n);if(!MessageDigest.isEqual(hash(body),envelope.readNBytes(32)))throw new IOException("identity journal checksum");
   try(var in=new DataInputStream(new ByteArrayInputStream(body))){if(!in.readUTF().equals("EXCEL-PRACTICE-ORIGINAL-1"))throw new IOException("identity journal version");int count=in.readInt();if(count<0||count>capacity)throw new IOException("identity journal capacity");
    for(int i=0;i<count;i++){String id=string(in,800),owner=in.readBoolean()?string(in,200):null,template=string(in,800),raw=string(in,1048576),provenance=string(in,1200);int templates=in.readInt();if(templates<1||templates>16)throw new IOException("template count");var allowed=new TreeSet<String>();for(int t=0;t<templates;t++)if(!allowed.add(string(in,800)))throw new IOException("duplicate template");int ranks=in.readInt();if(ranks<0||ranks>128)throw new IOException("baseline count");var baseline=new TreeMap<String,Integer>();for(int j=0;j<ranks;j++)if(baseline.put(string(in,800),in.readInt())!=null)throw new IOException("duplicate baseline");Entry e=new Entry(id,owner,template,raw,provenance,baseline,allowed);validate(e);if(entries.putIfAbsent(id,e)!=null)throw new IOException("duplicate identity");}
    if(in.available()!=0)throw new IOException("trailing journal bytes");
   }
  }
 }
 @Override public void close(){closed=true;try{if(lock!=null)lock.release();}catch(IOException ignored){}try{if(channel!=null)channel.close();}catch(IOException ignored){}}
}
