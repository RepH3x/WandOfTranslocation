package com.reph3x.wot;

import com.reph3x.wot.item.ModItems;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WandOfTranslocation implements ModInitializer {
	public static final String MOD_ID = "wandoftranslocation";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.registerModItems();
	}
}