package com.winkreyes.teamrewards.marker;
import java.io.*;import java.nio.channels.*;import java.nio.file.*;import java.util.*;
/** Single-process durable placement intent. Registry never contains rewards or edits reward ledger. */
public final class MarkerRegistry implements AutoCloseable {
 public record Marker(UUID id,UUID owner,UUID world,int x,int y,int z) { public Marker {Objects.requireNonNull(id);Objects.requireNonNull(owner);Objects.requireNonNull(world);} }
 interface WriteProbe {void afterMove() throws IOException;}
 WriteProbe writeProbe=()->{};
 private final Path file;private final FileChannel channel;private final FileLock lock;
 private final Map<UUID,Marker> markers=new HashMap<>();private byte[] expected;private boolean failed,closed;
 public MarkerRegistry(Path file)throws IOException{
  this.file=file;Files.createDirectories(file.toAbsolutePath().getParent());channel=FileChannel.open(file.resolveSibling(file.getFileName()+".lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
  FileLock acquired;try{acquired=channel.tryLock();}catch(Exception e){channel.close();throw new IOException("registry locked",e);}if(acquired==null){channel.close();throw new IOException("registry locked");}lock=acquired;
  try{expected=Files.exists(file)?Files.readAllBytes(file):null;if(expected!=null){try(DataInputStream d=new DataInputStream(new ByteArrayInputStream(expected))){if(d.readInt()!=1)throw new IOException("version");int n=d.readInt();if(n<0||n>10000)throw new IOException("count");for(int i=0;i<n;i++){Marker m=new Marker(UUID.fromString(d.readUTF()),UUID.fromString(d.readUTF()),UUID.fromString(d.readUTF()),d.readInt(),d.readInt(),d.readInt());if(markers.put(m.id(),m)!=null)throw new IOException("duplicate");}if(d.read()!=-1)throw new IOException("trailing bytes");}}}catch(Exception e){close();throw new IOException("invalid registry",e);}
 }
 public synchronized boolean writable(){return !failed&&!closed;}
 public synchronized Marker get(UUID id){ready();return markers.get(id);}
 public synchronized Collection<Marker> all(){ready();return List.copyOf(markers.values());}
 public synchronized void add(Marker marker)throws IOException{ready();if(markers.size()>=10000)throw new IOException("marker capacity");if(markers.containsKey(marker.id()))throw new IOException("duplicate marker");mutate(()->markers.put(marker.id(),marker));}
 public synchronized void remove(UUID id)throws IOException{ready();if(!markers.containsKey(id))return;mutate(()->markers.remove(id));}
 private void ready(){if(failed||closed)throw new IllegalStateException("registry closed or uncertain");}
 public synchronized void verifyCurrent() throws IOException {
  ready();
  try { if(!Arrays.equals(expected,Files.exists(file)?Files.readAllBytes(file):null))throw new IOException("external registry edit"); }
  catch(IOException failure){failed=true;throw failure;}
 }
 private void mutate(Runnable op)throws IOException{
  verifyCurrent();
  op.run();Path tmp=null;try{ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream d=new DataOutputStream(b)){d.writeInt(1);d.writeInt(markers.size());for(Marker m:markers.values()){d.writeUTF(m.id().toString());d.writeUTF(m.owner().toString());d.writeUTF(m.world().toString());d.writeInt(m.x());d.writeInt(m.y());d.writeInt(m.z());}}byte[] bytes=b.toByteArray();tmp=Files.createTempFile(file.toAbsolutePath().getParent(),"marker-",".tmp");try(FileChannel c=FileChannel.open(tmp,StandardOpenOption.WRITE)){var buffer=java.nio.ByteBuffer.wrap(bytes);while(buffer.hasRemaining())c.write(buffer);c.force(true);}Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);writeProbe.afterMove();try(FileChannel c=FileChannel.open(file.toAbsolutePath().getParent(),StandardOpenOption.READ)){c.force(true);}if(!Arrays.equals(bytes,Files.readAllBytes(file)))throw new IOException("registry readback mismatch");expected=bytes;}catch(Exception e){failed=true;throw new IOException("marker persistence uncertain",e);}finally{if(tmp!=null)Files.deleteIfExists(tmp);}
 }
 @Override public synchronized void close()throws IOException{if(closed)return;closed=true;try{lock.release();}finally{channel.close();}}
}
