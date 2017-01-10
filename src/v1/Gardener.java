package v1;

import battlecode.common.*;

public class Gardener extends Globals{
	
	private static MapLocation[] otherArchonLoc;
	private static int numTreesBuilt = 0;
	
	public static void loop() throws GameActionException{
		while(!rc.onTheMap(here.translate(0.001f, 0.001f), 2f)){
			Globals.update();
			if (!rc.onTheMap(here.add(Direction.getSouth(), 2f))){
				if(rc.canMove(Direction.getNorth())){
					rc.move(Direction.getNorth());
				}
			}
			else if (!rc.onTheMap(here.add(Direction.getEast(), 2f))){
				if(rc.canMove(Direction.getWest())){
					rc.move(Direction.getWest());
				}
			}
			else if (!rc.onTheMap(here.add(Direction.getNorth(), 2f))){
				if(rc.canMove(Direction.getSouth())){
					rc.move(Direction.getSouth());
				}
			}
			else if (!rc.onTheMap(here.add(Direction.getWest(), 2f))){
				if(rc.canMove(Direction.getEast())){
					rc.move(Direction.getEast());
				}
			}
		}
		while(rc.isCircleOccupiedExceptByThisRobot(here.translate(0.001f, 0.001f), 3) && !rc.hasMoved()){
			Globals.update();
			RobotInfo[] nearbyRobots = rc.senseNearbyRobots(3);
			if (nearbyRobots != null && nearbyRobots.length > 0){
				System.out.println("true"); 
				RobotInfo first = nearbyRobots[0];
				Direction movedir = here.directionTo(first.location).opposite();
				if (rc.canMove(movedir)){
					rc.move(movedir);
				}
			}
			TreeInfo[] nearbyTrees = rc.senseNearbyTrees(2);
			if (nearbyTrees != null && nearbyTrees.length > 0){
				TreeInfo first = nearbyTrees[0];
				Direction movedir = here.directionTo(first.location).opposite();
				if (rc.canMove(movedir)){
					rc.move(movedir);
				}
			}
		}
		while(true){
			try{
				Globals.update();
				if (numTreesBuilt < 6 && rc.canPlantTree(Direction.getNorth().rotateLeftDegrees(60 * numTreesBuilt))){
					rc.plantTree(Direction.getNorth().rotateLeftDegrees(60*numTreesBuilt));
					numTreesBuilt++;
				}
				/*else{
					if(rc.canBuildRobot(RobotType.SCOUT, Direction.getNorth().rotateRightDegrees(60))){
						rc.buildRobot(RobotType.SCOUT, Direction.getNorth().rotateRightDegrees(60));
					}
				}*/
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