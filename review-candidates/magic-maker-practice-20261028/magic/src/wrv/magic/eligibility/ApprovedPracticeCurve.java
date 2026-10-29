package wrv.magic.eligibility;
import java.util.*;
/** Owner-approved cumulative useful casts; single-level abilities stay unlock-only. */
public final class ApprovedPracticeCurve {
 private static final List<Long> POINTS=List.of(40L,120L,300L,600L,1000L);
 public static List<Long> forNativeLevels(int levels){if(levels<1||levels>6)throw new IllegalArgumentException("unreviewed native level range");return List.copyOf(POINTS.subList(0,levels-1));}
 public static int level(int nativeLevels,long usefulCount,int verifiedEarnedFloor){if(usefulCount<0||verifiedEarnedFloor<1||verifiedEarnedFloor>nativeLevels)throw new IllegalArgumentException();int learned=1;for(long n:forNativeLevels(nativeLevels))if(usefulCount>=n)learned++;return Math.max(learned,verifiedEarnedFloor);}
 private ApprovedPracticeCurve(){}
}
