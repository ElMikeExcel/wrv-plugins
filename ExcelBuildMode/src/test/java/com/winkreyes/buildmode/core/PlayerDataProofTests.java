package com.winkreyes.buildmode.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.GZIPOutputStream;
import static com.winkreyes.buildmode.core.PlayerDataProof.Status.*;

/** Isolated synthetic identities only. Native fixture came from the prior synthetic CraftPlayer adapter test. */
public final class PlayerDataProofTests {
    private static final UUID PLAYER = UUID.fromString("12345678-1234-5678-9abc-0123456789ab");
    private static final UUID SESSION = UUID.fromString("00000000-0000-0000-0000-000000000042");
    private static final String KEY = PlayerDataProof.MARKER_KEY, ACTIVE = "v1:active:" + SESSION, RESTORED = "v1:restored:" + SESSION, REVIEW = "v1:review:" + SESSION;
    private static Path root; private static int assertions;
    @FunctionalInterface interface Writer { void write(DataOutputStream out) throws IOException; }
    @FunctionalInterface interface Checked { void run() throws IOException; }
    public static void main(String[] args) throws Exception {
        root = Files.createTempDirectory(Path.of("target"), "playerdata-proof-");
        basicClassification(); markerClassification(); forceProtocol(); corruptInput(); gzipVariants(); nbtBounds(); forceRaces(); nativeFixture();
        String result = "PASS " + assertions + " assertions (bounded native-playerdata proof, exact marker force, mutation races and synthetic exact-Paper playerdata fixture; no new running Paper test)\n";
        Files.writeString(Path.of("target/playerdata-proof-test-result.txt"), result); System.out.print(result);
    }
    private static byte[] nbt(Writer writer) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) { out.writeByte(10); out.writeUTF(""); writer.write(out); out.writeByte(0); }
        return bytes.toByteArray();
    }
    private static void named(DataOutputStream out, int type, String name) throws IOException { out.writeByte(type); out.writeUTF(name); }
    private static void uuid(DataOutputStream out, UUID id) throws IOException {
        named(out,11,"UUID"); out.writeInt(4); out.writeInt((int)(id.getMostSignificantBits() >>> 32)); out.writeInt((int)id.getMostSignificantBits()); out.writeInt((int)(id.getLeastSignificantBits() >>> 32)); out.writeInt((int)id.getLeastSignificantBits());
    }
    private static void mode(DataOutputStream out,int mode) throws IOException { named(out,3,"playerGameType");out.writeInt(mode); }
    private static void base(DataOutputStream out,int mode) throws IOException { uuid(out,PLAYER);mode(out,mode); }
    private static void marker(DataOutputStream out,String value) throws IOException { named(out,10,"BukkitValues");named(out,8,KEY);out.writeUTF(value);out.writeByte(0); }
    private static byte[] record(int mode,String marker) throws IOException { return nbt(out->{base(out,mode);if(marker!=null)marker(out,marker);}); }
    private static byte[] gzip(byte[] data) throws IOException { ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(GZIPOutputStream zip=new GZIPOutputStream(bytes)){zip.write(data);}return bytes.toByteArray(); }
    private static Path bytes(byte[] value) throws IOException {Path p=Files.createTempFile(root,"record-",".dat");Files.write(p,value);return p;}
    private static Path file(byte[] decoded) throws IOException { return bytes(gzip(decoded)); }
    private static PlayerDataProof.Inspection inspect(Path path) { return PlayerDataProof.inspect(path,PLAYER,KEY); }
    private static void check(boolean value,String text) { if(!value)throw new AssertionError(text);assertions++; }
    private static void fails(Checked work,String text) throws IOException {try{work.run();}catch(IOException expected){assertions++;return;}throw new AssertionError(text);}
    private static void unknown(byte[] decoded,String text) throws IOException {check(inspect(file(decoded)).status()==UNKNOWN,text);}
    private static void basicClassification() throws IOException {
        Path normal=file(record(0,null));var result=inspect(normal);
        check(result.status()==VERIFIED_UNMARKED_SURVIVAL && PLAYER.equals(result.player()) && result.gameMode()==0 && result.marker()==null,"exact identity, Survival and absent marker verified");
        for(int mode:List.of(1,2,3))check(inspect(file(record(mode,null))).status()==UNKNOWN,"unmarked non-Survival is unsafe");
        for(int mode:List.of(-1,4,Integer.MAX_VALUE))unknown(record(mode,null),"invalid gamemode");
        check(PlayerDataProof.inspect(normal,UUID.randomUUID(),KEY).status()==UNKNOWN,"wrong expected UUID unknown");
        check(PlayerDataProof.inspect(normal,null,KEY).status()==UNKNOWN,"missing expected UUID unknown");
        check(PlayerDataProof.inspect(normal,PLAYER,"other:secret").status()==UNKNOWN,"cannot query unrelated PDC key");
        check(PlayerDataProof.inspect(null,PLAYER,KEY).status()==UNKNOWN,"null path unknown");
        check(inspect(root.resolve("missing.dat")).status()==UNKNOWN,"missing file unknown");
        check(inspect(root).status()==UNKNOWN,"directory unknown");
        Path link=root.resolve("symlink.dat");Files.createSymbolicLink(link,normal.getFileName());check(inspect(link).status()==UNKNOWN,"symlink file unknown");
        unknown(nbt(out->mode(out,0)),"missing UUID unknown"); unknown(nbt(out->uuid(out,PLAYER)),"missing gamemode unknown");
        unknown(nbt(out->{named(out,8,"UUID");out.writeUTF(PLAYER.toString());mode(out,0);}),"wrong UUID type unknown");
        unknown(nbt(out->{named(out,11,"UUID");out.writeInt(3);out.writeInt(0);out.writeInt(0);out.writeInt(0);mode(out,0);}),"wrong UUID array length unknown");
        unknown(nbt(out->{uuid(out,PLAYER);named(out,1,"playerGameType");out.writeByte(0);}),"wrong gamemode type unknown");
        unknown(nbt(out->{base(out,0);named(out,8,"BukkitValues");out.writeUTF("not a compound");}),"wrong PDC root type unknown");
    }
    private static void markerClassification() throws IOException {
        for(String value:List.of(ACTIVE,RESTORED,REVIEW,"", "v2:active:"+SESSION,"v1:active:invalid", "v1:other:"+SESSION)) {
            var marked=inspect(file(record(0,value)));check(marked.status()==MARKED && value.equals(marked.marker()),"every present string marker is unsafe, including unknown/malformed versions");
        }
        check(inspect(file(record(1,ACTIVE))).status()==MARKED,"marked Creative is identifiable but unsafe");
        Path wrongType=file(nbt(out->{base(out,0);named(out,10,"BukkitValues");named(out,3,KEY);out.writeInt(1);out.writeByte(0);}));
        check(inspect(wrongType).status()==MARKED && inspect(wrongType).marker()==null,"wrong-type marker is marked, never absent");
        fails(()->PlayerDataProof.forceVerifiedMarker(wrongType,PLAYER,KEY,null,true),"wrong-type marker cannot be cleared by absence proof");
        Path elsewhere=file(nbt(out->{base(out,0);named(out,8,KEY);out.writeUTF(ACTIVE);named(out,10,"nested");marker(out,ACTIVE);out.writeByte(0);}));
        check(inspect(elsewhere).status()==VERIFIED_UNMARKED_SURVIVAL,"only the root BukkitValues marker is authoritative");
        Path unrelated=file(nbt(out->{base(out,0);named(out,10,"BukkitValues");named(out,8,"anotherplugin:effect");out.writeUTF("preserve me");out.writeByte(0);}));
        byte[] before=Files.readAllBytes(unrelated);PlayerDataProof.forceVerifiedMarker(unrelated,PLAYER,KEY,null,true);check(Arrays.equals(before,Files.readAllBytes(unrelated)),"proof never alters unrelated PDC or record bytes");
    }
    private static void forceProtocol() throws IOException {
        Path review=file(record(1,REVIEW));
        check(PlayerDataProof.forceVerifiedMarker(review,PLAYER,KEY,REVIEW,false).status()==MARKED,"review marker is acknowledged without treating Creative as clean Survival");
        fails(()->PlayerDataProof.forceVerifiedMarker(review,PLAYER,KEY,ACTIVE,false),"review marker never acknowledges an active session");
        fails(()->PlayerDataProof.forceVerifiedMarker(review,PLAYER,KEY,RESTORED,false),"review marker never acknowledges restored inventory");
        fails(()->PlayerDataProof.forceVerifiedMarker(review,PLAYER,KEY,null,false),"review marker never counts as absence");
        fails(()->PlayerDataProof.forceVerifiedMarker(review,PLAYER,KEY,REVIEW,true),"review acknowledgment cannot bypass explicit Survival requirement");
        Path saved=file(record(0,ACTIVE));
        check(PlayerDataProof.forceVerifiedMarker(saved,PLAYER,KEY,ACTIVE,true).status()==MARKED,"exact active marker force verified before Creative");
        fails(()->PlayerDataProof.forceVerifiedMarker(saved,PLAYER,KEY,RESTORED,true),"active cannot acknowledge restored state");
        fails(()->PlayerDataProof.forceVerifiedMarker(saved,PLAYER,KEY,"v1:active:"+UUID.randomUUID(),true),"another session marker cannot satisfy expected session");
        fails(()->PlayerDataProof.forceVerifiedMarker(saved,PLAYER,KEY,null,true),"present marker cannot satisfy removal");
        Files.write(saved,gzip(record(0,RESTORED)));check(PlayerDataProof.forceVerifiedMarker(saved,PLAYER,KEY,RESTORED,true).status()==MARKED,"exact restored marker acknowledged before journal completion");
        Files.write(saved,gzip(record(0,null)));check(PlayerDataProof.forceVerifiedMarker(saved,PLAYER,KEY,null,true).status()==VERIFIED_UNMARKED_SURVIVAL,"actual marker removal verified after commits");
        fails(()->PlayerDataProof.forceVerifiedMarker(saved,PLAYER,KEY,ACTIVE,true),"absent marker cannot satisfy active entry");
        Files.write(saved,gzip(record(1,ACTIVE)));fails(()->PlayerDataProof.forceVerifiedMarker(saved,PLAYER,KEY,ACTIVE,true),"saved Creative cannot satisfy pre-entry Survival proof");
        check(PlayerDataProof.forceVerifiedMarker(saved,PLAYER,KEY,ACTIVE,false).status()==MARKED,"explicit non-Survival inspection can verify marker without claiming clean Survival");
        fails(()->PlayerDataProof.forceVerifiedMarker(saved,UUID.randomUUID(),KEY,ACTIVE,false),"force cannot accept wrong player UUID");
        fails(()->PlayerDataProof.forceVerifiedMarker(saved,PLAYER,KEY,"v9:active:"+SESSION,false),"unknown expected marker protocol rejected");
    }
    private static void corruptInput() throws IOException {
        byte[] valid=gzip(record(0,null));
        for(int length=0;length<valid.length;length++)check(inspect(bytes(Arrays.copyOf(valid,length))).status()==UNKNOWN,"every truncated gzip prefix fails closed");
        byte[] checksum=valid.clone();checksum[checksum.length-8]^=1;check(inspect(bytes(checksum)).status()==UNKNOWN,"payload CRC corruption unknown");
        byte[] size=valid.clone();size[size.length-4]^=1;check(inspect(bytes(size)).status()==UNKNOWN,"ISIZE corruption unknown");
        byte[] reserved=valid.clone();reserved[3]|=(byte)0x20;check(inspect(bytes(reserved)).status()==UNKNOWN,"reserved gzip flag unknown");
        byte[] method=valid.clone();method[2]=0;check(inspect(bytes(method)).status()==UNKNOWN,"unsupported gzip method unknown");
        check(inspect(bytes(record(0,null))).status()==UNKNOWN,"uncompressed NBT is not native gzip proof");
        check(inspect(bytes(Arrays.copyOf(valid,valid.length+1))).status()==UNKNOWN,"trailing compressed junk rejected");
        byte[] two=new byte[valid.length*2];System.arraycopy(valid,0,two,0,valid.length);System.arraycopy(valid,0,two,valid.length,valid.length);check(inspect(bytes(two)).status()==UNKNOWN,"concatenated gzip members rejected");
        byte[] trailing=record(0,null);unknown(Arrays.copyOf(trailing,trailing.length+1),"trailing decompressed NBT data rejected");
        unknown(new byte[]{9,0,0,0,0,0,0,0},"non-compound NBT root rejected");
        unknown(nbt(out->{base(out,0);uuid(out,PLAYER);}),"duplicate root UUID rejected");
        unknown(nbt(out->{base(out,0);mode(out,1);}),"duplicate root gamemode rejected");
        unknown(nbt(out->{base(out,0);named(out,10,"BukkitValues");named(out,8,KEY);out.writeUTF(ACTIVE);named(out,8,KEY);out.writeUTF(RESTORED);out.writeByte(0);}),"duplicate marker rejected");
        unknown(nbt(out->{base(out,0);named(out,13,"invalid");}),"invalid tag type rejected");
        unknown(nbt(out->{base(out,0);named(out,7,"invalid");out.writeInt(-1);}),"negative array length rejected");
        unknown(nbt(out->{base(out,0);named(out,9,"invalid");out.writeByte(0);out.writeInt(1);}),"nonempty END list rejected");
    }
    private static void gzipVariants() throws IOException {
        byte[] original=gzip(record(0,ACTIVE));ByteArrayOutputStream header=new ByteArrayOutputStream();
        byte[] first=Arrays.copyOf(original,10);first[3]=30;header.write(first);header.write(new byte[]{3,0,1,2,3});header.write("file.dat\0comment\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        CRC32 crc=new CRC32();crc.update(header.toByteArray());int sum=(int)crc.getValue();header.write(sum&255);header.write(sum>>>8&255);header.write(original,10,original.length-10);
        byte[] decorated=header.toByteArray();check(inspect(bytes(decorated)).status()==MARKED,"valid optional gzip fields and header CRC supported");
        decorated[10]^=1;check(inspect(bytes(decorated)).status()==UNKNOWN,"corrupt optional gzip header rejected");
    }
    private static void nbtBounds() throws IOException {
        unknown(nbt(out->{base(out,0);named(out,7,"huge");out.writeInt(PlayerDataProof.MAX_ELEMENTS+1);}),"oversized declared array rejected before allocation");
        unknown(nbt(out->{base(out,0);named(out,9,"huge");out.writeByte(1);out.writeInt(PlayerDataProof.MAX_ELEMENTS+1);}),"oversized declared list rejected before traversal");
        unknown(nbt(out->{base(out,0);for(int i=0;i<PlayerDataProof.MAX_DEPTH+2;i++)named(out,10,"nested");for(int i=0;i<PlayerDataProof.MAX_DEPTH+2;i++)out.writeByte(0);}),"deep nesting rejected without stack exhaustion");
        Path sparse=Files.createTempFile(root,"oversized-",".dat");try(RandomAccessFile file=new RandomAccessFile(sparse.toFile(),"rw")){file.setLength((long)PlayerDataProof.MAX_COMPRESSED_BYTES+1);}check(inspect(sparse).status()==UNKNOWN,"oversized compressed file rejected before read");
        ByteArrayOutputStream compressed=new ByteArrayOutputStream();try(GZIPOutputStream zip=new GZIPOutputStream(compressed)){byte[] block=new byte[8192];for(int i=0;i<PlayerDataProof.MAX_DECOMPRESSED_BYTES/8192+1;i++)zip.write(block);}check(inspect(bytes(compressed.toByteArray())).status()==UNKNOWN,"decompression bomb bounded");
        Path types=file(nbt(out->{base(out,0);for(int type=1;type<=12;type++){
            named(out,type,"type"+type);switch(type){case 1->out.writeByte(0);case 2->out.writeShort(0);case 3->out.writeInt(0);case 4->out.writeLong(0);case 5->out.writeFloat(0);case 6->out.writeDouble(0);case 7->{out.writeInt(3);out.write(new byte[]{1,2,3});}case 8->out.writeUTF("unicode \u0000 \ud83d\ude42");case 9->{out.writeByte(0);out.writeInt(0);}case 10->out.writeByte(0);case 11->{out.writeInt(1);out.writeInt(9);}case 12->{out.writeInt(1);out.writeLong(9);}}
        }}));check(inspect(types).status()==VERIFIED_UNMARKED_SURVIVAL,"all standard NBT payload types and modified UTF are handled without retaining unrelated data");
    }
    private static void forceRaces() throws IOException {
        for(String point:List.of("verified","forced")) {
            Path saved=file(record(0,ACTIVE));
            fails(()->PlayerDataProof.forceVerifiedMarker(saved,PLAYER,KEY,ACTIVE,true,p->{if(p.equals(point)){Path replacement=file(record(0,ACTIVE));Files.move(replacement,saved,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}}),"identical bytes on replaced inode never count as same acknowledged save");
            final Path changed=file(record(0,ACTIVE));
            fails(()->PlayerDataProof.forceVerifiedMarker(changed,PLAYER,KEY,ACTIVE,true,p->{if(p.equals(point))Files.write(changed,gzip(record(0,RESTORED)));}),"mutation during force rejected");
        }
        Path saved=file(record(0,ACTIVE));fails(()->PlayerDataProof.forceVerifiedMarker(saved,PLAYER,KEY,ACTIVE,true,p->{throw new IOException("injected force failure");}),"force-path failure never acknowledges saved marker");
    }
    private static void nativeFixture() throws IOException {
        Path source=Path.of("src/test/resources/playerdata/native-paper26.2-synthetic-survival.dat"),copy=root.resolve("native-synthetic.dat");Files.copy(source,copy);
        UUID syntheticNative=new UUID(443322,6);var read=PlayerDataProof.inspect(copy,syntheticNative,KEY);
        check(read.status()==VERIFIED_UNMARKED_SURVIVAL && read.gameMode()==0,"actual cached Paper26.2 native synthetic-playerdata structure verified");
        byte[] before=Files.readAllBytes(copy);PlayerDataProof.forceVerifiedMarker(copy,syntheticNative,KEY,null,true);check(Arrays.equals(before,Files.readAllBytes(copy)),"actual native fixture force is byte-preserving");
    }
}
