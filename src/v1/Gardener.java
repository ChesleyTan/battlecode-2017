package v1;

import battlecode.common.*;

public class Gardener extends Globals{
	
	private static MapLocation[] otherArchonLoc;
	private static int numTreesBuilt = 0;
	private static Direction lastDir = null;
	private static float detectRadius = 2.5f;
	private static final int defense_start_channel = 250;
	
	public static void checkspace() throws GameActionException{
		while(!rc.onTheMap(here.translate(0.001f, 0.001f), detectRadius) || rc.isCircleOccupiedExceptByThisRobot(here.translate(0.001f, 0.001f), detectRadius)){ 
			Globals.update();
			if (lastDir != null && rc.canMove(lastDir)){
				rc.move(lastDir);
				Clock.yield();
			}
			float[] volatileDirections = new float[10];
			int index = 0;
			if (!rc.onTheMap(here.add(Direction.getSouth(), detectRadius))){
				volatileDirections[index] = Direction.getSouth().getAngleDegrees();
				index ++;
			}
			else if (!rc.onTheMap(here.add(Direction.getEast(), detectRadius))){
				volatileDirections[index] = Direction.getEast().getAngleDegrees();
				index ++;
			}
			else if (!rc.onTheMap(here.add(Direction.getNorth(), detectRadius))){
				volatileDirections[index] = Direction.getNorth().getAngleDegrees();
				index ++;
			}
			else if (!rc.onTheMap(here.add(Direction.getWest(), detectRadius))){
				volatileDirections[index] = Direction.getWest().getAngleDegrees();
				index ++;
			}
		
			RobotInfo[] nearbyRobots = rc.senseNearbyRobots(detectRadius);
			if (nearbyRobots != null && nearbyRobots.length > 0 && !rc.hasMoved()){
				for(int i = 0; i < nearbyRobots.length; i++){
					if (nearbyRobots[i] != null){
						volatileDirections[index] = here.directionTo(nearbyRobots[i].location).getAngleDegrees();
						index++;
					}
				}
			}
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees(detectRadius);
			if (nearbyTrees != null && nearbyTrees.length > 0){
				for(int i = 0; i < nearbyTrees.length; i++){
					if (nearbyTrees[i] != null){
						volatileDirections[index] = here.directionTo(nearbyTrees[i].location).getAngleDegrees();
						index++;
					}
				}
			}
			float sum = 0;
			for(int i = 0; i < index; i++){
				sum += volatileDirections[i];
			}
			sum = sum / index;
			sum = sum / 180 * (float)(Math.PI);
			Direction movedir = new Direction(sum);
			if(rc.canMove(movedir)){
				rc.move(movedir);
				lastDir = movedir;
			}
			else if (rc.canMove(movedir.opposite())){
				rc.move(movedir.opposite());
				lastDir = movedir.opposite();
			}
			else{
				float rand = (float)(Math.random() * 2 * Math.PI);
				Direction newdir = new Direction(rand);
				if (rc.canMove(newdir)){
					rc.move(newdir);
					lastDir = newdir;
				}	
			}
			Clock.yield();
		}
	}
	public static void loop() throws GameActionException{
		checkspace();
		if (rc.getRoundNum() < 100){
		  int scoutCount = 0;
		  while(scoutCount < 3){
		    if (rc.canBuildRobot(RobotType.SCOUT, Direction.getNorth())){
		      rc.buildRobot(RobotType.SCOUT, Direction.getNorth());
		      scoutCount++;
		    }
		    else{
		      checkspace();
		    }
		    Clock.yield();
		  }
		}
		while(true){
			try{
				Globals.update();
				if (numTreesBuilt < 5 && rc.canPlantTree(Direction.getNorth().rotateLeftDegrees(60 * numTreesBuilt))){
					rc.plantTree(Direction.getNorth().rotateLeftDegrees(60*numTreesBuilt));
					numTreesBuilt++;
				}
				else if (numTreesBuilt == 0){
					checkspace();
					Clock.yield();
				}
				else{
					if(rc.getRoundNum() % 100 == 0 && rc.canBuildRobot(RobotType.SCOUT, Direction.getNorth().rotateRightDegrees(60))){
						rc.buildRobot(RobotType.SCOUT, Direction.getNorth().rotateRightDegrees(60));
					}
				}
				TreeInfo[] nearbyTrees = rc.senseNearbyTrees(2, us);
				if (nearbyTrees != null && nearbyTrees.length != 0){
					TreeInfo minWaterable = nearbyTrees[0];
					for(TreeInfo x: nearbyTrees){
						if(rc.canWater(x.ID) && x.health < minWaterable.health){
							minWaterable = x;
						}
					}
					if(rc.canWater(minWaterable.ID)){
						rc.water(minWaterable.ID);
					}
				}
				Clock.yield();
			}
			catch(Exception e){
				e.printStackTrace();
			}
		}
	}
}