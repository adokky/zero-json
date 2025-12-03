package karamel.benchmarks;

import dev.dokky.zerojson.ZeroJson;
import io.kodec.buffers.MutableBuffer;
import kotlinx.serialization.json.Json;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public interface ThreadLocalState {
    Json getKtxJson();
    ZeroJson getZJson();
    byte[] copyOf(byte[] other);
    OutputStream getOutputStream();
    MutableBuffer getBuffer();
}
