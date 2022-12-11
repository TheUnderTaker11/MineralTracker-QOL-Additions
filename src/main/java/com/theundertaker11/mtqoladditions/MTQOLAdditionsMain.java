package com.theundertaker11.mtqoladditions;

import com.theundertaker11.mtqoladditions.commands.MTLocateOresCommand;
import com.theundertaker11.mtqoladditions.commands.MTRemoveDepositCommand;

import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod(modid = MTQOLAdditionsMain.MODID, name = MTQOLAdditionsMain.NAME, version = MTQOLAdditionsMain.VERSION)
public class MTQOLAdditionsMain
{
    public static final String MODID = "mtqoladditions";
    public static final String NAME = "MineralTracker QOL Additions";
    public static final String VERSION = "1.1";
    

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
    	if(event.getSide() == Side.CLIENT) {
    		ClientCommandHandler.instance.registerCommand(new MTLocateOresCommand());
    		ClientCommandHandler.instance.registerCommand(new MTRemoveDepositCommand());
    	}
    }

    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    	
    }
}
