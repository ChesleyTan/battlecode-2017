package v1;

import battlecode.common.*;
import sun.tools.tree.SynchronizedStatement;

public class Gardener extends Globals{
	
	private static MapLocation[] otherArchonLoc;
	private static int numTreesBuilt = 0;
	private static Direction lastDir = null;
	private static float detectRadius = 3f;
	private static final int defense_start_channel = 250;
	private static final int early_scouts_channel = 5;
	
	
	/*
	 * Checks that there is enough space around the unit to begin planting
	 */
	public static void checkspace() throws GameActionException{
		while(!rc.onTheMap(here, detectRadius) || rc.isCircleOccupiedExceptByThisRobot(here, detectRadius)){ 
			Globals.update();
			float sumX = 0;
			float sumY = 0;
			
			// Opposing forces created by Robots
			RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
			for (RobotInfo r: nearbyRobots){
        Direction their_direction = here.directionTo(r.location).opposite();
        float their_distance = (RobotType.GARDENER.sensorRadius - here.distanceTo(r.location))/RobotType.GARDENER.sensorRadius * RobotType.GARDENER.strideRadius;
        sumX += their_direction.getDeltaX(their_distance);
        sumY += their_direction.getDeltaY(their_distance);
	    }
			
			// Opposing forces created by Trees
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
			for (TreeInfo t: nearbyTrees){
        Direction their_direction = here.directionTo(t.location).opposite();
        float their_distance = (RobotType.GARDENER.sensorRadius - here.distanceTo(t.location))/RobotType.GARDENER.sensorRadius * RobotType.GARDENER.strideRadius;
        sumX += their_direction.getDeltaX(their_distance);
        sumY += their_direction.getDeltaY(their_distance);
	    }
			
			// Opposing forces created by Edge of Map
			if (!rc.onTheMap(new MapLocation(here.x - 1, here.y))){
			  sumX += RobotType.GARDENER.strideRadius;
			}
			if (!rc.onTheMap(new MapLocation(here.x + 1, here.y))){
			  sumX -= RobotType.GARDENER.strideRadius;
			}
			if (!rc.onTheMap(new MapLocation(here.x, here.y - 1))){
			  sumY += RobotType.GARDENER.strideRadius;
			}
			if (!rc.onTheMap(new MapLocation(here.x, here.y + 1))){
			  sumY -= RobotType.GARDENER.strideRadius;
			}
			float finaldist = (float)Math.sqrt(Math.pow(sumX, 2) + Math.pow(sumY, 2));
			
      Direction finalDir = new Direction(sumX, sumY);
      if(rc.canMove(finalDir) && !rc.hasMoved()){
        rc.move(finalDir);
      }
      else {
        while (!rc.canMove(finalDir)){
          finalDir = finalDir.rotateLeftDegrees(20);
        }
        rc.move(finalDir);
      }
	    System.out.println("SumX: " + sumX);
	    System.out.println("SumY: "+ sumY);
	    Clock.yield();
		}
	}
	public static void loop() throws GameActionException{
	  
	  // Initial setup moves to a clear spot and spawns 3 scouts
		checkspace();
	  int scoutCount = rc.readBroadcast(early_scouts_channel);
	  while(scoutCount < 3){
	    if (rc.canBuildRobot(RobotType.SCOUT, Direction.getNorth())){
	      rc.buildRobot(RobotType.SCOUT, Direction.getNorth());
	      rc.broadcast(early_scouts_channel, scoutCount + 1);
	    }
	    else{
	      checkspace();
	    }
	    Clock.yield();
	    scoutCount = rc.readBroadcast(early_scouts_channel);
	  }
	  checkspace();
	  // Loop: Build trees and water them, and occasionally build scouts
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