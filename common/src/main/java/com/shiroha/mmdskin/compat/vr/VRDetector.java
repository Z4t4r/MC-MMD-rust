package com.shiroha.mmdskin.compat.vr;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * mc-vr-api 运行时检测守卫（线程安全，惰性检测）
 */
public final class VRDetector {

    private static final Logger logger = LogManager.getLogger();
    private static final String VR_API_CLASS = "net.blf02.vrapi.api.IVRAPI";

    private static volatile Boolean available = null;

    private VRDetector() {}

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
            logger.info("mc-vr-api 已检测到，VR 联动功能可用");
            return true;
        } catch (ClassNotFoundException e) {
            logger.info("mc-vr-api 未安装，VR 联动功能已禁用");
            return false;
        }
    }
}
