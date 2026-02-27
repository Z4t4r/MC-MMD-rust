package com.shiroha.mmdskin.fabric;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.shiroha.mmdskin.fabric.register.MmdSkinRegisterCommon;

import net.fabricmc.api.ModInitializer;

public class MmdSkinFabric implements ModInitializer {
    public static final Logger logger = LogManager.getLogger();
    @Override
    public void onInitialize() {
        MmdSkinRegisterCommon.Register();
    }
}
