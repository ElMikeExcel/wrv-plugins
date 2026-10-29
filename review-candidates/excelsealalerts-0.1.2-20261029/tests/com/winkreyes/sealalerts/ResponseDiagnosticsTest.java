/* Excel WRV maintained source; original attribution and license preserved. */ // Excel — WRV custom plugin source; original identity and credits preserved.
package com.winkreyes.sealalerts;
import org.junit.jupiter.api.Test;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Flow;
import static org.junit.jupiter.api.Assertions.*;
class ResponseDiagnosticsTest {
 @Test void allowsOnlyFixedCodeAndPath() { assertEquals("code=INVALID_PAYLOAD path=events.type",ResponseDiagnostics.summary("{\"code\":\"INVALID_PAYLOAD\",\"path\":\"events.type\"}")); }
 @Test void excludesSecretsAndArbitraryFields() { assertEquals("diagnostic=not-allowlisted",ResponseDiagnostics.summary("{\"code\":\"secretToken\",\"message\":\"private player and URL\",\"path\":\"players.secret\"}")); }
 @Test void ignoresEscapedInjection() { assertEquals("diagnostic=not-allowlisted",ResponseDiagnostics.summary("{\"message\":\"\\\"code\\\":\\\"INVALID_JSON\\\"\"}")); }
 @Test void boundedExtraction() { assertEquals("diagnostic=not-allowlisted",ResponseDiagnostics.summary("x".repeat(4096)+"{\"code\":\"INVALID_JSON\"}")); }
 @Test void unavailableSafe() { assertEquals("diagnostic=unavailable",ResponseDiagnostics.summary(null)); }
 static class Sub implements Flow.Subscription { boolean cancelled; public void request(long n) {} public void cancel(){cancelled=true;} }
 @Test void subscriberCapsBytesAndCancels() { var b=new ResponseDiagnostics.Bounded(); var s=new Sub(); b.onSubscribe(s); b.onNext(List.of(ByteBuffer.wrap(new byte[9000]))); assertTrue(s.cancelled); assertEquals(4096,b.result.join().length()); }
 @Test void subscriberPreservesSmallBody() { var b=new ResponseDiagnostics.Bounded(); b.onSubscribe(new Sub()); b.onNext(List.of(ByteBuffer.wrap("abc".getBytes()))); b.onComplete(); assertEquals("abc",b.result.join()); }
 @Test void subscriberErrorDoesNotLeak() { var b=new ResponseDiagnostics.Bounded(); b.onSubscribe(new Sub()); b.onError(new RuntimeException("secret")); assertNull(b.result.join()); }
}
