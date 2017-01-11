package evasion;

import battlecode.common.*;

public strictfp class EvasiveArchon extends Globals {
  // Change to RobotType enums
  static Direction[] angleDirections = new Direction[12];
  static final int[] cardinalAngleIndices = new int[] { 0, 3, 6, 9 };
  static float UNKNOWN = -1f;
  static float minX = UNKNOWN;
  static float minY = UNKNOWN;
  static float maxX = UNKNOWN;
  static float maxY = UNKNOWN;
  static final int EDGE_BIAS_RADIUS = 15;
  static int lastMoveAngleIndex = -1;

  static void run() throws GameActionException {
    for (int angle = 0; angle < 12; ++angle) {
      angleDirections[angle] = new Direction((float) (angle * Math.PI / 6));
    }
    // The code you want your robot to perform every round should be in this loop
    while (true) {

      // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
      try {
        Globals.update();
        System.out.println("========== Round: " + rc.getRoundNum() + "==========");
        RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
        System.out.println(here);
        float[] directionWeights = new float[12];
        for (RobotInfo ri : nearbyRobots) {
          if (ri.team == them && ri.type.canAttack()) {
            Direction enemyAngle = here.directionTo(ri.location);
            System.out.println("Enemy angle: " + enemyAngle.getAngleDegrees());
            for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
              // TODO avoid unnecessary angles
              float weightOffset = (100 * Math.max(0,
                  (70 - Math.abs(degreesBetween(enemyAngle, angleDirections[angleIndex])))));
              directionWeights[angleIndex] -= weightOffset;
              System.out.println("Weight for angle " + angleDirections[angleIndex].getAngleDegrees()
                  + ":" + weightOffset);
              System.out.println("Degrees between: "
                  + Math.abs(degreesBetween(enemyAngle, angleDirections[angleIndex])));
            }
          }
        }
        TreeInfo[] nearbyTrees = rc.senseNearbyTrees(5);
        for (TreeInfo ti : nearbyTrees) {
          Direction treeAngle = here.directionTo(ti.location);
          System.out.println("Tree angle: " + treeAngle.getAngleDegrees());
          for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
            // TODO avoid unnecessary angles
            float weightOffset = (30 * Math.max(0,
                (60 - Math.abs(degreesBetween(treeAngle, angleDirections[angleIndex])))));
            directionWeights[angleIndex] -= weightOffset;
            System.out.println("Weight for angle " + angleDirections[angleIndex].getAngleDegrees()
                + ":" + weightOffset);
            System.out.println("Degrees between: "
                + Math.abs(degreesBetween(treeAngle, angleDirections[angleIndex])));
          }
        }
        BulletInfo[] nearbyBullets = rc.senseNearbyBullets(7);
        for (int i = 0; i < nearbyBullets.length && i < 5; ++i) {
          BulletInfo bi = nearbyBullets[i];
          if (RobotPlayer.willCollideWithMe(bi)) {
            Direction bulletAngle = bi.location.directionTo(here);
            System.out.println("Bullet angle: " + bulletAngle.getAngleDegrees());
            int collisionAngleIndex = (int)bulletAngle.getAngleDegrees() / 30;
            for (int angleIndexOffset = -1; angleIndexOffset <= 1; ++angleIndexOffset) {
              int angleIndex = pmod(collisionAngleIndex + angleIndexOffset, 12);
              // Avoid moving along axis of bullet trajectory
              float weightOffset = (150 * (60 - 30 * Math.abs(angleIndexOffset)));
              directionWeights[angleIndex] -= weightOffset;
              System.out.println("Weight for angle " + angleDirections[angleIndex].getAngleDegrees()
                  + ":" + weightOffset);
              System.out.println("Degrees between: "
                  + Math.abs(degreesBetween(bulletAngle, angleDirections[angleIndex])));
              // Avoid moving towards bullet
              angleIndex = pmod(angleIndex + 6, 12);
              weightOffset = (200 * (60 - 30 * Math.abs(angleIndexOffset)));
              directionWeights[angleIndex] -= weightOffset;
              System.out.println("Weight for angle " + angleDirections[angleIndex].getAngleDegrees()
                  + ":" + weightOffset);
              System.out.println("Degrees between: "
                  + Math.abs(degreesBetween(bulletAngle, angleDirections[angleIndex])));
            }
          }
        }
        for (int angleIndex : cardinalAngleIndices) {
          // FIXME is ARCHON_SIGHT_RADIUS broken?
          MapLocation testLocation = here.add(angleDirections[angleIndex],
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
        /*
        System.out.println("minX: " + minX);
        System.out.println("maxX: " + maxX);
        System.out.println("minY: " + minY);
        System.out.println("maxY: " + maxY);
        */
        // Avoid corners and edges
        if (minX != UNKNOWN && here.x - minX < EDGE_BIAS_RADIUS) {
          for (int angleIndex = 4; angleIndex < 9; ++angleIndex) {
            directionWeights[angleIndex] -= 2000 * (EDGE_BIAS_RADIUS - (here.x - minX));
          }
        }
        if (minY != UNKNOWN && here.y - minY < EDGE_BIAS_RADIUS) {
          for (int angleIndex = 7; angleIndex < 12; ++angleIndex) {
            directionWeights[angleIndex] -= 2000 * (EDGE_BIAS_RADIUS - (here.y - minY));
          }
        }
        if (maxX != UNKNOWN && maxX - here.x < EDGE_BIAS_RADIUS) {
          for (int angleIndex = 0; angleIndex < 3; ++angleIndex) {
            directionWeights[angleIndex] -= 2000 * (EDGE_BIAS_RADIUS - (maxX - here.x));
          }
          for (int angleIndex = 10; angleIndex < 12; ++angleIndex) {
            directionWeights[angleIndex] -= 2000 * (EDGE_BIAS_RADIUS - (maxX - here.x));
          }
        }
        if (maxY != UNKNOWN && maxY - here.y < EDGE_BIAS_RADIUS) {
          for (int angleIndex = 1; angleIndex < 6; ++angleIndex) {
            directionWeights[angleIndex] -= 2000 * (EDGE_BIAS_RADIUS - (maxY - here.y));
          }
        }
        for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
          System.out
              .println("Angle: " + (angleIndex * 30) + ", Weight: " + directionWeights[angleIndex]);
        }
        // TODO avoid corners using messaging
        //rc.setIndicatorDot(arg0, arg1, arg2, arg3);
        /*
        // Generate a random direction
        Direction dir = RobotPlayer.randomDirection();
        
        // Randomly attempt to build a gardener in this direction
        
        if (rc.canHireGardener(dir)) {
            rc.hireGardener(dir);
        }
        */

        // Increase preference for last direction moved,
        // helps prevent getting trapped in an oscillation
        if (lastMoveAngleIndex >= 0) {
          directionWeights[lastMoveAngleIndex] += 5000;
        }

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
          System.out.println("Trying to move in direction: " + angleDirections[moveAngleIndex]);
          moved = RobotPlayer.tryMove(angleDirections[moveAngleIndex], 5, 3);
          rc.setIndicatorLine(here, here.add(angleDirections[moveAngleIndex], 1), 0, 255, 0);
          rc.setIndicatorLine(here.add(angleDirections[moveAngleIndex], 1),
              here.add(angleDirections[moveAngleIndex], 1.5f), 255, 0, 0);
          rc.setIndicatorDot(here, 0, 0, 0);
          for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
            rc.setIndicatorDot(here.add(angleDirections[angleIndex]),
                (int) Math.max(-25500, directionWeights[angleIndex]) / (-100),
                (int) Math.max(-25500, directionWeights[angleIndex]) / (-100),
                (int) Math.max(-25500, directionWeights[angleIndex]) / (-100));
          }
          directionWeights[moveAngleIndex] -= 999999;
        } while (!moved && ++attempts <= 12);
        if (attempts > 12) {
          RobotPlayer.tryMove(Direction.getNorth(), 10, 18);
          lastMoveAngleIndex = -1;
        }
        else {
          lastMoveAngleIndex = moveAngleIndex;
        }
        // Broadcast archon's location for other robots on the team to know
        rc.broadcast(0, (int) here.x);
        rc.broadcast(1, (int) here.y);
        System.out.println("Bytecodes left: " + Clock.getBytecodesLeft());
        // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
        Clock.yield();

      } catch (Exception e) {
        System.out.println("Archon Exception");
        e.printStackTrace();
      }

    }
  }
  
  private static int pmod(int n, int modulo) {
    int m = n % modulo;
    if (m < 0) {
      return m + modulo;
    }
    return m;
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
