package wrv.magic.evidence;
/** One optional downstream history consumer. Provider never installs the shared observer. */
public interface SharedHistoryProvider {
 Lease acquire(Runnable invalidated);
 interface Lease extends AutoCloseable {
  ConsumerFanout.Binding binding();
  boolean active();
  @Override void close();
 }
}
