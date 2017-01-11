package v1;

import battlecode.common.*;
import evasion.RobotPlayer;

public class Archon extends Globals{
	
  static int producedGardeners = 0;
	static Direction[] angleDirections = new Direction[12];
    static float UNKNOWN = -1f;
    static float minX = UNKNOWN;
    static float minY = UNKNOWN;
    static float maxX = UNKNOWN;
    static float maxY = UNKNOWN;
	
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide)
            throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while (currentCheck <= checksPerSide) {
            // Try the offset of the left side
            if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
                return true;
            }
            // Try the offset on the right side
            if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }
    
	public static void moveDirection() throws GameActionException {
        for (int angle = 0; angle < 12; ++angle) {
            angleDirections[angle] = new Direction((float) (angle * Math.PI / 6));
        }
        // The code you want your robot to perform every round should be in this loop

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
        try {
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
            MapLocation myLoc = rc.getLocation();
            System.out.println(myLoc);
            float[] directionWeights = new float[12];
            for (RobotInfo ri : nearbyRobots) {
                if (ri.team == them && ri.type != RobotType.GARDENER) {
                    Direction enemyAngle = myLoc.directionTo(ri.location);
                    for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
                        float weightOffset = (50 * (180 - Math
                                .abs(enemyAngle.degreesBetween(angleDirections[angleIndex]))));
                        directionWeights[angleIndex] -= weightOffset;
                        System.out.println("Enemy angle: " + enemyAngle.getAngleDegrees());
                        System.out.println("Weight for angle "
                                + angleDirections[angleIndex].getAngleDegrees() + ":"
                                + weightOffset);
                        System.out.println("Degrees between: "
                                + Math.abs(enemyAngle.degreesBetween(angleDirections[angleIndex])));
                    }
                }
            }
            for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
                // Locate edges
                if (angleIndex % 3 == 0) {
                    // FIXME is ARCHON_SIGHT_RADIUS broken?
                    MapLocation testLocation = myLoc.add(angleDirections[angleIndex],
                            RobotType.ARCHON.sensorRadius - 1);
                    if (!rc.onTheMap(testLocation)) {
                        /*
                        System.out.println(testLocation.x);
                        System.out.println(myLoc.x);
                        System.out.println(testLocation.y);
                        System.out.println(myLoc.y);
                        System.out.println(angleDirections[angleIndex]);
                        */
                        switch (angleIndex) {
                            case 0:
                                if (maxX == UNKNOWN) {
                                    maxX = testLocation.x;
                                }
                                else {
                                    maxX = Math.min(maxX, testLocation.x);
                                }
                                break;
                            case 3:
                                if (maxY == UNKNOWN) {
                                    maxY = testLocation.y;
                                }
                                else {
                                    maxY = Math.min(maxY, testLocation.y);
                                }
                                break;
                            case 6:
                                if (minX == UNKNOWN) {
                                    minX = testLocation.x;
                                }
                                else {
                                    minX = Math.max(minX, testLocation.x);
                                }
                                break;
                            case 9:
                                if (minY == UNKNOWN) {
                                    minY = testLocation.y;
                                }
                                else {
                                    minY = Math.max(minY, testLocation.y);
                                }
                                break;
                        }
                    }
                }
            }
            /*
            System.out.println("minX: " + minX);
            System.out.println("maxX: " + maxX);
            System.out.println("minY: " + minY);
            System.out.println("maxY: " + maxY);
            */
            // Avoid corners and edges
            /*
            if (minX != UNKNOWN && myLoc.x - minX < 5) {
                for (int angleIndex = 4; angleIndex < 9; ++angleIndex) {
                    directionWeights[angleIndex] -= 10000;
                }
            }
            if (minY != UNKNOWN && myLoc.y - minY < 5) {
                for (int angleIndex = 7; angleIndex < 12; ++angleIndex) {
                    directionWeights[angleIndex] -= 10000;
                }
            }
            if (maxX != UNKNOWN && maxX - myLoc.x < 5) {
                for (int angleIndex = 0; angleIndex < 3; ++angleIndex) {
                    directionWeights[angleIndex] -= 10000;
                }
                for (int angleIndex = 10; angleIndex < 12; ++angleIndex) {
                    directionWeights[angleIndex] -= 10000;
                }
            }
            if (maxY != UNKNOWN && maxY - myLoc.y < 5) {
                for (int angleIndex = 1; angleIndex < 6; ++angleIndex) {
                    directionWeights[angleIndex] -= 10000;
                }
            }
            */
            for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
                System.out.println("Angle: " + (angleIndex * 30) + ", Weight: "
                        + directionWeights[angleIndex]);
            }
            // TODO avoid corners using messaging
            //RobotPlayer.rc.setIndicatorDot(arg0, arg1, arg2, arg3);
            /*
            // Generate a random direction
            Direction dir = RobotPlayer.randomDirection();
            
            // Randomly attempt to build a gardener in this direction
            
            if (RobotPlayer.rc.canHireGardener(dir)) {
                RobotPlayer.rc.hireGardener(dir);
            }
            */

            int moveAngleIndex = 0;
            int attempts = 0;
            boolean moved = false;
            do {
                moveAngleIndex = 0;
                for (int angleIndex = 1; angleIndex < 12; ++angleIndex) {
                    if (directionWeights[angleIndex] > directionWeights[moveAngleIndex]) {
                        moveAngleIndex = angleIndex;
                    }
                }
                directionWeights[moveAngleIndex] -= 999999;
                System.out.println(
                        "Trying to move in direction: " + angleDirections[moveAngleIndex]);
                moved = tryMove(angleDirections[moveAngleIndex], 5, 3);
            } while (!moved && ++attempts <= 3);

            // Broadcast archon's location for other robots on the team to know
            rc.broadcast(0, (int) myLoc.x);
            rc.broadcast(1, (int) myLoc.y);

            // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
            Clock.yield();

        } catch (Exception e) {
            System.out.println("Archon Exception");
            e.printStackTrace();
        }
 
	}
	public static boolean enemyNearby() throws GameActionException {
		RobotInfo[] robots = rc.senseNearbyRobots();
		if (robots != null){
			for(RobotInfo x: robots){
				if(x.getTeam() == them){
					return true;
				}
			}
		}
		return false;
	}
	public static void loop() throws GameActionException{
		
		while(true){
			if(rc.canHireGardener(Direction.getNorth()) && producedGardeners < 5){
				rc.hireGardener(Direction.getNorth());
				producedGardeners ++;
			}
			else{
				if (producedGardeners >= 5 || enemyNearby()){
				  moveDirection();
				}
				else{
				  float randomRads = (float)(Math.random() * 2 * Math.PI);
				  Direction randomDir = new Direction(randomRads);
				  if(rc.canMove(randomDir)){
				    rc.move(randomDir);
				  }
				  else if (rc.canMove(randomDir.opposite())){
				    rc.move(randomDir.opposite());
				  }
				}
			}
			if (rc.getTeamBullets() > 200){
				int donationAmount = (int)((int)(rc.getTeamBullets() / 10) * 10 - 150);
				rc.donate((float)donationAmount);
			}
			Clock.yield();
		}
	}
}