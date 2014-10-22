/*
 * Copyright (c) IntellectualCrafters - 2014. You are not allowed to distribute
 * and/or monetize any of our intellectual property. IntellectualCrafters is not
 * affiliated with Mojang AB. Minecraft is a trademark of Mojang AB.
 * 
 * >> File = Claim.java >> Generated by: Citymonstret at 2014-08-09 01:41
 */

package com.intellectualcrafters.plot.commands;

import java.util.ArrayList;
import java.util.UUID;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;

import com.google.common.collect.BiMap;
import com.intellectualcrafters.plot.C;
import com.intellectualcrafters.plot.FlagManager;
import com.intellectualcrafters.plot.PlayerFunctions;
import com.intellectualcrafters.plot.Plot;
import com.intellectualcrafters.plot.PlotHelper;
import com.intellectualcrafters.plot.PlotId;
import com.intellectualcrafters.plot.PlotMain;
import com.intellectualcrafters.plot.PlotManager;
import com.intellectualcrafters.plot.PlotWorld;
import com.intellectualcrafters.plot.SchematicHandler;
import com.intellectualcrafters.plot.StringWrapper;
import com.intellectualcrafters.plot.UUIDHandler;
import com.intellectualcrafters.plot.database.DBFunc;
import com.intellectualcrafters.plot.events.PlayerClaimPlotEvent;

/**
 * @author Citymonstret
 */
public class DebugClaimTest extends SubCommand {

	public DebugClaimTest() {
		super(Command.DEBUGCLAIMTEST, "If you accidentally delete your database, this command will attempt to restore all plots based on the data from the plot signs. Execution time may vary", "claim", CommandCategory.INFO, false);
	}

	@Override
	public boolean execute(Player plr, String... args) {
	    if (plr==null) {
	        if (args.length<3) {
	            PlayerFunctions.sendMessage(plr, "If you accidentally delete your database, this command will attempt to restore all plots based on the data from the plot signs. \n\n&cMissing world arg /plot debugclaimtest {world} {PlotId min} {PlotId max}");
	            return false;
	        }
	        World world = Bukkit.getWorld(args[0]);
	        if (world==null || !PlotMain.isPlotWorld(world)) {
	            PlayerFunctions.sendMessage(plr, "&cInvalid plot world!");
	            return false;
	        }
	        
	        PlotId min, max;
	        
	        try {
	            String[] split1 = args[1].split(";");
	            String[] split2 = args[2].split(";");
	            
	            min = new PlotId(Integer.parseInt(split1[0]), Integer.parseInt(split1[1]));
	            max = new PlotId(Integer.parseInt(split2[0]), Integer.parseInt(split2[1]));
	        }
	        catch (Exception e) {
	            PlayerFunctions.sendMessage(plr, "&cInvalid min/max values. &7The values are to Plot IDs in the format &cX;Y &7where X,Y are the plot coords\nThe conversion will only check the plots in the selected area.");
	            return false;
	        }
	        PlayerFunctions.sendMessage(plr, "&3Sign Block&8->&3PlotSquared&8: &7Beginning sign to plot conversion. This may take a while...");
	        PlayerFunctions.sendMessage(plr, "&3Sign Block&8->&3PlotSquared&8: Found an excess of 250,000 chunks. Limiting search radius... (~3.8 min)");
	        
	        PlotManager manager = PlotMain.getPlotManager(world);
	        PlotWorld plotworld = PlotMain.getWorldSettings(world);
	        
	        ArrayList<Plot> plots = new ArrayList<Plot>();

	        for (PlotId id : PlayerFunctions.getPlotSelectionIds(world, min, max)) {
                Plot plot = PlotHelper.getPlot(world, id);
                boolean contains = PlotMain.getPlots(world).containsKey(plot.id);
                if (contains) {
                    PlayerFunctions.sendMessage(plr, " - &cDB Already contains: "+plot.id);
                    continue;
                }
                
                Location loc = manager.getSignLoc(world, plotworld, plot);
                
                Chunk chunk = world.getChunkAt(loc);
                
                if (!chunk.isLoaded()) {
                    boolean result = chunk.load(false);
                    if (!result) {
                        continue;
                    }
                }
                
                Block block = world.getBlockAt(loc);
                if (block!=null) {
                    if (block.getState() instanceof Sign) {
                        Sign sign = (Sign) block.getState();
                        if (sign!=null) {
                            String line = sign.getLine(2);
                            if (line!=null && line.length() > 2) {
                                line = line.substring(2);
                                
                                BiMap<StringWrapper, UUID> map = UUIDHandler.getUuidMap();
                                
                                UUID uuid = (map.get(new StringWrapper(line)));
                                
                                if (uuid==null) {
                                    for (StringWrapper string : map.keySet()) {
                                        if (string.value.toLowerCase().startsWith(line.toLowerCase())) {
                                            uuid = map.get(string);
                                            break;
                                        }
                                    }
                                }
                                if (uuid==null) {
                                    uuid = UUIDHandler.getUUID(line);
                                }
                                if (uuid!=null) {
                                    PlayerFunctions.sendMessage(plr, " - &aFound plot: "+plot.id+" : "+line);
                                    plot.owner = uuid;
                                    plot.hasChanged = true;
                                    plots.add(plot);
                                }
                                else {
                                    PlayerFunctions.sendMessage(plr, " - &cInvalid playername: "+plot.id+" : "+line);
                                }
                            }
                        }
                    }
                }
	        }
	        
	        if (plots.size()>0) {
	            PlayerFunctions.sendMessage(plr, "&3Sign Block&8->&3PlotSquared&8: &7Updating '"+plots.size()+"' plots!");
    	        DBFunc.createPlots(plots);
    	        DBFunc.createAllSettingsAndHelpers(plots);
    	        
    	        for (Plot plot : plots) {
    	            PlotMain.updatePlot(plot);
    	        }
    	        
    	        PlayerFunctions.sendMessage(plr, "&3Sign Block&8->&3PlotSquared&8: &7Complete!");
	        
	        }
	        else {
	            PlayerFunctions.sendMessage(plr, "No plots were found for the given search.");
	        }
	        
	    }
	    else {
	        PlayerFunctions.sendMessage(plr, "This debug command can only be executed by console as it has been deemed unsafe if abused.");
	    }
        return true;
	}

	public static boolean claimPlot(Player player, Plot plot, boolean teleport) {
		return claimPlot(player, plot, teleport, "");
	}

	public static boolean claimPlot(Player player, Plot plot, boolean teleport, String schematic) {
		PlayerClaimPlotEvent event = new PlayerClaimPlotEvent(player, plot);
		Bukkit.getPluginManager().callEvent(event);
		if (!event.isCancelled()) {
			PlotHelper.createPlot(player, plot);
			PlotHelper.setSign(player, plot);
			PlayerFunctions.sendMessage(player, C.CLAIMED);
			if (teleport) {
				PlotMain.teleportPlayer(player, player.getLocation(), plot);
			}
			PlotWorld world = PlotMain.getWorldSettings(plot.getWorld());
			if (world.SCHEMATIC_ON_CLAIM) {
				SchematicHandler handler = new SchematicHandler();
				SchematicHandler.Schematic sch;
				if (schematic.equals("")) {
					sch = handler.getSchematic(world.SCHEMATIC_FILE);
				}
				else {
					sch = handler.getSchematic(schematic);
					if (sch == null) {
						sch = handler.getSchematic(world.SCHEMATIC_FILE);
					}
				}
				handler.paste(player.getLocation(), sch, plot);
			}
			plot.settings.setFlags(FlagManager.parseFlags(PlotMain.getWorldSettings(player.getWorld()).DEFAULT_FLAGS));
		}
		return event.isCancelled();
	}
}
