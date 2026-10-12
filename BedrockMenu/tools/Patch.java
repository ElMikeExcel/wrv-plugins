import org.objectweb.asm.*;
import java.nio.file.*;
public class Patch {
  public static void main(String[] a) throws Exception {
    for (String f : a) {
      byte[] b = Files.readAllBytes(Path.of(f));
      ClassReader r = new ClassReader(b);
      ClassWriter w = new ClassWriter(0);
      boolean main = f.endsWith("/BedrockMenu.class");
      r.accept(new ClassVisitor(Opcodes.ASM9, w) {
        public void visit(int v,int acc,String n,String s,String sup,String[] i){ super.visit(65,acc,n,s,sup,i);} // Java 21
        public MethodVisitor visitMethod(int acc,String n,String d,String s,String[] e){
          MethodVisitor mv = super.visitMethod(acc,n,d,s,e);
          if (!main || !n.equals("<clinit>")) return mv;
          String[] L={"Spawn","Teleport to a player","Spawn Shop","Jobs & Money","mcMMO Stats","Leaderboard","Server Info","Auto-open on join"};
          String[] C={"spawn","tpmenu","shop","jobs browse","mcstats","leaderboard","motd","menujoin"};
          mv.visitCode();
          emit(mv,L,"BUTTONS"); emit(mv,C,"COMMANDS");
          mv.visitInsn(Opcodes.RETURN); mv.visitMaxs(4,0); mv.visitEnd();
          return null;
        }
        void emit(MethodVisitor mv,String[] v,String field){
          mv.visitIntInsn(Opcodes.BIPUSH,v.length); mv.visitTypeInsn(Opcodes.ANEWARRAY,"java/lang/String");
          for(int i=0;i<v.length;i++){mv.visitInsn(Opcodes.DUP);mv.visitIntInsn(Opcodes.BIPUSH,i);mv.visitLdcInsn(v[i]);mv.visitInsn(Opcodes.AASTORE);}
          mv.visitFieldInsn(Opcodes.PUTSTATIC,"com/winkreyes/bedrockmenu/BedrockMenu",field,"[Ljava/lang/String;");
        }
      }, 0);
      Files.write(Path.of(f), w.toByteArray());
    }
  }
}
