package com.example.app.common.support;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 聚合身份生成器：<b>UUID v7，应用侧生成</b>。
 * <p>为什么不再用数据库自增主键（见 ADR-0006）：</p>
 * <ul>
 *   <li>自增 ID 让聚合在工厂方法里"没有身份"，只能等仓储插入后回填，逼出一个
 *       {@code attachId()} 这种"仅供仓储调用"的封装破洞；</li>
 *   <li>自增 ID 暴露业务量级、分库分表与多实例唯一性都要额外基础设施（雪花/号段）；</li>
 *   <li>64 位 Long 出到前端还要全局序列化成字符串防 JS 精度丢失。</li>
 * </ul>
 * <p>UUID v7 布局（RFC 9562）：48 位毫秒时间戳 + 4 位版本号 7 + 12 位 rand_a + 2 位变体 10 + 62 位 rand_b。
 * 时间戳在高位，因此 <b>字典序≈时间序</b>，B-tree 索引不会像 UUID v4 那样随机插入导致页分裂；
 * 同毫秒内用 rand_a 做递增序号，保证同一进程内严格单调不重复。</p>
 * <p>该类是纯 JDK 实现，零框架依赖，可脱离 Spring 容器做确定性单测。</p>
 * <p>增强 MVC 分层同样适用：Service 在插入前就持有主键，工号这类业务编号由主键派生，
 * 不再需要"先插入拿自增 ID 再回填"的二次 UPDATE。</p>
 */
public final class Ids {

    /** RFC 9562 version 7 */
    private static final int VERSION_7 = 7;

    /** RFC 9562 variant：二进位 10 */
    private static final long VARIANT_RFC_4122 = 0x2L << 62;

    /** 12 位 rand_a 掩码 */
    private static final long RAND_A_MASK = 0xFFFL;

    /** 62 位 rand_b 掩码 */
    private static final long RAND_B_MASK = 0x3FFFFFFFFFFFFFFFL;

    /** 同毫秒序号的状态保护锁：显式 ReentrantLock（PMD AvoidSynchronizedStatement 要求），
     *  仅竞争极短的临界区，不影响吞吐 */
    private static final ReentrantLock LOCK = new ReentrantLock();

    /** 上一次分配的毫秒时间戳 */
    private static long lastMillis;

    /** 同毫秒内的递增序号（占 rand_a 的 12 位） */
    private static long sequence;

    private Ids() {
    }

    /** 生成字符串形态的新身份（推荐：DO/主键/对外暴露统一用字符串，天然规避大整数精度问题） */
    public static String newId() {
        return uuidV7().toString();
    }

    /**
     * 生成 UUID v7。
     * <p>同毫秒序号耗尽（4096 个/毫秒）时自旋到下一毫秒，保证单调不重复而非静默回绕。</p>
     */
    public static UUID uuidV7() {
        long millis;
        long randA;
        LOCK.lock();
        try {
            long now = System.currentTimeMillis();
            if (now != lastMillis) {
                lastMillis = now;
                sequence = ThreadLocalRandom.current().nextLong(RAND_A_MASK + 1);
            } else if (sequence < RAND_A_MASK) {
                sequence++;
            } else {
                // 同毫秒序号耗尽：自旋到下一毫秒，保证单调不重复而非静默回绕
                while (now == lastMillis) {
                    Thread.onSpinWait();
                    now = System.currentTimeMillis();
                }
                lastMillis = now;
                sequence = ThreadLocalRandom.current().nextLong(RAND_A_MASK + 1);
            }
            millis = lastMillis;
            randA = sequence;
        } finally {
            LOCK.unlock();
        }
        long randB = ThreadLocalRandom.current().nextLong() & RAND_B_MASK;
        long mostSignificantBits = (millis << 16) | ((long) VERSION_7 << 12) | randA;
        long leastSignificantBits = randB | VARIANT_RFC_4122;
        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
