package com.verse.spleef;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import turniplabs.halplibe.HalpLibe;

public final class BtaSpleef implements ModInitializer {
	public static final String MOD_ID = HalpLibe.registerMod("btaspleef", true);
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		SpleefManager.initialize();
		LOGGER.info("BTA Spleef initialized (server-side gameplay; vanilla BTA clients supported).");
	}
}
