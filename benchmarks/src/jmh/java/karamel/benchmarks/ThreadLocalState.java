package karamel.benchmarks;

import dev.dokky.zerojson.ZeroJson;
import kotlinx.serialization.json.Json;

public interface ThreadLocalState {
    Json getKtxJson();
    ZeroJson getZJson();
    byte[] copyOf(byte[] other);
}
