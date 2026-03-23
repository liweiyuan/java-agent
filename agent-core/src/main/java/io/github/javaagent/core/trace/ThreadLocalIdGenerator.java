package io.github.javaagent.core.trace;

import java.util.Random;

/**
 * 基于 ThreadLocal&lt;Random&gt; 的 ID 生成器，避免 UUID/SecureRandom 的锁竞争。
 * traceId: 32位十六进制（128bit），spanId: 16位十六进制（64bit）
 */
public class ThreadLocalIdGenerator implements IdGenerator {

    private static final ThreadLocal<Random> RANDOM = new ThreadLocal<Random>() {
        @Override
        protected Random initialValue() {
            return new Random();
        }
    };

    @Override
    public String generateTraceId() {
        return generateHex(32);
    }

    @Override
    public String generateSpanId() {
        return generateHex(16);
    }

    private static String generateHex(int length) {
        Random random = RANDOM.get();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString(random.nextInt(16)));
        }
        return sb.toString();
    }
}
