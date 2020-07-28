package space.mazegenerator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class MazeGenerator
{
	/*
	 * For all methods in here, z and y are swapped because of how the original code worked, and it would've been harder to fix
	 * then it is to simply deal with that. That also applies to the width and height arguments.
	 */
	
	//Method to generate multi-layer maze
	public static ArrayList<ArrayList<String>> genMaze3d(int width, int height, int length, int pathHeight, int pathWidth, double loopPercent, Random r, boolean genMode)
	{
		//Initialize values and select starting cell. The cells hold the information to create the maze, in the form of binary flags
		byte[][][] grid = new byte[height][width][length];
		int x = r.nextInt(width);
		int y = r.nextInt(height);
		int z = r.nextInt(length);
		ArrayList<Held> cells = new ArrayList<Held>();
		cells.add(new Held(x,y,z));
		ArrayList<Integer> directions = new ArrayList<Integer>();
		directions.add(1);
		directions.add(2);
		directions.add(4);
		directions.add(8);
		directions.add(16);
		directions.add(32);
		
		//Loop until there is no cell unconnected
		while(!cells.isEmpty())
		{
			int index = 0;
			/*
			 * The different generator modes, only two so represented with a boolean. Normal (genMode true) acts like a recursive
			 * backtracker and tends to have longer paths, while short tends to have shorter ones
			 */
			if(genMode)
			{
				index = cells.size() - 1;
			}
			else
			{
				index = r.nextInt(cells.size());
			}
			x = cells.get(index).x;
			y = cells.get(index).y;
			z = cells.get(index).z;
			
			//iterate through the directions in random order
			ArrayList<Integer> unselected = (ArrayList<Integer>) directions.clone();
			while(!unselected.isEmpty())
			{
				int indexS = r.nextInt(unselected.size());
				int direction = unselected.get(indexS);
				unselected.remove(indexS);
				
				//copy variables
				int nx = x;
				int ny = y;
				int nz = z;
				int opposite = 0;
				//change copy variables depending on current direction
				switch(direction)
				{
					case 1:
					{
						ny --;
						opposite = 4;
						break;
					}
					case 2:
					{
						nx ++;
						opposite = 8;
						break;
					}
					case 4:
					{
						ny ++;
						opposite = 1;
						break;
					}
					case 8:
					{
						nx --;
						opposite = 2;
						break;
					}
					case 16:
					{
						nz ++;
						opposite = 32;
						break;
					}
					case 32:
					{
						nz --;
						opposite = 16;
						break;
					}
				}
				//check to see if direction and cell is valid
				if(nx >= 0 && ny >= 0 && nz >= 0 && nx < width && ny < height && nz < length && grid[ny][nx][nz] == 0)
				{
					//set byte flags
					grid[y][x][z] |= direction;
					grid[ny][nx][nz] |= opposite;
					cells.add(new Held(nx,ny,nz));
					index = -1;
					break;
				}
			}
			//if cell didn't have any valid neighbors, remove it
			if(index != -1) cells.remove(index);		
		}
		
		int numWallsToBreak = (int)(loopPercent * width * height * length);
		//probably not the most efficient algorithm, but even with a loopPercent of 1 it goes quickly
		while(numWallsToBreak > 0)
		{
			int xtb = r.nextInt(width);
			int ytb = r.nextInt(height);
			int ztb = r.nextInt(length);
			//select one of the three controlling directions
			int direction = r.nextInt(3) * 2 + 2;
			if(direction == 6) direction = 16;
			//check if direction for cell is valid, and if so break the wall in that direction and decrement the counter
			if((grid[ytb][xtb][ztb] & direction) == 0 && !((ztb == length - 1 && direction == 16) || (ytb == height - 1 && direction == 4) || (xtb == width - 1 && direction == 2)))
			{
				grid[ytb][xtb][ztb] = (byte) (grid[ytb][xtb][ztb] | direction);
				numWallsToBreak --;
			}
		}
		//Fill a three-dimensional array of chars with W.
		char[][][] planes = new char[grid.length * 2][grid[0].length * 2][grid[0][0].length * 2];
		for(char[][] lines : planes)
		{
			for(char[] line : lines)
			{
				Arrays.fill(line, 'W');
			}
		}
		//Generate the string that gets placed at the top of each layer
		String ceilingW = "";
		for(int i = 0; i < (grid[0].length * 2 + 1) * pathWidth; i ++)
		{
			ceilingW = ceilingW + "W";
		}
		/*
		 * Change the chars corresponding to the cells and the open walls that they control. Different letter for the vertical direction
		 * as a flag to replace that letter with water instead of the wall block.
		 */
		for(int z2 = 0; z2 < grid[0][0].length; z2 ++)
		{
			for(int y2 = 0; y2 < grid.length; y2 ++)
			{
				for(int x2 = 0; x2 < grid[0].length; x2 ++)
				{
					planes[y2 * 2][x2 * 2][z2 * 2] = 'P';
					if((grid[y2][x2][z2] & 2) != 0)
					{
						planes[y2 * 2][x2 * 2 + 1][z2 * 2] = 'P';
					}
					if((grid[y2][x2][z2] & 4) != 0)
					{
						planes[y2 * 2 + 1][x2 * 2][z2 * 2] = 'P';
					}
					if((grid[y2][x2][z2] & 16) != 0)
					{
						planes[y2 * 2][x2 * 2][z2 * 2 + 1] = 'Q';
					}
				}
			}
		}
		//ArrayList<ArrayList<String>>? Given I knew the length, I probably should've just used a standard array, but it works so its fine.
		ArrayList<ArrayList<String>> layers = new ArrayList<ArrayList<String>>();
		//Create and add a layer to cover the maze.
		ArrayList<String> ceiling = new ArrayList<String>();
		for(int i = 0; i < (planes.length + 1) * pathWidth; i ++)
		{
			ceiling.add(ceilingW);
		}
		layers.add(ceiling);
		//Generate the final letter version of the maze, from the char array, pathWidth, and pathHeight.
		for(int z2 = 0; z2 < planes[0][0].length; z2 ++)
		{
			ArrayList<String> plane = new ArrayList<String>();
			String startLine = "";
			for(int i = 0; i < pathWidth; i ++)
			{
				plane.add(ceilingW);
				startLine = startLine + "W";
			}
			for(int p = 0; p < planes.length; p ++)
			{
				String toPrint = startLine + "";
				for(int p2 = 0; p2 < planes[0].length; p2 ++)
				{
					for(int i = 0; i < pathWidth; i ++)
					{
						toPrint = toPrint + planes[p][p2][z2];
					}
				}
				for(int i = 0; i < pathWidth; i ++)
				{
					plane.add(toPrint);
				}
			}
			if((z2 & 1) == 0)
			{
				for(int i = 0; i < pathHeight; i ++)
				{
					layers.add(plane);
				}
			}
			else
			{
				layers.add(plane);
			}
		}
		return layers;
	}
	
	//Convenience class for holding cell locations.
	private static class Held
	{
		public int x,y,z;
		public Held(int x, int y, int z)
		{
			this.x = x;
			this.y = y;
			this.z = z;
		}
		
		public Held(int x, int y)
		{
			this(x, y, 0);
		}
	}
	
	//Method to generate single-layer maze; see commends for the above method, its quite similar.
	public static ArrayList<String> genMaze(int width, int height, int pathWidth, double loopPercent, Random r, boolean genMode)
	{
		byte[][] grid = new byte[height][width];
		int x = r.nextInt(width);
		int y = r.nextInt(height);
		ArrayList<Held> cells = new ArrayList<Held>();
		cells.add(new Held(x,y));
		ArrayList<Integer> directions = new ArrayList<Integer>();
		directions.add(1);
		directions.add(2);
		directions.add(4);
		directions.add(8);
		
		while(!cells.isEmpty())
		{
			int index = 0;
			if(genMode)
			{
				index = cells.size() - 1;
			}
			else
			{
				index = r.nextInt(cells.size());
			}
			x = cells.get(index).x;
			y = cells.get(index).y;
			ArrayList<Integer> unselected = (ArrayList<Integer>) directions.clone();
			while(!unselected.isEmpty())
			{
				int indexS = r.nextInt(unselected.size());
				int direction = unselected.get(indexS);
				unselected.remove(indexS);
				int nx = x;
				int ny = y;
				int opposite = 0;
				switch(direction)
				{
					case 1:
					{
						ny --;
						opposite = 4;
						break;
					}
					case 2:
					{
						nx ++;
						opposite = 8;
						break;
					}
					case 4:
					{
						ny ++;
						opposite = 1;
						break;
					}
					case 8:
					{
						nx --;
						opposite = 2;
						break;
					}
				}
				if(nx >= 0 && ny >= 0 && nx < width && ny < height && grid[ny][nx] == 0)
				{
					grid[y][x] |= direction;
					grid[ny][nx] |= opposite;
					cells.add(new Held(nx,ny));
					index = -1;
					break;
				}
			}
			if(index != -1) cells.remove(index);
		}
		int numWallsToBreak = (int)(loopPercent * width * height);
		while(numWallsToBreak > 0)
		{
			int xtb = r.nextInt(width);
			int ytb = r.nextInt(height);
			int direction = r.nextInt(2) * 2 + 2;
			if((grid[ytb][xtb] & direction) == 0 && !((ytb == height - 1 && direction == 4) || (xtb == width - 1 && direction == 2)))
			{
				grid[ytb][xtb] = (byte) (grid[ytb][xtb] | direction);
				numWallsToBreak --;
			}
		}
		String ceilingW = "";
		for(int i = 0; i < (grid[0].length * 2 + 1) * pathWidth; i ++)
		{
			ceilingW = ceilingW + "W";
		}
		char[][] lines = new char[grid.length * 2][grid[0].length * 2];
		for(char[] line : lines)
		{
			Arrays.fill(line, 'W');
		}
		for(int y2 = 0; y2 < grid.length; y2 ++)
		{
			for(int x2 = 0; x2 < grid[0].length; x2 ++)
			{
				lines[y2 * 2][x2 * 2] = 'P';
				if((grid[y2][x2] & 2) != 0)
				{
					lines[y2 * 2][x2 * 2 + 1] = 'P';
				}
				if((grid[y2][x2] & 4) != 0)
				{
					lines[y2 * 2 + 1][x2 * 2] = 'P';
				}
			}
		}
		String startLine = "";
		ArrayList<String> result = new ArrayList<String>();
		for(int i = 0; i < pathWidth; i ++)
		{
			result.add(ceilingW);
			startLine = startLine + "W";
		}
		for(char[] line : lines)
		{
			String toPrint = startLine + "";
			for(char c : line)
			{
				for(int i = 0; i < pathWidth; i ++)
				{
					toPrint = toPrint + c;
				}
			}
			for(int i = 0; i < pathWidth; i ++)
			{
				result.add(toPrint);
			}
		}
		return result;
	}
}
