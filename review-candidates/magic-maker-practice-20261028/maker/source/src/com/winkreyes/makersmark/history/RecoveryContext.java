package com.winkreyes.makersmark.history;
import java.io.*;import java.util.*;import java.security.*;import org.bukkit.inventory.ItemStack;
/** Minimal private recovery context: UUIDs and fingerprints only, no raw item text, location or chat. */
public record RecoveryContext(UUID initiator,String workstation,String resultFingerprint,List<Input> inputs) {
 public record Input(int slot,UUID item,long revision,String fingerprint){}
 public RecoveryContext {inputs=List.copyOf(inputs);if(initiator==null||workstation==null||!workstation.matches("[A-Z_]{1,40}")||!valid(resultFingerprint)||inputs.isEmpty()||inputs.size()>2)throw new IllegalArgumentException("Invalid recovery context");Set<UUID> ids=new HashSet<>();for(Input i:inputs)if(i==null||i.item==null||i.slot<0||i.revision<1||!ids.add(i.item)||!valid(i.fingerprint))throw new IllegalArgumentException("Invalid recovery input");}
 private static boolean valid(String s){return s!=null&&s.matches("[0-9a-f]{64}");}
 public byte[] encode()throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream d=new DataOutputStream(b);d.writeInt(0x4d4d4331);uuid(d,initiator);d.writeUTF(workstation);d.writeUTF(resultFingerprint);d.writeInt(inputs.size());for(Input i:inputs){d.writeInt(i.slot);uuid(d,i.item);d.writeLong(i.revision);d.writeUTF(i.fingerprint);}return b.toByteArray();}
 public static RecoveryContext decode(byte[] bytes)throws IOException{try{DataInputStream d=new DataInputStream(new ByteArrayInputStream(bytes));if(d.readInt()!=0x4d4d4331)throw new IOException("Unknown context version");UUID owner=uuid(d);String kind=d.readUTF(),result=d.readUTF();int n=d.readInt();if(n<1||n>2)throw new IOException("Invalid input count");List<Input> inputs=new ArrayList<>();for(int i=0;i<n;i++)inputs.add(new Input(d.readInt(),uuid(d),d.readLong(),d.readUTF()));if(d.available()!=0)throw new IOException("Trailing context data");return new RecoveryContext(owner,kind,result,inputs);}catch(IllegalArgumentException e){throw new IOException("Invalid recovery context",e);}}
 public static String fingerprint(ItemStack item){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(item.serializeAsBytes()));}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
 private static void uuid(DataOutputStream d,UUID u)throws IOException{d.writeLong(u.getMostSignificantBits());d.writeLong(u.getLeastSignificantBits());}
 private static UUID uuid(DataInputStream d)throws IOException{return new UUID(d.readLong(),d.readLong());}
}
