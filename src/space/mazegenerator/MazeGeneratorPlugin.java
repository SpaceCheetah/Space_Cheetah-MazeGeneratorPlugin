package space.mazegenerator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.help.HelpMap;
import org.bukkit.help.HelpTopic;
import org.bukkit.help.IndexHelpTopic;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.BooleanArgument;
import io.github.jorelali.commandapi.api.arguments.CustomArgument;
import io.github.jorelali.commandapi.api.arguments.CustomArgument.CustomArgumentException;
import io.github.jorelali.commandapi.api.arguments.CustomArgument.CustomArgumentFunction;
import io.github.jorelali.commandapi.api.arguments.CustomArgument.MessageBuilder;
import io.github.jorelali.commandapi.api.arguments.DoubleArgument;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;
import io.github.jorelali.commandapi.api.arguments.LocationArgument;
import io.github.jorelali.commandapi.api.arguments.LocationArgument.LocationType;
import io.github.jorelali.commandapi.api.arguments.PotionEffectArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

public class MazeGeneratorPlugin extends JavaPlugin implements Listener
{
	public static MazeGeneratorPlugin instance;
	public LinkedHashMap<String, ArrayList<AreaEffect>> effects = new LinkedHashMap();
	public class AreaEffect
	{
		private BoundingBox b;
		private PotionEffect p;
		public AreaEffect(BoundingBox b, PotionEffect p)
		{
			this.b = b;
			this.p = p;
		}
		
		public void writeTo(List<String> aes)
		{
			aes.add(String.valueOf(b.getMinX()));
			aes.add(String.valueOf(b.getMinY()));
			aes.add(String.valueOf(b.getMinZ()));
			aes.add(String.valueOf(b.getMaxX()));
			aes.add(String.valueOf(b.getMaxY()));
			aes.add(String.valueOf(b.getMaxZ()));
			aes.add(p.getType().getName());
			aes.add(String.valueOf(p.getAmplifier()));
		}
		
		public boolean getIntersects(Vector pos)
		{
			return pos.getX() + 0.2 >= b.getMinX() && pos.getX() - 0.2 <= b.getMaxX() && pos.getY() + 0.2 >= b.getMinY() && pos.getY() - 0.2 <= b.getMaxY() && pos.getZ() + 0.2 >= b.getMinZ() && pos.getZ() - 0.2 <= b.getMaxZ();
		}
	}
	@Override
	public void onLoad()
	{
		//Register commands on load so they can be used in functions (if CommandAPI is present)
		if(Bukkit.getPluginManager().getPlugin("CommandAPI") != null)
		{
			registerGenMaze();
			registerGenMaze3d();
			registerAreaEffect();
		}
		else
		{
			try
			{
				registerCommand(new GenMazeCommand());
				registerCommand(new AreaEffectCommand());
				registerCommand(new GenMaze3dCommand());
			}
			catch (ReflectiveOperationException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	private void registerCommand(Command whatCommand) throws ReflectiveOperationException
	{
        Method commandMap = Bukkit.getServer().getClass().getMethod("getCommandMap", null);
        Object cmdmap = commandMap.invoke(Bukkit.getServer(), null);
        Method register = cmdmap.getClass().getMethod("register", String.class,Command.class);
        register.invoke(cmdmap, "mazegenerator", whatCommand);
    }
	
	@Override
	public void onEnable()
	{
		saveDefaultConfig();
		
		instance = this;
		//Edit help after 1 tick; I found this was necessary to let Bukkit generate the help topics first.
		Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> editHelp(), 1L);
		Bukkit.getPluginManager().registerEvents(this, this);
		File effectsFile = new File(getDataFolder(), "effects.yml");
		if(!effectsFile.exists())
		{
			try
			{
				effectsFile.createNewFile();
			} catch (IOException e)
			{
				getLogger().warning("Could not create effects.yml");
			}
		}
		//Read effects.yml
		FileConfiguration effects = YamlConfiguration.loadConfiguration(effectsFile);
		for(String s : effects.getKeys(false))
		{
			ArrayList<AreaEffect> effectT = new ArrayList<AreaEffect>();
			List<String> effectS = effects.getStringList(s);
			double sx = 0;
			double sy = 0;
			double sz = 0;
			double ex = 0;
			double ey = 0;
			double ez = 0;
			String name = null;
			int amplifier = 0;
			int i2 = 0;
			for(int i = 0; i < effectS.size(); i ++)
			{
				switch(i % 8)
				{
					case 0:
					{
						if(i > 1)
						{
							effectT.add(new AreaEffect(new BoundingBox(sx,sy,sz,ex,ey,ez), new PotionEffect(PotionEffectType.getByName(name), 2, amplifier, true, false)));
						}
						sx = Double.valueOf(effectS.get(i));
						break;
					}
					case 1:
					{
						sy = Double.valueOf(effectS.get(i));
						break;
					}
					case 2:
					{
						sz = Double.valueOf(effectS.get(i));
						break;
					}
					case 3:
					{
						ex = Double.valueOf(effectS.get(i));
						break;
					}
					case 4:
					{
						ey = Double.valueOf(effectS.get(i));
						break;
					}
					case 5:
					{
						ez = Double.valueOf(effectS.get(i));
						break;
					}
					case 6:
					{
						name = effectS.get(i);
						break;
					}
					case 7:
					{
						amplifier = Integer.valueOf(effectS.get(i));
						break;
					}
				}
				i2 = i;
			}
			if(!(i2 % 8 == 0))
			{
				effectT.add(new AreaEffect(new BoundingBox(sx,sy,sz,ex,ey,ez), new PotionEffect(PotionEffectType.getByName(name), 20, amplifier, true, false)));
			}
			this.effects.put(s, effectT);
		}
	}
	
	@EventHandler
	public void playerMoveEvent(PlayerMoveEvent event)
	{
		if(effects.containsKey(event.getPlayer().getWorld().getName()))
		{
			ArrayList<AreaEffect> aes = effects.get(event.getPlayer().getWorld().getName());
			for(AreaEffect ae : aes)
			{
				if(ae.getIntersects(event.getPlayer().getLocation().toVector()))
				{
					event.getPlayer().addPotionEffect(ae.p);
				}
			}
		}
	}
	
	private void editHelp()
	{
		HelpMap helpMap = Bukkit.getHelpMap();
		HelpTopic mazegen = helpMap.getHelpTopic("/genmaze");
		HelpTopic mazegen3d = helpMap.getHelpTopic("/genmaze3d");
		HelpTopic areaeffect = helpMap.getHelpTopic("/areaeffect");
		Collection<HelpTopic> topics = new ArrayList<HelpTopic>();
		topics.add(mazegen);
		topics.add(mazegen3d);
		topics.add(areaeffect);
		mazegen.amendTopic("Generate maze",ChatColor.GOLD + "Description: " + ChatColor.RESET + "Generate maze\n" + ChatColor.GOLD + "Usage:\n" + ChatColor.RESET + "genmaze <startPos> <endPos> [<wall_block>] [<path_width> (maze size multiplier)] [<loop_percent> (percentage of walls to break)] [<include_floor>] [<floor_block>] [<include_ceiling>] [<ceiling_block>] [<normal : short> (short tends twoards shorter paths and more dead ends)] [<world>] [<seed>]");
		mazegen3d.amendTopic("Generate multi-level maze",ChatColor.GOLD + "Description: " + ChatColor.RESET + "Generate multi-level maze\n" + ChatColor.GOLD + "Usage:\n" + ChatColor.RESET + "genmaze3d <startPos> <endPos> [<wall_block>] [<floor_block>] [<loop_percent> (percentage of walls to break)] [<path_width> (maze size multiplier>] [<path_height>] [<normal : short> (short tends twoards shorter paths and more dead ends)] [<world>] [<seed>]");
		areaeffect.amendTopic("Add or remove area effect", ChatColor.GOLD + "Description: " + ChatColor.RESET + "Add or remove area effect\n" + ChatColor.GOLD + "Usage:\n" + ChatColor.RESET + "areaeffect add <startPos> <endPos> <effect> [<amplifier>] [<world>] (add area effect)\nareaeffect remove <pos> [<world>] (remove all area effects that intersect pos)");
		helpMap.addTopic(new IndexHelpTopic("MazeGenerator","All commands for MazeGenerator","mazegenerator.help",topics,ChatColor.GRAY + "Below is a list of all MazeGenerator commands:"));
	}
	
	//Get the limits from the config; it reloads each time, which at the cost of a tiny bit of time allows config edits without restarting
	public int[] getLimits()
	{
		reloadConfig();
		FileConfiguration config = getConfig();
		return new int[] {config.getInt("limits.x"),config.getInt("limits.y"),config.getInt("limits.z")};
	}
	
	//Same as above but for allowed lag. Possibly should've just added it as a term in the limits.
	public int getAllowedLag()
	{
		reloadConfig();
		return getConfig().getInt("lag-per-tick");
	}
	
	//Essentially two commands in one
	private void registerAreaEffect()
	{
		LinkedHashMap<String, Argument> addArguments = new LinkedHashMap();
		LinkedHashMap<String, Argument> removeArguments = new LinkedHashMap();
		addArguments.put("add", new LiteralArgument("add"));
		removeArguments.put("remove", new LiteralArgument("remove"));
		addArguments.put("start", new LocationArgument());
		addArguments.put("end", new LocationArgument());
		addArguments.put("effect", new PotionEffectArgument());
		registerAddAreaEffect(addArguments);
		addArguments.put("amplifier", new IntegerArgument(0, 255));
		registerAddAreaEffect(addArguments);
		removeArguments.put("pos", new LocationArgument());
		registerRemoveAreaEffect(removeArguments);
		addArguments.put("world", new CustomArgument((input) -> {
			World w = Bukkit.getWorld(input);
			if(w == null)
			{
				CustomArgument.throwError(new MessageBuilder("Could not find world: ").appendArgInput());
			}
			return w;
		}));
		registerAddAreaEffect(addArguments);
		removeArguments.put("world", new CustomArgument((input) -> {
			World w = Bukkit.getWorld(input);
			if(w == null)
			{
				CustomArgument.throwError(new MessageBuilder("Could not find world: ").appendArgInput());
			}
			return w;
		}));
		registerRemoveAreaEffect(removeArguments);
	}
	
	private void registerRemoveAreaEffect(LinkedHashMap<String, Argument> arguments)
	{
		CommandAPI.getInstance().register("areaeffect", CommandPermission.fromString("mazegenerator.areaeffect"), arguments, (sender, args) -> {
			World w = null;
			if(args.length == 2) w = (World)args[1];
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
					CommandAPI.fail("Command sender cannot be console without specifying world");
					return;
				}
			}
			if(!(sender.hasPermission("mazegenerator.world.*") || sender.hasPermission("mazegenerator.world." + w.getName())))
			{
				CommandAPI.fail("You do not have permission to run MazeGenerator commands for this world");
				return;
			}
			ArrayList<AreaEffect> aeffects = effects.get(w.getName());
			if(aeffects == null)
			{
				CommandAPI.fail("No effects found");
			}
			else
			{
				Location pos = (Location)args[0];
				ArrayList<AreaEffect> toRemove = new ArrayList<AreaEffect>();
				int removed = 0;
				for(AreaEffect effect : aeffects)
				{
					if(effect.getIntersects(pos.toVector()))
					{
						toRemove.add(effect);
						removed ++;
					}
				}
				if(removed == 0)
				{
					CommandAPI.fail("No effects found");
					return;
				}
				if(w.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK))
				{
					sender.sendMessage(removed + " effects removed");
				}
				aeffects.removeAll(toRemove);
				FileConfiguration effects = YamlConfiguration.loadConfiguration(new File(getDataFolder(),"effects.yml"));
				List<String> aes = new ArrayList<String>();
				//Write the new effects list after any intersecting ones were removed
				for(AreaEffect effect : aeffects)
				{
					effect.writeTo(aes);
				}
				effects.set(w.getName(), aes);
				try
				{
					effects.save(new File(getDataFolder(),"effects.yml"));
				}
				catch (IOException e)
				{
					getLogger().warning("Could not save effects.yml");
				}
			}
		});
	}
	
	private void registerAddAreaEffect(LinkedHashMap<String, Argument> arguments)
	{
		CommandAPI.getInstance().register("areaeffect", CommandPermission.fromString("mazegenerator.areaeffect"), arguments, (sender, args) -> {
			FileConfiguration effects = YamlConfiguration.loadConfiguration(new File(getDataFolder(),"effects.yml"));
			World w = null;
			if(args.length == 5) w = (World)args[4];
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
					CommandAPI.fail("Command sender cannot be console without specifying world");
					return;
				}
			}
			if(!(sender.hasPermission("mazegenerator.world.*") || sender.hasPermission("mazegenerator.world." + w.getName())))
			{
				CommandAPI.fail("You do not have permission to run MazeGenerator commands for this world");
				return;
			}
			PotionEffectType p = (PotionEffectType)args[2];
			Location start = (Location)args[0];
			Location end = (Location)args[1];
			int amplifier = 0;
			if(args.length > 3)
			{
				amplifier = (int)args[3];
			}
			PotionEffect effect = new PotionEffect(p, 20, amplifier, true, false);
			ArrayList<AreaEffect> potions;
			if(this.effects.containsKey(w.getName()))
			{
				potions = this.effects.get(w.getName());
			}
			else
			{
				potions = new ArrayList<AreaEffect>();
				this.effects.put(w.getName(), potions);
			}
			double sx = Math.min(start.getX(), end.getX());
			double sy = Math.min(start.getY(), end.getY());
			double sz = Math.min(start.getZ(), end.getZ());
			double ex = Math.max(start.getX(), end.getX());
			double ey = Math.max(start.getY(), end.getY());
			double ez = Math.max(start.getZ(), end.getZ());
			AreaEffect ae = new AreaEffect(new BoundingBox(sx, sy, sz, ex, ey, ez), effect);
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
				effects.save(new File(getDataFolder(),"effects.yml"));
			}
			catch (IOException e)
			{
				getLogger().warning("Could not save effects.yml");
			}
			if(w.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK))
			{
				sender.sendMessage("Potion effect added");
			}
		});
	}
	
	/*
	 * Register genMaze3d, with all the arguments except start and end optional. I would've used a Literal argument for genMode, but
	 * I couldn't get CommandAPI to work with me on that; possibly just the number of other arguments?
	 */
	private void registerGenMaze3d()
	{
		this.getLogger().info("Registering genmaze3d");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap();
		arguments.put("start", new LocationArgument());
		arguments.put("end", new LocationArgument());
		registerGenMazePart3d(arguments);
		arguments.put("wall_block", new TextArgument());
		registerGenMazePart3d(arguments);
		arguments.put("floor_block", new TextArgument());
		registerGenMazePart3d(arguments);
		arguments.put("loop_percent", new DoubleArgument(0, 100));
		registerGenMazePart3d(arguments);
		arguments.put("path_width", new IntegerArgument(1));
		registerGenMazePart3d(arguments);
		arguments.put("path_height", new IntegerArgument(1));
		registerGenMazePart3d(arguments);
		arguments.put("gen_mode", new StringArgument().overrideSuggestions("normal","short"));
		registerGenMazePart3d(arguments);
		arguments.put("world", new CustomArgument((input) -> {
			World w = Bukkit.getWorld(input);
			if(w == null)
			{
				CustomArgument.throwError(new MessageBuilder("Could not find world: ").appendArgInput());
			}
			return w;
		}));
		registerGenMazePart3d(arguments);
		arguments.put("seed", new IntegerArgument());
		registerGenMazePart3d(arguments);
	}
	
	/*
	 * Parse arguments and then if valid send the result to the 3d builder (well, create a thread; while the placing has to be done
	 * on the main thread due to Bukkit limitations, the maze can be generated in an async thread. I've never had the maze take long
	 * enough to generate to have that matter, but I suppose it could if it it was a really big maze.)
	 */
	private void registerGenMazePart3d(LinkedHashMap<String, Argument> arguments)
	{
		CommandAPI.getInstance().register("genmaze3d", CommandPermission.fromString("mazegenerator.genmaze3d"), arguments, (sender, args) -> {
			World w = null;
			if(args.length > 9) w = (World)args[8];
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
					CommandAPI.fail("Command sender cannot be console without specifying world");
					return;
				}
			}
			if(!(sender.hasPermission("mazegenerator.world.*") || sender.hasPermission("mazegenerator.world." + w.getName())))
			{
				CommandAPI.fail("You do not have permission to run MazeGenerator commands for this world");
				return;
			}
			Location start = (Location)args[0];
			Location end = (Location)args[1];
			BlockData wallBlock = Material.STONE.createBlockData();
			BlockData floorBlock = wallBlock;
			int pathWidth = 1;
			int pathHeight = 2;
			int seed = new Random().nextInt();
			double loopPercent = 0;
			boolean genMaze = true;
			switch(args.length)
			{
				case 10:
				{
					seed = (int)args[9];
				}
				case 9: case 8:
				{
					if(args[7].equals("normal"))
					{
						genMaze = true;
					}
					else if(args[7].equals("short"))
					{
						genMaze = false;
					}
					else
					{
						CommandAPI.fail(args[7] + " is not a valid mode. Valid modes: normal. short.");
						return;
					}
				}
				case 7:
				{
					pathHeight = (int)args[6];
				}
				case 6:
				{
					pathWidth = (int)args[5];
				}
				case 5:
				{
					loopPercent = ((double)args[4]) / 100;
				}
				case 4:
				{
					try
					{
						floorBlock = Bukkit.createBlockData((String)args[3]);
					}
					catch(Exception e)
					{
						CommandAPI.fail("Could not parse block: " + args[3]);
						return;
					}
				}
				case 3:
				{
					try
					{
						wallBlock = Bukkit.createBlockData((String)args[2]);
					}
					catch(Exception e)
					{
						CommandAPI.fail("Could not parse block: " + args[2]);
						return;
					}
				}
			}
			int distanceX = Math.abs(start.getBlockX() - end.getBlockX());
			int distanceY = Math.abs(start.getBlockY() - end.getBlockY());
			int distanceZ = Math.abs(start.getBlockZ() - end.getBlockZ());
			int mazeWidth = (int) ((double)distanceX / (2 * pathWidth) - 0.5);
			if((((mazeWidth + 1) * 2 + 1) * pathWidth) <= distanceX) mazeWidth ++;
			int mazeHeight = (int) ((double)distanceZ / (2 * pathWidth) - 0.5);
			if((((mazeHeight + 1) * 2 + 1) * pathWidth) <= distanceZ) mazeHeight ++;
			int mazeLength = (int) ((double)(distanceY - 1) / (pathHeight + 1));
			if(((pathHeight + 1) * (mazeLength + 1) + 1) <= distanceY) mazeLength ++;
			if(mazeWidth < 1)
			{
				CommandAPI.fail("X coords too close together");
			}
			else if(mazeLength < 1)
			{
				CommandAPI.fail("Y coords too close together");
			}
			else if(mazeHeight < 1)
			{
				CommandAPI.fail("Z coords too close together");
			}
			else
			{
				if(!sender.hasPermission("mazegenerator.overridelimit"))
				{
					int[] limits = getLimits();
					if(distanceX > limits[0] && limits[0] > -1)
					{
						CommandAPI.fail("Exceeded x limit. Limits:\nX: " + limits[0] + "\nY: " + limits[1] + "\nZ: " + limits[2]);
						return;
					}	
					else if(distanceY > limits[1] && limits[1] > -1)
					{
						CommandAPI.fail("Exceeded y limit. Limits:\nX: " + limits[0] + "\nY: " + limits[1] + "\nZ: " + limits[2]);
						return;
					}
					else if(distanceZ > limits[2] && limits[2] > -1)
					{
						CommandAPI.fail("Exceeded z limit. Limits:\nX: " + limits[0] + "\nY: " + limits[1] + "\nZ: " + limits[2]);
						return;
					}
				}
				if(w.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK))
					sender.sendMessage("Parameters valid. Seed: " + seed);
				Random r = new Random(Integer.toUnsignedLong(seed));
				new MazeBuilder3D(w, sender, wallBlock, floorBlock, start, end, r, loopPercent, pathWidth, pathHeight, mazeWidth, mazeHeight, mazeLength, genMaze).start();
			}
		});
	}
	
	//same as for the 3d version
	private void registerGenMaze()
	{
		this.getLogger().info("Registering genmaze");
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap();
		arguments.put("start", new LocationArgument());
		arguments.put("end", new LocationArgument());
		registerGenMazePart(arguments);
		arguments.put("wall_block", new TextArgument());
		registerGenMazePart(arguments);
		arguments.put("path_width", new IntegerArgument(1));
		registerGenMazePart(arguments);
		arguments.put("loop_percent", new DoubleArgument(0,100));
		registerGenMazePart(arguments);
		arguments.put("include_floor", new BooleanArgument());
		registerGenMazePart(arguments);
		arguments.put("floor_block", new TextArgument());
		registerGenMazePart(arguments);
		arguments.put("include_ceil", new BooleanArgument());
		registerGenMazePart(arguments);
		arguments.put("ceil_block", new TextArgument());
		registerGenMazePart(arguments);
		arguments.put("gen_mode", new StringArgument().overrideSuggestions("normal", "short"));
		registerGenMazePart(arguments);
		arguments.put("world", new CustomArgument((input) -> {
			World w = Bukkit.getWorld(input);
			if(w == null)
			{
				CustomArgument.throwError(new MessageBuilder("Could not find world: ").appendArgInput());
			}
			return w;
		}));
		registerGenMazePart(arguments);
		arguments.put("seed", new IntegerArgument());
		registerGenMazePart(arguments);
	}
	
	//same as for the 3d version, except to the non-3d builder
	private void registerGenMazePart(LinkedHashMap<String,Argument> arguments)
	{
		CommandAPI.getInstance().register("genmaze", CommandPermission.fromString("mazegenerator.genmaze"), arguments, (sender, args) ->
		{
			World w = null;
			if(args.length > 10) w = (World)args[10];
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
					CommandAPI.fail("Command sender cannot be console without specifying world");
					return;
				}
			}
			if(!(sender.hasPermission("mazegenerator.world.*") || sender.hasPermission("mazegenerator.world." + w.getName())))
			{
				CommandAPI.fail("You do not have permission to run MazeGenerator commands for this world");
				return;
			}
			Location start = (Location)args[0];
			Location end = (Location)args[1];
			int[] coords = {start.getBlockX(),start.getBlockY(),start.getBlockZ(),end.getBlockX(),end.getBlockY(),end.getBlockZ()};
			BlockData wallBlock = Material.STONE.createBlockData();
			int pathWidth = 1;
			double loopPercent = 0;
			boolean includeFloor = true;
			BlockData floorBlock = wallBlock;
			boolean includeCeil = true;
			BlockData ceilBlock = wallBlock;
			int seed = new Random().nextInt();
			Random r = new Random(Integer.toUnsignedLong(seed));
			boolean genMaze = true;
			switch(args.length)
			{
				case 12:
				{
					r = new Random(Integer.toUnsignedLong((int)args[11]));
					seed = (int)args[11];
				}
				case 11: case 10:
				{
					if(args[9].equals("normal"))
					{
						genMaze = true;
					}
					else if(args[9].equals("short"))
					{
						genMaze = false;
					}
					else
					{
						CommandAPI.fail(args[9] + " is not a valid mode. Valid modes: normal. short.");
						return;
					}
				}
				case 9:
				{
					if((boolean)args[7])
					{
						try
						{
							ceilBlock = Bukkit.createBlockData((String)args[8]);
						}
						catch(Exception e)
						{
							CommandAPI.fail("Could not parse block: " + args[8]);
						}
					}
				}
				case 8:
				{
					includeCeil = (boolean)args[7];
				}
				case 7:
				{
					if((boolean)args[5])
					{
						try
						{
							floorBlock = Bukkit.createBlockData((String)args[6]);
						}
						catch(Exception e)
						{
							CommandAPI.fail("Could not parse block: " + args[6]);
						}
					}
				}
				case 6:
				{
					includeFloor = (boolean)args[5];
				}
				case 5:
				{
					loopPercent = (double)args[4] / 100;
				}
				case 4:
				{
					pathWidth = (int)args[3];
				}
				case 3:
				{
					try
					{
						wallBlock = Bukkit.createBlockData((String)args[2]);
					}
					catch(Exception e)
					{
						CommandAPI.fail("Could not parse block: " + args[2]);
					}
				}
			}
			int height = Math.abs(coords[4] - coords[1]);
			if(height >= ((includeFloor ? 1 : 0) + (includeCeil ? 1 : 0)))
			{
				int distanceX = Math.abs(coords[0] - coords[3]);
				int distanceZ = Math.abs(coords[2] - coords[5]);
				int[] limits = getLimits();
				int mazeWidth = (int) ((double)distanceX / (2 * pathWidth) - 0.5);
				if((((mazeWidth + 1) * 2 + 1) * pathWidth) <= distanceX) mazeWidth ++;
				int mazeHeight = (int) Math.round(((double)distanceZ / (2 * pathWidth) - 0.5));
				if((((mazeHeight + 1) * 2 + 1) * pathWidth) <= distanceZ) mazeHeight ++;
				if(!sender.hasPermission("mazegenerator.overridelimit"))
				{
					if(distanceX > limits[0] && limits[0] > -1)
					{
						CommandAPI.fail("Exceeded x limit. Limits:\nX: " + limits[0] + "\nY: " + limits[1] + "\nZ: " + limits[2]);
						return;
					}	
					else if(height > limits[1] && limits[1] > -1)
					{
						CommandAPI.fail("Exceeded y limit. Limits:\nX: " + limits[0] + "\nY: " + limits[1] + "\nZ: " + limits[2]);
						return;
					}
					else if(distanceZ > limits[2] && limits[2] > -1)
					{
						CommandAPI.fail("Exceeded z limit. Limits:\nX: " + limits[0] + "\nY: " + limits[1] + "\nZ: " + limits[2]);
						return;
					}
				}
				if(mazeWidth < 1)
				{
					CommandAPI.fail("X coords too close together");
				}
				else if(mazeHeight < 1)
				{
					CommandAPI.fail("Z coords too close together");
				}
				else
				{
					if(w.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK))
						sender.sendMessage("Parameters valid. Seed: " + seed);
					new MazeBuilder(mazeWidth, mazeHeight, pathWidth, loopPercent, r, coords, includeFloor, includeCeil, wallBlock, floorBlock, ceilBlock, sender, w, genMaze).start();
				}
			}
			else
			{
				CommandAPI.fail("Y coords too close together");
			}
		});
	}
}
