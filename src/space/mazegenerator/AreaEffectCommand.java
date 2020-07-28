package space.mazegenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import net.md_5.bungee.api.ChatColor;

import space.mazegenerator.MazeGeneratorPlugin.AreaEffect;

public class AreaEffectCommand extends BukkitCommand
{
	public AreaEffectCommand()
	{
		super("areaeffect", "", "", Arrays.asList("areaeffect"));
		this.setPermission("mazegenerator.areaeffect");
	}
	
	@Override
	public List<String> tabComplete(CommandSender sender, String alias, String[] args, Location location)
	{
		if(args.length == 1)
		{
			if(args[0].equals("")) return Arrays.asList("add","remove");
			else if("add".startsWith(args[0])) return Arrays.asList("add");
			else if("remove".startsWith(args[0])) return Arrays.asList("remove");
		}
		else
		{
			if(args[0].equals("add"))
			{
				if(args.length < 8)
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
				if(args.length == 8)
				{
					ArrayList<String> completions = new ArrayList<String>();
					for(PotionEffectType p : PotionEffectType.values())
					{
						if(p.getName().toLowerCase().startsWith(args[args.length - 1])) completions.add(p.getName().toLowerCase());
					}
					return completions;
				}
			}
			else if(args[0].equals("remove"))
			{
				if(args.length < 5)
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
			}
		}
		return Arrays.asList();
	}

	@Override
	public boolean execute(CommandSender sender, String alias, String[] args)
	{
		if(args.length == 0)
		{
			sender.sendMessage(ChatColor.DARK_RED + "Not enough arguments");
		}
		else
		{
			boolean type;
			if(args[0].equals("add"))
			{
				type = true;
			}
			else if(args[0].equals("remove"))
			{
				type = false;
			}
			else
			{
				sender.sendMessage(ChatColor.DARK_RED + "First argument must either be add or remove");
				return false;
			}
			if(args.length < (type ? 8 : 4))
			{
				sender.sendMessage(ChatColor.DARK_RED + "Not enough arguments");
				return false;
			}
			World w = null;
			if(args.length == (type ? 10 : 5))
			{
				w = Bukkit.getWorld(args[type ? 9 : 4]);
				if(w == null)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Could not find world: " + args[type ? 9 : 4]);
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
			if(type)
			{
				int[] coords = new int[6];
				for(int i = 0; i < 6; i ++)
				{
					String cArg = args[i + 1] + "";
					if(args[i + 1].startsWith("~"))
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
						sender.sendMessage(ChatColor.DARK_RED + "Could not parse integer: " + args[i + 1]);
						return false;
					}
				}
				FileConfiguration effects = YamlConfiguration.loadConfiguration(new File(MazeGeneratorPlugin.instance.getDataFolder(),"effects.yml"));
				PotionEffectType p = PotionEffectType.getByName(args[7].toUpperCase());
				if(p == null)
				{
					sender.sendMessage(ChatColor.DARK_RED + "Could not parse effect: " + args[7]);
				}
				int amplifier = 0;
				if(args.length > 8)
				{
					try
					{
						amplifier = Integer.valueOf(args[8]);
					}
					catch(Exception e)
					{
						sender.sendMessage(ChatColor.DARK_RED + "Could not parse integer: " + args[7]);
					}
				}
				PotionEffect effect = new PotionEffect(p, 20, amplifier, true, false);
				ArrayList<AreaEffect> potions;
				if(MazeGeneratorPlugin.instance.effects.containsKey(w.getName()))
				{
					potions = MazeGeneratorPlugin.instance.effects.get(w.getName());
				}
				else
				{
					potions = new ArrayList<AreaEffect>();
					MazeGeneratorPlugin.instance.effects.put(w.getName(), potions);
				}
				double sx = Math.min(coords[0], coords[3]);
				double sy = Math.min(coords[1], coords[4]);
				double sz = Math.min(coords[2], coords[5]);
				double ex = Math.max(coords[0], coords[3]);
				double ey = Math.max(coords[1], coords[4]);
				double ez = Math.max(coords[2], coords[5]);
				AreaEffect ae = MazeGeneratorPlugin.instance.new AreaEffect(new BoundingBox(sx, sy, sz, ex, ey, ez), effect);
				potions.add(ae);
				List<String> aes = new ArrayList<String>();
				if(effects.contains(w.getName()))
				{
					aes = effects.getStringList(w.getName());
				}
				ae.writeTo(aes);
				effects.set(w.getName(), aes);
				try
				{
					effects.save(new File(MazeGeneratorPlugin.instance.getDataFolder(),"effects.yml"));
				}
				catch (IOException e)
				{
					MazeGeneratorPlugin.instance.getLogger().warning("Could not save effects.yml");
				}
				if(w.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK))
				{
					sender.sendMessage("Potion effect added");
				}
			}
			else
			{
				int[] coords = new int[3];
				for(int i = 0; i < 3; i ++)
				{
					String cArg = args[i + 1] + "";
					if(args[i + 1].startsWith("~"))
					{
						if(relativeValid)
						{
							cArg = cArg.substring(1);
							if(cArg.equals("")) cArg = "0";
							switch(i)
							{
								case 1: 
								{
									coords[i - 1] = senderCoords.getBlockX();
									break;
								}
								case 2:
								{
									coords[i - 1] = senderCoords.getBlockY();
									break;
								}
								case 3:
								{
									coords[i - 1] = senderCoords.getBlockZ();
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
						sender.sendMessage(ChatColor.DARK_RED + "Could not parse integer: " + args[i] + 1);
						return false;
					}
				}
				ArrayList<AreaEffect> aeffects = MazeGeneratorPlugin.instance.effects.get(w.getName());
				if(aeffects == null)
				{
					sender.sendMessage(ChatColor.DARK_RED + "No effects found");
				}
				else
				{
					Vector pos = new Vector(coords[0], coords[1], coords[2]);
					ArrayList<AreaEffect> toRemove = new ArrayList<AreaEffect>();
					int removed = 0;
					for(AreaEffect effect : aeffects)
					{
						if(effect.getIntersects(pos))
						{
							toRemove.add(effect);
							removed ++;
						}
					}
					if(removed == 0)
					{
						sender.sendMessage(ChatColor.DARK_RED + "No effects found");
						return false;
					}
					if(w.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK))
					{
						sender.sendMessage(removed + " effects removed");
					}
					aeffects.removeAll(toRemove);
					FileConfiguration effects = YamlConfiguration.loadConfiguration(new File(MazeGeneratorPlugin.instance.getDataFolder(),"effects.yml"));
					List<String> aes = new ArrayList<String>();
					//Write the new effects list after any intersecting ones were removed
					for(AreaEffect effect : aeffects)
					{
						effect.writeTo(aes);
					}
					effects.set(w.getName(), aes);
					try
					{
						effects.save(new File(MazeGeneratorPlugin.instance.getDataFolder(),"effects.yml"));
					}
					catch (IOException e)
					{
						MazeGeneratorPlugin.instance.getLogger().warning("Could not save effects.yml");
					}
					return true;
				}
			}
		}
		return false;
	}
}
