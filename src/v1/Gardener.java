package v1;

import battlecode.common.*;

public class Gardener extends Globals{
	
	private static MapLocation[] otherArchonLoc = rc.getInitialArchonLocations(them);
	private static int numTreesBuilt = 0;
	
	public static void loop() throws GameActionException{
		try{
			System.out.println(rc.onTheMap(here, 2));
		}
		catch(GameActionException e){
			e.printStackTrace();
		}
		while(!rc.onTheMap(here, 2)){
			Globals.update();
			if (!rc.onTheMap(here.add(Direction.getSouth(), 2))){
				if(rc.canMove(Direction.getNorth())){
					rc.move(Direction.getNorth());
				}
			}
			else if (!rc.onTheMap(here.add(Direction.getEast(), 2))){
				if(rc.canMove(Direction.getWest())){
					rc.move(Direction.getWest());
				}
			}
			else if (!rc.onTheMap(here.add(Direction.getNorth(), 2))){
				if(rc.canMove(Direction.getSouth())){
					rc.move(Direction.getSouth());
				}
			}
			else if (!rc.onTheMap(here.add(Direction.getWest(), 2))){
				if(rc.canMove(Direction.getEast())){
					rc.move(Direction.getEast());
				}
			}
		}
		while(rc.isCircleOccupiedExceptByThisRobot(here, 2) && !rc.hasMoved()){
			Globals.update();
			System.out.println("checking circle");
			RobotInfo[] nearbyRobots = rc.senseNearbyRobots(1);
			if (nearbyRobots[0] != null){
				RobotInfo first = nearbyRobots[0];
				Direction movedir = here.directionTo(first.location).opposite();
				if (rc.canMove(movedir)){
					rc.move(movedir);
				}
			}
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees(1);
			if (nearbyTrees[0] != null){
				TreeInfo first = nearbyTrees[0];
				Direction movedir = here.directionTo(first.location).opposite();
				if (rc.canMove(movedir)){
					rc.move(movedir);
				}
			}
		}
		while(true){
			Globals.update();
			if (numTreesBuilt < 5 && rc.canPlantTree(Direction.getNorth().rotateLeftDegrees(60 * numTreesBuilt))){
				rc.plantTree(Direction.getNorth().rotateLeftDegrees(60*numTreesBuilt));
				numTreesBuilt++;
			}
			else{
				if(rc.canBuildRobot(RobotType.SCOUT, Direction.getNorth().rotateRightDegrees(60))){
					rc.buildRobot(RobotType.SCOUT, Direction.getNorth().rotateRightDegrees(60));
				}
			}
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees(1, us);
			TreeInfo minWaterable = nearbyTrees[0];
			for(TreeInfo x: nearbyTrees){
				if(rc.canWater(x.ID) && x.health < minWaterable.health){
					minWaterable = x;
				}
			}
			if(rc.canWater(minWaterable.ID)){
				rc.water(minWaterable.ID);
			}
			Clock.yield();
		}
	}
}