package wrv.magic.evidence;
import java.io.*;import java.security.*;import java.util.*;
/** V2 typed fingerprints. Strings preserve exact Java UTF-16 code units with an explicit length. */
public final class EvidenceFingerprint {
 private interface Encoding{void write(DataOutputStream out)throws IOException;}
 public static String root(EffectObserver.RootEvidence r){Objects.requireNonNull(r);return hash(out->{text(out,"wrv-root-evidence-v2");uuid(out,r.cast());uuid(out,r.caster());text(out,r.spell());source(out,r.source());});}
 public static String receipt(EffectObserver.Receipt r){Objects.requireNonNull(r);return hash(out->{text(out,"wrv-useful-evidence-v2");uuid(out,r.cast());uuid(out,r.caster());text(out,r.spell());uuid(out,r.recipient());text(out,r.kind()==null?null:r.kind().name());number(out,r.effectiveAmount());source(out,r.source());var c=r.control();out.writeBoolean(c!=null);if(c!=null){integer(out,c.beforeAmplifier());out.writeInt(c.beforeDuration());out.writeInt(c.afterAmplifier());out.writeInt(c.afterDuration());}var x=r.cleanse();out.writeBoolean(x!=null);if(x!=null){text(out,x.effectKey());out.writeInt(x.beforeAmplifier());out.writeInt(x.beforeDuration());out.writeBoolean(x.ambient());out.writeBoolean(x.particles());out.writeBoolean(x.icon());}});}
 private static void source(DataOutputStream out,EffectObserver.SourceSnapshot s)throws IOException{out.writeBoolean(s!=null);if(s==null)return;text(out,s.wandId());text(out,s.templateKey());uuid(out,s.ownerId());out.writeBoolean(s.ownerKnown());bool(out,s.bound());bool(out,s.offhand());out.writeBoolean(s.earns()!=null);if(s.earns()!=null)number(out,s.earns());out.writeLong(s.issuedAtMillis());uuid(out,s.observedMakerItemId());}
 private static void text(DataOutputStream out,String s)throws IOException{out.writeBoolean(s!=null);if(s!=null){out.writeInt(s.length());for(int i=0;i<s.length();i++)out.writeChar(s.charAt(i));}}
 private static void uuid(DataOutputStream out,UUID id)throws IOException{out.writeBoolean(id!=null);if(id!=null){out.writeLong(id.getMostSignificantBits());out.writeLong(id.getLeastSignificantBits());}}
 private static void bool(DataOutputStream out,Boolean b)throws IOException{out.writeByte(b==null?0:b?2:1);}
 private static void integer(DataOutputStream out,Integer i)throws IOException{out.writeBoolean(i!=null);if(i!=null)out.writeInt(i);}
 private static void number(DataOutputStream out,double d)throws IOException{if(!Double.isFinite(d))throw new IllegalArgumentException("nonfinite evidence number");out.writeLong(Double.doubleToRawLongBits(d));}
 private static String hash(Encoding encoding){try{var bytes=new ByteArrayOutputStream();try(var out=new DataOutputStream(bytes)){encoding.write(out);}return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));}catch(IOException|NoSuchAlgorithmException e){throw new IllegalStateException("evidence encoding unavailable",e);}}
 private EvidenceFingerprint(){}
}
