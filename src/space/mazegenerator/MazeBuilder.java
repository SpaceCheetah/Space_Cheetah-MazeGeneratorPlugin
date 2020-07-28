package space.mazegenerator;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

import org.bukkit.Bukkit;
import org.bukkit.GameRule;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;

public class MazeBuilder extends Thread
{
	private int width, height, pathWidth, x, y, z, dx, dy, dz;
	private int[] coords;
	private boolean includeFloor, includeCeil, genMode;
	private BlockData floorBlock, wallBlock, ceilBlock;
	private double loopPercent;
	private Random r;
	private CommandSender sender;
	private World w;
	public MazeBuilder(int width, int height, int pathWidth, double loopPercent, Random r, int[] coords, boolean includeFloor, boolean includeCeil, BlockData wallBlock, BlockData floorBlock, BlockData ceilBlock, CommandSender sender, World w, boolean genMode)
	{
		this.width = width;
		this.height = height;
		this.pathWidth = pathWidth;
		this.loopPercent = loopPercent;
		this.r = r;
		this.x = Math.min(coords[0], coords[3]);
		this.y = Math.min(coords[1], coords[4]);
		this.z = Math.min(coords[2], coords[5]);
		this.dx = Math.abs(coords[0] - coords[3]);
		this.dz = Math.abs(coords[2] - coords[5]);
		this.dy = Math.abs(coords[1] - coords[4]);
		this.coords = coords;
		this.includeCeil = includeCeil;
		this.includeFloor = includeFloor;
		this.wallBlock = wallBlock;
		this.ceilBlock = ceilBlock;
		this.floorBlock = floorBlock;
		this.sender = sender;
		this.w = w;
		this.genMode = genMode;
	}
	
	@Override
	//Generate maze, send feedback, then start building.
	public void run()
	{
		long startTime = new Date().getTime();
		ArrayList<String> maze = MazeGenerator.genMaze(width, height, pathWidth, loopPercent, r, genMode);
		if(w.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK))
		{
			sender.sendMessage("Maze generated. Time of generation: " + (new Date().getTime() - startTime) / 1000 + "s.");
			sender.sendMessage("Building.");
		}
		int allowedLag = MazeGeneratorPlugin.instance.getAllowedLag();
		Bukkit.getScheduler().scheduleSyncDelayedTask(MazeGeneratorPlugin.instance, () -> {
			new BuildCall(maze, allowedLag).start();
		});
	}
	private class BuildCall implements Runnable
	{
		private ArrayList<String> maze;
		private boolean stopped;
		private int delay, id, yoff, ydoff, dx2, dy2, dz2;
		private Timer t = new Timer();
		private long startTime;
		public BuildCall(ArrayList<String> maze, int delay)
		{
			this.maze = maze;
			this.delay = delay;
			if(includeFloor)
			{
				yoff ++;
				ydoff --;
			}
			if(includeCeil)
			{
				ydoff --;
			}
		}
		public void start()
		{
			id = Bukkit.getScheduler().scheduleSyncRepeatingTask(MazeGeneratorPlugin.instance, this, 1, 1);
			startTime = new Date().getTime();
		}
		private void recursiveForPartZ()
		{
			if(stopped) return;
			if(dz2 < maze.size())
			{
				recursiveForPartX();
				if(stopped) return;
				dx2 = 0;
				dz2 ++;
				recursiveForPartZ();
			}
		}
		private void recursiveForPartX()
		{
			if(stopped) return;
			if(dx2 < maze.get(0).length())
			{
				recursiveForPartY();
				if(stopped) return;
				if(includeFloor)
				{
					w.getBlockAt(x + dx2, y, z + dz2).setBlockData(floorBlock, false);
				}
				if(includeCeil)
				{
					w.getBlockAt(x + dx2, y + dy, z + dz2).setBlockData(ceilBlock, false);
				}
				dy2 = 0;
				dx2 ++;
				recursiveForPartX();
			}
		}
		private void recursiveForPartY()
		{
			if(stopped) return;
			if(dy2 < dy + 1 + ydoff)
			{
				if(maze.get(dz2).charAt(dx2) == 'W')
				{
					w.getBlockAt(x + dx2, y + yoff + dy2, z + dz2).setBlockData(wallBlock, false);
				}
				else
				{
					w.getBlockAt(x + dx2, y + yoff + dy2, z + dz2).setType(Material.AIR, false);
				}
				dy2 ++;
				recursiveForPartY();
			}
		}
		@Override
		public void run()
		{
			stopped = false;
			if(delay > -1)
			{
				t.schedule(new TimerTask() {
					@Override
					public void run()
					{
						stopped = true;
					}
				}, delay);
			}
			recursiveForPartZ();
			if(stopped) return;
			if(w.getGameRuleValue(GameRule.SEND_COMMAND_FEEDBACK))
				sender.sendMessage("Success. Time: " + (new Date().getTime() - startTime) / 1000 + "s");
			Bukkit.getScheduler().cancelTask(id);
		}
	}
}
