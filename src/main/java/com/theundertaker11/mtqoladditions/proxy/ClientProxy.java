package com.theundertaker11.mtqoladditions.proxy;

import com.theundertaker11.mtqoladditions.MTQOLAdditionsMain;

import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {
	
	@SubscribeEvent
	public static void registerModels(ModelRegistryEvent event) {
		//ResourceLocation essenceTextures = new ResourceLocation(MTQOLAdditionsMain.MODID + ":" + "essence2");

		//ModelBakery.registerItemVariants(ItemRegistry.essence, essenceTextures);

		//RenderRegistry.render();
	}
}
