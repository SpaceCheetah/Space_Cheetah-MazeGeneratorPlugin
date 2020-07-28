package space.mazegenerator;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;

public class MazeBuilder3D extends Thread
{
	private World w;
	private CommandSender sender;
	private BlockData wallBlock, floorBlock;
	private Location start, end;
	private Random r;
	private boolean running, genMode;
	private double loopPercent;
	private int pathWidth, pathHeight, width, height, length;
	public MazeBuilder3D(World w, CommandSender sender, BlockData wallBlock, BlockData floorBlock, Location start, Location end, Random r, double loopPercent, int pathWidth, int pathHeight, int width, int height, int length, boolean genMode)
	{
		this.w = w;
		this.sender = sender;
		this.wallBlock = wallBlock;
		this.floorBlock = floorBlock;
		this.start = start;
		this.end = end;
		this.r = r;
		this.genMode = genMode;
		this.loopPercent = loopPercent;
		this.pathWidth = pathWidth;
		this.pathHeight = pathHeight;
		this.width = width;
		this.height = height;
		this.length = length;
	}
	
	@Override
	//Generate the maze then start building.
	public void run()
	{
		long startTime = new Date().getTime();
		ArrayList<ArrayList<String>> maze = MazeGenerator.genMaze3d(width, height, length, pathHeight, pathWidth, loopPercent, r, genMode);
		if(w.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK))
		{
			sender.sendMessage("Maze generated. Time of generation: " + (new Date().getTime() - startTime) / 1000 + "s.");
			sender.sendMessage("Building.");
		}
		int allowedLag = MazeGeneratorPlugin.instance.getAllowedLag();
		Bukkit.getScheduler().scheduleSyncDelayedTask(MazeGeneratorPlugin.instance, () -> {
		new BuildCall(start, maze, allowedLag, pathHeight, w, floorBlock, wallBlock).start();
		});
	}
	
	/*
	 * For whatever reason, I had a lot of trouble with this class. While I could get it to generate in one tick easily, I kept
	 * running into errors when I tried to stagger it. I eventually landed on recursive methods emulating a for loop that worked
	 * for some reason, but I never did figure out why it failed in the first place; Its all in one thread, so I shouldn't have to worry
	 * about threading issues.
	 */
	private class BuildCall implements Runnable
	{
		private Location start;
		private ArrayList<ArrayList<String>> maze;
		private int allowedLag, id, pathHeight, dx, dy, dz;
		private boolean stopped;
		private long startTime;
		private World w;
		private Timer t = new Timer();
		private BlockData floorBlock, wallBlock;
		public BuildCall(Location start, ArrayList<ArrayList<String>> maze, int allowedLag, int pathHeight, World w, BlockData floorBlock, BlockData wallBlock)
		{
			this.start = start;
			this.maze = maze;
			this.allowedLag = allowedLag;
			this.w = w;
			this.floorBlock = floorBlock;
			this.wallBlock = wallBlock;
			this.pathHeight = pathHeight;
		}
		public void start()
		{
			id = Bukkit.getScheduler().scheduleSyncRepeatingTask(MazeGeneratorPlugin.instance, this, 1, 1);
			startTime = new Date().getTime();
		}
		private void recursiveForPartY()
		{
			if(stopped) return;
			if(dy < maze.size())
			{
				recursiveForPartZ();
				if(stopped) return;
				dz = 0;
				dy ++;
				recursiveForPartY();
			}
		}
		private void recursiveForPartZ()
		{
			if(stopped) return;
			if(dz < maze.get(0).size())
			{
				recursiveForPartX();
				if(stopped) return;
				dx = 0;
				dz ++;
				recursiveForPartZ();
			}
		}
		private void recursiveForPartX()
		{
			if(stopped) return;
			if(dx < maze.get(0).get(0).length())
			{
				char b = maze.get(dy).get(dz).toCharArray()[dx];
				if(b == 'W')
				{
					if((dy % (pathHeight + 1)) != 0)
					{
						w.getBlockAt(start.clone().add(dx, dy, dz)).setBlockData(wallBlock,false);
					}
					else
					{
						w.getBlockAt(start.clone().add(dx, dy, dz)).setBlockData(floorBlock, false);
					}
				}
				else if(b == 'P')
				{
					w.getBlockAt(start.clone().add(dx, dy, dz)).setType(Material.AIR,false);
				}
				else
				{
					w.getBlockAt(start.clone().add(dx, dy, dz)).setType(Material.WATER,false);
				}
				dx ++;
				recursiveForPartX();
			}
		}
		@Override
		public void run()
		{
			stopped = false;
			if(allowedLag > -1)
			{
				t.schedule(new TimerTask() {
					@Override
					public void run()
					{
						stopped = true;
					}
				}, allowedLag);
			}
			recursiveForPartY();
			if(stopped) return;
			if(w.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK))
				sender.sendMessage("Success. Time: " + (new Date().getTime() - startTime) / 1000 + "s");
			Bukkit.getScheduler().cancelTask(id);
		}
	}
}
