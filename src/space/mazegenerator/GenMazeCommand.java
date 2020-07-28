package space.mazegenerator;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Entity;

import io.github.jorelali.commandapi.api.CommandAPI;

public class GenMazeCommand extends BukkitCommand
{
	public GenMazeCommand()
	{
		super("genmaze","","", Arrays.asList("genmaze"));
		this.setPermission("mazegenerator.genmaze");
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location)
	{
		if(args.length < 7)
		{
			if(args[args.length - 1].equals(""))
			{
				if(location == null) return Arrays.asList("~");
				else
				{
					switch((args.length - 1) % 3)
					{
						case 0: return Arrays.asList(String.valueOf(location.getBlockX()));
						case 1: return Arrays.asList(String.valueOf(location.getBlockY()));
						case 2: return Arrays.asList(String.valueOf(location.getBlockZ()));
					}
				}
			}
		}
		else if(args.length == 10)
		{
			if(args[9].equals("")) return Arrays.asList("true","false");
			else
			{
				if("true".startsWith(args[9])) return Arrays.asList("true");
				else if("false".startsWith(args[9])) return Arrays.asList("false");
			}
		}
		else if(args.length == 12)
		{
			if(args[11].equals("")) return Arrays.asList("true","false");
			else
			{
				if("true".startsWith(args[11])) return Arrays.asList("true");
				else if("false".startsWith(args[11])) return Arrays.asList("false");
			}
		}
		else if(args.length == 14)
		{
			if(args[13].equals("")) return Arrays.asList("normal","short");
			else
			{
				if("normal".startsWith(args[13])) return Arrays.asList("normal");
				else if("short".startsWith(args[13])) return Arrays.asList("short");
			}
		}
		return Arrays.asList();
	}

	@Override
	public boolean execute(CommandSender sender, String alias, String[] args)
	{
		if(args.length < 6)
		{
			sender.sendMessage(ChatColor.DARK_RED + "Not enough arguments");
			return false;
		}
		World w = null;
		if(args.length > 14)
		{
			w = Bukkit.getWorld(args[14]);
			if(w == null)
			{
				sender.sendMessage(ChatColor.DARK_RED + "Could not find world: " + args[10]);
				return false;
			}
		}
		else
		{
			if(sender instanceof Entity)
			{
				w = ((Entity) sender).getWorld();
			}
			else if(sender instanceof BlockCommandSender)
			{
				w = ((BlockCommandSender)sender).getBlock().getWorld();
			}
			else
			{
				sender.sendMessage(ChatColor.DARK_RED + "Command sender cannot be console without specifying world");
				return false;
			}
		}
		if(!(sender.hasPermission("mazegenerator.world.*") || sender.hasPermission("mazegenerator.world." + w.getName())))
		{
			sender.sendMessage(ChatColor.DARK_RED + "You do not have permission to run MazeGenerator commands for this world");
			return false;
		}
		int[] coords = new int[6];
		boolean relativeValid = false;
		Location senderCoords = null;
		if(sender instanceof Entity)
		{
			senderCoords = ((Entity) sender).getLocation();
			relativeValid = true;
		}
		else if(sender instanceof BlockCommandSender)
		{
			senderCoords = ((BlockCommandSender) sender).getBlock().getLocation();
			relativeValid = true;
		}
		for(int i = 0; i < 6; i ++)
		{
			String cArg = args[i] + "";
			if(args[i].startsWith("~"))
			{
				if(relativeValid)
				{
					cArg = cArg.substring(1);
					if(cArg.equals("")) cArg = "0";
					switch(i % 3)
					{
						case 0: 
						{
							coords[i] = senderCoords.getBlockX();
							break;
						}
						case 1:
						{
							coords[i] = senderCoords.getBlockY();
							break;
						}
						case 2:
						{
							coords[i] = senderCoords.getBlockZ();
							break;
						}
					}
				}
				else
				{
					sender.sendMessage(ChatColor.DARK_RED + "Relative coordinates are not applicable from console");
					return false;
				}
			}
			try
			{
				coords[i] += Integer.valueOf(cArg);
			}
			catch(Exception e)
			{
				sender.sendMessage(ChatColor.DARK_RED + "Could not parse integer: " + args[i]);
				return false;
			}
		}
		BlockData wallBlock = Material.STONE.createBlockData();
		int pathWidth = 1;
		double loopPercent = 0;
		boolean includeFloor = true;
		BlockData floorBlock = wallBlock;
		boolean includeCeil = true;
		BlockData ceilBlock = wallBlock;
		long seed = new Random().nextLong();
		Random r = new Random(seed);
		boolean genMaze = true;
		switch(args.length)
		{
			case 16:
			{
				try
				{
					seed = Long.valueOf(args[15]);
				}
				catch(Exception e)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Could not parse long: " + args[15]);
					return false;
				}
				r = new Random(seed);
			}
			case 15: case 14:
			{
				if(args[13].equals("normal"))
				{
					genMaze = true;
				}
				else if(args[13].equals("short"))
				{
					genMaze = false;
				}
				else
				{
					sender.sendMessage(ChatColor.DARK_RED + args[13] + " is not a valid mode. Valid modes: normal. short.");
					return false;
				}
			}
			case 13:
			{
				try
				{
					if(Boolean.valueOf(args[11]))
					{
						try
						{
							ceilBlock = Bukkit.createBlockData((String)args[12]);
						}
						catch(Exception e)
						{
							sender.sendMessage(ChatColor.DARK_RED + "Could not parse block: " + args[12]);
							return false;
						}
					}
				}
				catch(Exception e)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Could not parse boolean: " + args[11]);
					return false;
				}
			}
			case 12:
			{
				try
				{
					includeCeil = Boolean.valueOf(args[11]);
				}
				catch(Exception e)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Could not parse boolean: " + args[11]);
					return false;
				}
			}
			case 11:
			{
				try
				{
					if(Boolean.valueOf(args[9]))
					{
						try
						{
							floorBlock = Bukkit.createBlockData((String)args[10]);
						}
						catch(Exception e)
						{
							sender.sendMessage(ChatColor.DARK_RED + "Could not parse block: " + args[10]);
							return false;
						}
					}
				}
				catch(Exception e)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Could not parse boolean: " + args[9]);
					return false;
				}
			}
			case 10:
			{
				try
				{
					includeFloor = Boolean.valueOf(args[9]);
				}
				catch(Exception e)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Could not parse boolean: " + args[9]);
					return false;
				}
			}
			case 9:
			{
				try
				{
					loopPercent = Double.valueOf(args[8]) / 100;
				}
				catch(Exception e)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Could not parse double: " + args[8]);
					return false;
				}
				if(!(loopPercent >= 0 && loopPercent <= 1))
				{
					sender.sendMessage(ChatColor.DARK_RED + "Loop percent out of range (0-100");
					return false;
				}
			}
			case 8:
			{
				try
				{
					pathWidth = Integer.valueOf(args[7]);
				}
				catch(Exception e)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Could not parse integer: " + args[7]);
					return false;
				}
				if(pathWidth < 1)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Path width must be at least 1");
					return false;
				}
			}
			case 7:
			{
				try
				{
					wallBlock = Bukkit.createBlockData((String)args[6]);
				}
				catch(Exception e)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Could not parse block: " + args[6]);
					return false;
				}
			}
			case 6: break;
			default:
			{
				sender.sendMessage(ChatColor.DARK_RED + "Invalid number of arguments");
				return false;
			}
		}
		int height = Math.abs(coords[4] - coords[1]);
		if(height >= ((includeFloor ? 1 : 0) + (includeCeil ? 1 : 0)))
		{
			int distanceX = Math.abs(coords[0] - coords[3]);
			int distanceZ = Math.abs(coords[2] - coords[5]);
			int[] limits = MazeGeneratorPlugin.instance.getLimits();
			int mazeWidth = (int) ((double)distanceX / (2 * pathWidth) - 0.5);
			if((((mazeWidth + 1) * 2 + 1) * pathWidth) <= distanceX) mazeWidth ++;
			int mazeHeight = (int) Math.round(((double)distanceZ / (2 * pathWidth) - 0.5));
			if((((mazeHeight + 1) * 2 + 1) * pathWidth) <= distanceZ) mazeHeight ++;
			if(!sender.hasPermission("mazegenerator.overridelimit"))
			{
				if(distanceX > limits[0] && limits[0] > -1)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Exceeded x limit. Limits:\nX: " + limits[0] + "\nY: " + limits[1] + "\nZ: " + limits[2]);
					return false;
				}	
				else if(height > limits[1] && limits[1] > -1)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Exceeded y limit. Limits:\nX: " + limits[0] + "\nY: " + limits[1] + "\nZ: " + limits[2]);
					return false;
				}
				else if(distanceZ > limits[2] && limits[2] > -1)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Exceeded z limit. Limits:\nX: " + limits[0] + "\nY: " + limits[1] + "\nZ: " + limits[2]);
					return false;
				}
			}
			if(mazeWidth < 1)
			{
				sender.sendMessage(ChatColor.DARK_RED + "X coords too close together");
			}
			else if(mazeHeight < 1)
			{
				sender.sendMessage(ChatColor.DARK_RED + "Z coords too close together");
			}
			else
			{
				if(w.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK))
					sender.sendMessage("Parameters valid. Seed: " + seed);
				new MazeBuilder(mazeWidth, mazeHeight, pathWidth, loopPercent, r, coords, includeFloor, includeCeil, wallBlock, floorBlock, ceilBlock, sender, w, genMaze).start();
				return true;
			}
		}
		else
		{
			sender.sendMessage(ChatColor.DARK_RED + "Y coords too close together");
		}
		return false;
	}
}
