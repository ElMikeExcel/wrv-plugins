package com.winkreyes.excelwandbars;
import java.util.*;import org.bukkit.Bukkit;import wrv.magic.evidence.*;
/** Generation-scoped attachment to one explicit provider; never installs or replaces an observer. */
final class SharedHistoryAttachment implements AutoCloseable {
 private SharedHistoryProvider.Lease lease;private Runnable invalidateOwner;private boolean invalidated,closed,ownerAttached;
 private void thread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("server owner thread required");}
 static SharedHistoryAttachment acquire(){
  if(!Bukkit.isPrimaryThread())throw new IllegalStateException("server owner thread required");
  var registrations=Bukkit.getServicesManager().getRegistrations(SharedHistoryProvider.class);
  if(registrations.size()!=1)throw new IllegalStateException("exactly one shared history provider required");
  var registration=registrations.iterator().next();
  if(!registration.getPlugin().isEnabled())throw new IllegalStateException("history provider disabled");
  var attachment=new SharedHistoryAttachment();
  try{attachment.lease=Objects.requireNonNull(registration.getProvider().acquire(attachment::invalidate));
   if(attachment.invalidated||!attachment.lease.active())throw new IllegalStateException("history provider invalidated during acquire");
   return attachment;
  }catch(Throwable failure){try{attachment.close();}catch(Throwable cleanup){failure.addSuppressed(cleanup);}throw failure;}
 }
 private void invalidate(){thread();if(closed)return;invalidated=true;var callback=invalidateOwner;invalidateOwner=null;if(callback!=null)callback.run();}
 ConsumerFanout.Binding binding(){thread();if(closed||invalidated||lease==null||!lease.active())throw new IllegalStateException("history attachment inactive");return lease.binding();}
 void attachOwner(Runnable closeOwner){thread();if(ownerAttached)throw new IllegalStateException("owner already attached");Objects.requireNonNull(closeOwner);ownerAttached=true;if(closed||invalidated||!lease.active()){closeOwner.run();throw new IllegalStateException("history lost during shared startup");}invalidateOwner=closeOwner;}
 @Override public void close(){thread();if(closed)return;closed=true;invalidateOwner=null;if(lease!=null){var old=lease;lease=null;old.close();}}
}
