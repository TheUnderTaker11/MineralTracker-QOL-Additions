package com.theundertaker11.mtqoladditions.commands;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.theundertaker11.mtqoladditions.utils.DepositRegistryPublic;

import lordfokas.mineraltracker.tracker.ClientTracker;
import lordfokas.mineraltracker.tracker.DepositRegistry;
import lordfokas.mineraltracker.tracker.IDeposit;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class MTRemoveDepositCommand implements ICommand {

	private static boolean calledOnceYet = false;
	
	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] params) throws CommandException {
		if(!calledOnceYet) {
			calledOnceYet = true;
			String message = "I HIGHLY recommend backing up your /mineraltracker/ folder before attempting this. I am messing with the save file in a way that could permanently delete ALL of your MineralTracker data!!!"
					+ "\nThis warning will only pop up once per session, so you can run the same command again and it will now work.";
			TextComponentString text = new TextComponentString(message);
			text.getStyle().setColor(TextFormatting.RED);
			text.getStyle().setBold(true);
			sender.sendMessage(text);
			return;
		}
		if (params != null && params.length == 2) {
			String xStr = params[0];
			String zStr = params[1];
			int depX = Integer.MIN_VALUE;
			int depZ = Integer.MIN_VALUE;
			try{
				depX = Integer.parseInt(xStr);
				depZ = Integer.parseInt(zStr);
			}catch(Error | Exception e) {}
			if(depX == Integer.MIN_VALUE || depZ == Integer.MIN_VALUE) {
				ITextComponent errorMessage= new TextComponentString("Invalid X and/or Z pos, please try again.");
				sender.sendMessage(errorMessage);
				return;
			}
			
			String gameDir = Minecraft.getMinecraft().gameDir.getAbsolutePath();
			String fixedGameDir = gameDir.endsWith(".") ? gameDir.substring(0,gameDir.length()-1) : gameDir;
			
			Minecraft mc = Minecraft.getMinecraft();
	        final String serverName = mc.isSingleplayer() ? ("sp@" + mc.getIntegratedServer().getFolderName()) : ("mp@" + mc.getCurrentServerData().serverIP.replace(':', '_'));
			File mtFile = new File(fixedGameDir + "/mineraltracker/" + serverName + ".samples2");
			
			DepositRegistryPublic depositsReg = new DepositRegistryPublic.Client(mtFile);
			depositsReg.load();
			
			
			int depositsRemoved = 0;
			for(Iterator<IDeposit> iterator = depositsReg.deposits.iterator(); iterator.hasNext();) {
				IDeposit deposit = iterator.next();
				if(deposit.getPosX() == depX && deposit.getPosZ() == depZ) {
					iterator.remove();
					depositsRemoved += 1;
				}
			}
			if(depositsRemoved > 0) {
				boolean savedSuccessfully = false;
				try {
					Field depositRegistryField = ClientTracker.INSTANCE.getClass().getDeclaredField("deposits");
					depositRegistryField.setAccessible(true);
					
					Object depositRegistryObj = depositRegistryField.get(ClientTracker.INSTANCE);
					if(depositRegistryObj instanceof DepositRegistry) {
						System.out.println("DepositRegistry field grabbed successfully!");
						DepositRegistry depositRegistry = (DepositRegistry)depositRegistryObj;
						System.out.println("Test value:" + depositRegistry.toString());
						Field depositsTreeField = DepositRegistry.class.getDeclaredField("deposits");
						depositsTreeField.setAccessible(true);
						
						Object depositsTreeObj = depositsTreeField.get(depositRegistry);
						if(depositsTreeObj instanceof TreeSet<?>) {
							TreeSet<IDeposit> depositsTree = (TreeSet<IDeposit>)depositsTreeObj;
							System.out.println("DepositsTree field grabbed successfully!");
							System.out.println("Before clearing and overwriting list:");
							System.out.println("My tree size = " + depositsReg.deposits.size());
							System.out.println("MineralTracker tree size = " + depositsTree.size());
							
							//Now that all that BS reflection is done, finally clear the tree and add all my values.
							depositsTree.clear();
							
							depositsTree.addAll(depositsReg.deposits);
							
							System.out.println("After clearing and overwriting list:");
							System.out.println("My tree size = " + depositsReg.deposits.size());
							System.out.println("MineralTracker tree size = " + depositsTree.size());
							savedSuccessfully = true;
						}
					}
				}catch(Error | Exception e) {
					System.out.println("MTQOLAdditions: Saving MineralTracker RAM Tree data structure has FAILED with below error:");
					e.printStackTrace();
				}
				if(savedSuccessfully) {
					try {
						depositsReg.save();
					}catch(Exception e) {
						savedSuccessfully = false;
						System.out.println("MTQOLAdditions: Saving MineralTracker file FAILED with below error");
						e.printStackTrace();
					}
				}
				if(savedSuccessfully) {
					String message = "Removed " + depositsRemoved + " deposits from the MineralTracker data successfully!";
					TextComponentString text = new TextComponentString(message);
					text.getStyle().setColor(TextFormatting.GREEN);
					sender.sendMessage(text);
				}else {
					String message = "Failed to save data to MineralTracker file. Check logs for why.";
					TextComponentString text = new TextComponentString(message);
					text.getStyle().setColor(TextFormatting.RED);
					sender.sendMessage(text);
				}
			}else {
				String message = "There are 0 deposits found for those X Y coordinates, so nothing was changed.";
				TextComponentString text = new TextComponentString(message);
				text.getStyle().setColor(TextFormatting.GREEN);
				sender.sendMessage(text);
			}
			
		}else if(params.length == 1 && params[0].equals("help")) {
			String message = "Use the following format for the command:\n /mtremovedeposit X Z"
					+ "\nYou can use /mtlocateores to determine the X and Z of the deposit you want to remove!";
			TextComponentString text = new TextComponentString(message);
			text.getStyle().setColor(TextFormatting.GREEN);
			sender.sendMessage(text);
		}else {
			String message = "Invalid format, please use the following format for the command:\n /mtremovedeposit X Z";
			TextComponentString text = new TextComponentString(message);
			text.getStyle().setColor(TextFormatting.RED);
			sender.sendMessage(text);
		}
		
	}
	
	
	@Override
	public String getName() {
		return "mtremovedeposit";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/mtremovedeposit X Z";
	}

	@Override
	public int compareTo(ICommand o) {
		return 0;
	}

	@Override
	public List<String> getAliases() {
		List<String> aliases = new ArrayList<String>();
		aliases.add("/mtremovedeposit");
		return aliases;
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return true;
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args,
			BlockPos targetPos) {
		return new ArrayList<String>();
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		return false;
	}

}
