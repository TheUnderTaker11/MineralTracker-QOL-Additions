package com.theundertaker11.mtqoladditions.commands;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.theundertaker11.mtqoladditions.utils.DepositDistanceObj;
import com.theundertaker11.mtqoladditions.utils.DepositRegistryPublic;

import journeymap.client.waypoint.WaypointParser;
import lordfokas.mineraltracker.plugins.terrafirma.DepositTFC;
import lordfokas.mineraltracker.tracker.IDeposit;
import lordfokas.mineraltracker.tracker.IDeposit.Type;
import net.minecraft.client.Minecraft;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.common.Optional;

public class MTLocateOresCommand implements ICommand {

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] params) {
		if (params != null && params.length == 2) {
			String maxToListStr = params[0];
			int maxToList = -1;
			try{
				maxToList = Integer.parseInt(maxToListStr);
			}catch(Error | Exception e) {}
			if(maxToList <= 0) {
				ITextComponent errorMessage= new TextComponentString("Invalid max number of deposits. Must be between 1 or greater");
				sender.sendMessage(errorMessage);
				return;
			}
			
			String targetOresParam = params[1];
			
			String gameDir = Minecraft.getMinecraft().gameDir.getAbsolutePath();
			String fixedGameDir = gameDir.endsWith(".") ? gameDir.substring(0,gameDir.length()-1) : gameDir;
			
			Minecraft mc = Minecraft.getMinecraft();
	        final String serverName = mc.isSingleplayer() ? ("sp@" + mc.getIntegratedServer().getFolderName()) : ("mp@" + mc.getCurrentServerData().serverIP.replace(':', '_'));
			File mtFile = new File(fixedGameDir + "/mineraltracker/" + serverName + ".samples2");
			
			DepositRegistryPublic depositsReg = new DepositRegistryPublic.Client(mtFile);
			depositsReg.load();
			
			
			System.out.println("Original Game dir:" + gameDir);
			System.out.println("Fixed Game dir: " + fixedGameDir);
			System.out.println("MT file exist?: " + mtFile.exists());
			System.out.println("MT file: " + mtFile.getAbsolutePath());
			
			
			
			List<DepositDistanceObj> allTargetDeposits = new ArrayList<>();
			
			System.out.println("Total Number of deposits in File: " + depositsReg.deposits.size());
			//Add all deposits to the list based on their name to be sorted.
			String[] targetOresIfContainsList = getOreIfContainsList(targetOresParam);
			for (IDeposit deposit : depositsReg.deposits) {
				if(deposit.getWorld() == sender.getEntityWorld().provider.getDimension()) {
					if(deposit.getType() == Type.TERRAFIRMA) {
						DepositTFC tfcdeposit = (DepositTFC)deposit;
						boolean isMatch = false;
						for(String oreStrToCheck : targetOresIfContainsList) {
							if(tfcdeposit.ore.toLowerCase().contains(oreStrToCheck.toLowerCase())) {
								isMatch = true;
								break;
							}
						}
						if(isMatch) {
							int distance = (int)getDistanceFromPlayer(deposit.getPosX(),deposit.getPosZ());
							allTargetDeposits.add(new DepositDistanceObj(deposit,distance));
						}
					}
				}
			}
			
			//Sort list of all deposits by distance to player.
			Collections.sort(allTargetDeposits);
			
			//List is sorted in asc order, but when outputting to chat you need to reverse this order if you want
			// the closest deposits to be listed at the bottom of the chat screen.
			// So not only do I have to loop in reverse order, I have to specify the starting index to be maxToList - 1 
			// (or list size maxToList is to large)
			int startingIndex = allTargetDeposits.size() < maxToList ? allTargetDeposits.size() - 1 : maxToList - 1;
			for(int i=startingIndex; i >= 0; i--) {
				IDeposit deposit = allTargetDeposits.get(i).deposit;
				if(deposit.getWorld() == sender.getEntityWorld().provider.getDimension()) {
					if(deposit.getType() == Type.TERRAFIRMA) {
						DepositTFC tfcdeposit = (DepositTFC)deposit;
						int depositX = deposit.getPosX();
						int depositZ = deposit.getPosZ();
						
						int playerY = sender.getPosition().getY();
						
						String name = "UNKNOWN ORE";
						System.out.println("Distance from player = " + allTargetDeposits.get(i).distance);
						try{
							name = prettifyTFCOreName(tfcdeposit.ore);
						}catch(Error | Exception e) {
							System.out.println("MT QOL Additions mod: Failed to get ore name");
							System.out.println(tfcdeposit.ore);
							e.printStackTrace();
						}
						
						String message = journeyMapFormattedLocation(name,depositX,playerY,depositZ);
						ITextComponent text = new TextComponentString(message);
						
						//Attempt to Journymap format this text so it's clickable as a waypoint.
						try {
							text = formatMessageForJourneymapWaypoint(message);
						}catch(Error | Exception e) {
							System.out.println("MT QOL Additions mod: failed to format waypoint.");
							System.out.println("Caught error and will send as plain text");
							e.printStackTrace();
						}
						//text.getStyle().setColor(TextFormatting.AQUA);
						
						sender.sendMessage(text.appendText(" Distance = " + allTargetDeposits.get(i).distance));
					}
					
					
				}
			}
			
			if(allTargetDeposits.size() == 0) {
				ITextComponent noDepositsFoundMessage = new TextComponentString("No Ore Deposits found for the given entry. Make sure your list is comma-seperated. "
						+ "Also note that ore Deposit are only found if the text you gave is contained within the ore name itself!");
				noDepositsFoundMessage.getStyle().setColor(TextFormatting.RED);
				sender.sendMessage(noDepositsFoundMessage);
			}
		}else if(params.length == 1 && params[0].equals("help")) {
			String message = "Use the following format for the command:\n /mtlocateores <Max number of deposits to list> <Comma-seperated ore list>";
			TextComponentString text = new TextComponentString(message);
			text.getStyle().setColor(TextFormatting.GREEN);
			sender.sendMessage(text);
		}else {
			String message = "Invalid format, please use the following format for the command:\n /mtlocateores <Max number of deposits to list> <Comma-seperated ore list>";
			TextComponentString text = new TextComponentString(message);
			text.getStyle().setColor(TextFormatting.RED);
			sender.sendMessage(text);
		}
	}
	
	/**
	 * Hardcode some shortcuts for convience sake.
	 * @param oreParam
	 * @return
	 */
	public static String[] getOreIfContainsList(String oreParam) {
		String[] splitParam = oreParam.split(",");
		
		//Allow wildcard to get all deposits
		if(oreParam.toLowerCase().equals("*")) {
			return new String[] {""};
		}
		//Hardcoded list all variants when finding just a listing for a general ore type
		if(oreParam.toLowerCase().equals("iron*") || oreParam.toLowerCase().equals("iron")) {
			return new String[] {"iron","hematite","limonite","magnetite"};
		}
		if(oreParam.toLowerCase().equals("copper*") || oreParam.toLowerCase().equals("copper")) {
			return new String[] {"copper","tetrahedrite","malachite"};
		}
		if(oreParam.toLowerCase().equals("tin*") || oreParam.toLowerCase().equals("tin")) {
			return new String[] {"tin","cassiterite"};
		}
		if(oreParam.toLowerCase().equals("nickle*") || oreParam.toLowerCase().equals("nickle") || oreParam.toLowerCase().equals("nickel*") || oreParam.toLowerCase().equals("nickel")) {
			return new String[] {"nickle","nickel","garnierite"};
		}
		if(oreParam.toLowerCase().equals("bismuth*") || oreParam.toLowerCase().equals("bismuth")) {
			return new String[] {"bismuth","bismuthinite"};
		}
		if(oreParam.toLowerCase().equals("zinc*") || oreParam.toLowerCase().equals("zinc")) {
			return new String[] {"zinc","sphalerite"};
		}
		if(oreParam.toLowerCase().equals("diamond*") || oreParam.toLowerCase().equals("diamond")) {
			return new String[] {"diamond","kimberlite"};
		}
		if(oreParam.toLowerCase().equals("redstone*") || oreParam.toLowerCase().equals("redstone")) {
			return new String[] {"redstone","cinnabar","cryolite"};
		}
		if(oreParam.toLowerCase().equals("lithium*") || oreParam.toLowerCase().equals("lithium")) {
			return new String[] {"lithium","spodumene"};
		}
		if(oreParam.toLowerCase().equals("tungsten*") || oreParam.toLowerCase().equals("tungsten")) {
			return new String[] {"tungsten","wolframite"};
		}
		if(oreParam.toLowerCase().equals("cobal*") || oreParam.toLowerCase().equals("cobal") || oreParam.toLowerCase().equals("cobalt")) {
			return new String[] {"cobalt","cobal","cobaltite"};
		}
		if(oreParam.toLowerCase().equals("titanium*") || oreParam.toLowerCase().equals("titanium")) {
			return new String[] {"titanium","rutile"};
		}
		if(oreParam.toLowerCase().equals("boron*") || oreParam.toLowerCase().equals("boron")) {
			return new String[] {"boron","borax"};
		}
		if(oreParam.toLowerCase().equals("lead*") || oreParam.toLowerCase().equals("lead")) {
			return new String[] {"lead","galena"};
		}
		if(oreParam.toLowerCase().equals("aluminum*") || oreParam.toLowerCase().equals("aluminum")) {
			return new String[] {"aluminum","bauxite"};
		}
		if(oreParam.toLowerCase().equals("beryllium*") || oreParam.toLowerCase().equals("beryllium")) {
			return new String[] {"beryllium","beryl"};
		}
		return splitParam;
	}
	public static String prettifyTFCOreName(String name) {
		String string = name;
		if(string.contains(".")) {
			string = string.split("\\.")[1].replace('_', ' ');
		}
        final char[] chars = string.toLowerCase().toCharArray();
        boolean found = false;
        for (int i = 0; i < chars.length; ++i) {
            if (!found && Character.isLetter(chars[i])) {
                chars[i] = Character.toUpperCase(chars[i]);
                found = true;
            }
            else if (Character.isWhitespace(chars[i]) || chars[i] == '.' || chars[i] == '\'') {
                found = false;
            }
        }
        return String.valueOf(chars);
	    
	}
	
	@Optional.Method(modid="journeymap")
	public static ITextComponent formatMessageForJourneymapWaypoint(final String jmWaypointFormattedText) {
        final List<String> matches = WaypointParser.getWaypointStrings(jmWaypointFormattedText);
        if (matches != null) {
            boolean changed = false;
            final ITextComponent result = addWaypointMarkup(jmWaypointFormattedText,matches);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
	
	public static double getDistanceFromPlayer(int posX, int posZ) {
		double deltaX = Minecraft.getMinecraft().player.getPosition().getX() - posX;
		double deltaZ = Minecraft.getMinecraft().player.getPosition().getZ() - posZ;
			
		return Math.sqrt((deltaX * deltaX) + (deltaZ * deltaZ));
	}
	
	@Optional.Method(modid="journeymap")
	private static ITextComponent addWaypointMarkup(final String text, final List<String> matches) {
        final List<ITextComponent> newParts = new ArrayList<ITextComponent>();
        int index = 0;
        boolean matched = false;
        final Iterator<String> iterator = matches.iterator();
        while (iterator.hasNext()) {
            final String match = iterator.next();
            if (text.contains(match)) {
                final int start = text.indexOf(match);
                if (start > index) {
                    newParts.add((ITextComponent)new TextComponentString(text.substring(index, start)));
                }
                matched = true;
                final TextComponentString clickable = new TextComponentString(match);
                final Style chatStyle = clickable.getStyle();
                chatStyle.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/jm wpedit " + match));
                final TextComponentString hover = new TextComponentString("JourneyMap: ");
                hover.getStyle().setColor(TextFormatting.YELLOW);
                final TextComponentString hover2 = new TextComponentString("Click to create Waypoint.\nCtrl+Click to view on map.");
                hover2.getStyle().setColor(TextFormatting.AQUA);
                hover.appendSibling((ITextComponent)hover2);
                chatStyle.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, (ITextComponent)hover));
                chatStyle.setColor(TextFormatting.AQUA);
                newParts.add((ITextComponent)clickable);
                index = start + match.length();
                iterator.remove();
            }
        }
        if (!matched) {
            return null;
        }
        if (index < text.length() - 1) {
            newParts.add((ITextComponent)new TextComponentString(text.substring(index, text.length())));
        }
        if (!newParts.isEmpty()) {
            final TextComponentString replacement = new TextComponentString("");
            for (final ITextComponent sib : newParts) {
                replacement.appendSibling(sib);
            }
            return (ITextComponent)replacement;
        }
        return null;
    }
	
	
	public static String journeyMapFormattedLocation(String name, int x, int y, int z) {
		return "[name:\"" + name + "\", x:" + x + ", y:" + y + ", z:" + z + "]";
	}

	@Override
	public String getName() {
		return "mtlocateores";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/mtlocateores <Max number of deposits to list> <Comma-seperated ore list>";
	}

	@Override
	public int compareTo(ICommand o) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<String> getAliases() {
		List<String> aliases = new ArrayList<String>();
		aliases.add("/mtlocateores");
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
