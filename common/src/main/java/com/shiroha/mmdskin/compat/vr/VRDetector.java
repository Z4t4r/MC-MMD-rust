package com.shiroha.mmdskin.compat.vr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * mc-vr-api 运行时检测守卫
 * <p>
 * 通过 Class.forName 检测 mc-vr-api 是否存在，
 * 结果缓存，确保后续所有 VR 类引用安全隔离。
 */
public final class VRDetector {

    private static final Logger logger = LogManager.getLogger();
    private static final String VR_API_CLASS = "net.blf02.vrapi.api.IVRAPI";

    // 三态：null=未检测, true=可用, false=不可用
    private static volatile Boolean available = null;

    private VRDetector() {}

    /**
     * mc-vr-api 是否可用（线程安全，惰性检测）
     */
    public static boolean isAvailable() {
        if (available == null) {
            synchronized (VRDetector.class) {
                if (available == null) {
                    available = detect();
                }
            }
        }
        return available;
    }

    private static boolean detect() {
        try {
            Class.forName(VR_API_CLASS);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
