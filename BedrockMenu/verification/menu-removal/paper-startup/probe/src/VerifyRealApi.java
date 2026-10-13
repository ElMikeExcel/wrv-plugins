import java.lang.reflect.Field;
import java.util.Arrays;
public class VerifyRealApi {
  public static void main(String[] args) throws Exception {
    int checks=0;
    for (String simple : new String[]{"BedrockMenu","GeyserForms","TeleportMenu","TeleportMenu$PickHolder"}) {
      Class<?> type=Class.forName("com.winkreyes.bedrockmenu."+simple);
      type.getDeclaredFields(); type.getDeclaredMethods(); type.getDeclaredConstructors();
      checks++; System.out.println("PASS actual Paper API class linkage: "+simple);
    }
    Class<?> type=Class.forName("com.winkreyes.bedrockmenu.BedrockMenu");
    Field b=type.getDeclaredField("BUTTONS"), c=type.getDeclaredField("COMMANDS");b.setAccessible(true);c.setAccessible(true);
    if(!Arrays.equals((String[])b.get(null),new String[]{"Spawn","Teleport to a player","Spawn Shop","mcMMO Stats","Leaderboard","Server Info","Auto-open on join"}))throw new AssertionError("buttons");
    if(!Arrays.equals((String[])c.get(null),new String[]{"spawn","tpmenu","shop","mcstats","leaderboard","motd","menujoin"}))throw new AssertionError("commands");
    checks+=2;
    System.out.println("PASS "+checks+" real Paper API/JDK25 class-loading checks; no server was started or JavaPlugin instantiated.");
  }
}
