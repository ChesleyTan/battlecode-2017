package evasion;

import battlecode.common.*;

public strictfp class EvasiveArchon {
    static int ARCHON_BODY_RADIUS = 2;
    static int ARCHON_HP = 400;
    static int ARCHON_SIGHT_RADIUS = 10;
    static int ARCHON_BULLET_SIGHT_RADIUS = 15;
    static int ARCHON_STRIDE_RADIUS = 1;
    static Direction[] angleDirections = new Direction[12];
    static float UNKNOWN = -1f;
    static float minX = UNKNOWN;
    static float minY = UNKNOWN;
    static float maxX = UNKNOWN;
    static float maxY = UNKNOWN;

    static void run() throws GameActionException {
        System.out.println("I'm an archon!");
        for (int angle = 0; angle < 12; ++angle) {
            angleDirections[angle] = new Direction((float) (angle * Math.PI / 6));
        }
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                RobotInfo[] nearbyRobots = RobotPlayer.rc.senseNearbyRobots();
                MapLocation myLoc = RobotPlayer.rc.getLocation();
                System.out.println(myLoc);
                float[] directionWeights = new float[12];
                for (RobotInfo ri : nearbyRobots) {
                    if (ri.team == RobotPlayer.opponentTeam && ri.type.canAttack()) {
                        Direction enemyAngle = myLoc.directionTo(ri.location);
                        for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
                            float weightOffset = (50 * (180 - Math
                                    .abs(degreesBetween(enemyAngle, angleDirections[angleIndex]))));
                            directionWeights[angleIndex] -= weightOffset;
                            System.out.println("Enemy angle: " + enemyAngle.getAngleDegrees());
                            System.out.println("Weight for angle "
                                    + angleDirections[angleIndex].getAngleDegrees() + ":"
                                    + weightOffset);
                            System.out.println("Radians between: " + Math
                                    .abs(radiansBetween(enemyAngle, angleDirections[angleIndex])));
                            System.out.println("Degrees between: " + Math
                                    .abs(degreesBetween(enemyAngle, angleDirections[angleIndex])));
                        }
                    }
                }
                for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
                    // Locate edges
                    if (angleIndex % 3 == 0) {
                        // FIXME is ARCHON_SIGHT_RADIUS broken?
                        MapLocation testLocation = myLoc.add(angleDirections[angleIndex],
                                ARCHON_SIGHT_RADIUS - 1);
                        if (!RobotPlayer.rc.onTheMap(testLocation)) {
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
                System.out.println("minX: " + minX);
                System.out.println("maxX: " + maxX);
                System.out.println("minY: " + minY);
                System.out.println("maxY: " + maxY);
                // Avoid corners and edges
                if (minX != UNKNOWN && myLoc.x - minX < 10) {
                    for (int angleIndex = 4; angleIndex < 9; ++angleIndex) {
                        directionWeights[angleIndex] -= 2000 * (10 - (myLoc.x - minX));
                    }
                }
                if (minY != UNKNOWN && myLoc.y - minY < 10) {
                    for (int angleIndex = 7; angleIndex < 12; ++angleIndex) {
                        directionWeights[angleIndex] -= 2000 * (10 - (myLoc.y - minY));
                    }
                }
                if (maxX != UNKNOWN && maxX - myLoc.x < 10) {
                    for (int angleIndex = 0; angleIndex < 3; ++angleIndex) {
                        directionWeights[angleIndex] -= 2000 * (10 - (maxX - myLoc.x));
                    }
                    for (int angleIndex = 10; angleIndex < 12; ++angleIndex) {
                        directionWeights[angleIndex] -= 2000 * (10 - (maxX - myLoc.x));
                    }
                }
                if (maxY != UNKNOWN && maxY - myLoc.y < 10) {
                    for (int angleIndex = 1; angleIndex < 6; ++angleIndex) {
                        directionWeights[angleIndex] -= 2000 * (10 - (maxY - myLoc.y));
                    }
                }
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
                    System.out.println("Round: " + RobotPlayer.rc.getRoundNum());
                    System.out.println(
                            "Trying to move in direction: " + angleDirections[moveAngleIndex]);
                    moved = RobotPlayer.tryMove(angleDirections[moveAngleIndex], 5, 3);
                    RobotPlayer.rc.setIndicatorLine(myLoc,
                            myLoc.add(angleDirections[moveAngleIndex], 1), 0, 255, 0);
                    RobotPlayer.rc.setIndicatorLine(myLoc.add(angleDirections[moveAngleIndex], 2),
                            myLoc.add(angleDirections[moveAngleIndex], 1.5f), 255, 0, 0);
                    RobotPlayer.rc.setIndicatorDot(myLoc,
                            0,
                            0,
                            0);
                    for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
                        RobotPlayer.rc.setIndicatorDot(myLoc.add(angleDirections[angleIndex]),
                        (int)Math.max(-25500, directionWeights[angleIndex]) / (-100),
                        (int)Math.max(-25500, directionWeights[angleIndex]) / (-100),
                        (int)Math.max(-25500, directionWeights[angleIndex]) / (-100));
                    }
                    directionWeights[moveAngleIndex] -= 999999;
                } while (!moved && ++attempts <= 12);
                if (attempts > 12) {
                    RobotPlayer.tryMove(Direction.getNorth(), 10, 18);
                }
                // Broadcast archon's location for other robots on the team to know
                RobotPlayer.rc.broadcast(0, (int) myLoc.x);
                RobotPlayer.rc.broadcast(1, (int) myLoc.y);

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }

        }
    }

    public static float radiansBetween(Direction a, Direction b) {
        return reduce(b.radians - a.radians);
    }

    public static float degreesBetween(Direction a, Direction b) {
        return (float) Math.toDegrees(radiansBetween(a, b));
    }

    // Internally used to keep angles in the range (-Math.PI,Math.PI]
    private static float reduce(float rads) {
        if (rads <= -Math.PI) {
            int circles = (int) Math.ceil(-(rads + Math.PI) / (2 * Math.PI));
            return rads + (float) (Math.PI * 2 * circles);
        }
        else if (rads > Math.PI) {
            int circles = (int) Math.ceil((rads - Math.PI) / (2 * Math.PI));
            return rads - (float) (Math.PI * 2 * circles);
        }
        return rads;
    }
}
