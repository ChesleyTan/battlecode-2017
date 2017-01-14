package finalVersion;

import battlecode.common.*;
import utils.RobotUtils;
import utils.Globals;

public class EvasiveScout extends Globals {
  // Change to RobotType enums
  static Direction[] angleDirections = new Direction[12];
  static final int EDGE_BIAS_RADIUS = 10;
  static final int BULLET_DETECT_RADIUS = 10;
  static final int ENEMY_DETECT_RADIUS = 6;
  static final float EVASION_STRIDE_RADIUS = 1.5f;
  private static MapLocation[] moveLocations = new MapLocation[12];

  static void init() {
    for (int angle = 0; angle < 12; ++angle) {
      angleDirections[angle] = new Direction((float) (angle * Math.PI / 6));
    }
  }

  static void move(BulletInfo[] nearbyBullets, RobotInfo[] nearbyRobots) {
    // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
    try {
      Globals.update();
      /*
      if (DEBUG) {
        System.out.println("========== Round: " + rc.getRoundNum() + "==========");
        System.out.println(here);
      }
      */
      for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
        moveLocations[angleIndex] = here.add(angleDirections[angleIndex], EVASION_STRIDE_RADIUS);
      }
      float[] directionWeights = new float[12];

      boolean unsafeFromUnit = false;
      for (RobotInfo ri : nearbyRobots) {
        if (ri.type.canAttack()) {
          unsafeFromUnit = true;
          Direction enemyAngle = here.directionTo(ri.location);
          /*
          if (DEBUG) {
            System.out.println("Enemy angle: " + enemyAngle.getAngleDegrees());
          }
          */
          for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
            // TODO add weight for other units or optimize this loop
            float angleDelta = Math.abs(enemyAngle.degreesBetween(angleDirections[angleIndex]));
            if (angleDelta > 70) {
              continue;
            }
            float distBetween = moveLocations[angleIndex].distanceTo(here);
            if (ri.type == RobotType.LUMBERJACK) {
              float weightOffset = (150 * (70 - angleDelta)) + 1000 * (EVASION_STRIDE_RADIUS + ENEMY_DETECT_RADIUS - distBetween);
              directionWeights[angleIndex] -= weightOffset;
            }
            else if (ri.type == RobotType.SOLDIER) {
              float weightOffset = (150 * (70 - angleDelta)) + 1000 * (EVASION_STRIDE_RADIUS + ENEMY_DETECT_RADIUS - distBetween);
              directionWeights[angleIndex] -= weightOffset;
            }
            /*
            else if (ri.type == RobotType.SCOUT) {
              float weightOffset = (50 * (70 - angleDelta)) + 1000 * (EVASION_STRIDE_RADIUS + ENEMY_DETECT_RADIUS - distBetween);
              directionWeights[angleIndex] -= weightOffset;
            }
            */
            /*
            if (DEBUG) {
              System.out.println("Weight for angle "
                  + angleDirections[angleIndex].getAngleDegrees() + ":" + weightOffset);
              System.out.println("Degrees between: " + angleDelta);
            }
            */
          }
        }
      }
      // TODO don't move if it guarantees you will be hit by a bullet?
      boolean unsafeFromBullet = false;
      int numBulletsAnalyzed = 0;
      for (int i = 0; i < nearbyBullets.length && numBulletsAnalyzed < 5; ++i) {
        BulletInfo bi = nearbyBullets[i];
        if (!unsafeFromBullet && RobotUtils.willCollideWithMe(bi)) {
          unsafeFromBullet = true;
        }
        // Get relevant bullet information
        Direction propagationDirection = bi.dir;
        MapLocation bulletLocation = bi.location;
        /*
        if (DEBUG) {
          System.out.println("Bullet direction: " + propagationDirection);
        }
        */
        boolean couldCollide = false;
        for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
          // Calculate bullet relations to this robot
          Direction directionToRobot = bulletLocation.directionTo(moveLocations[angleIndex]);
          float theta = propagationDirection.radiansBetween(directionToRobot);
          // Make sure we don't collide into our own bullets!
          float distToRobot = bulletLocation.distanceTo(moveLocations[angleIndex]);
          // If theta > 90 degrees, then the bullet is traveling away from us and we can continue
          if (Math.abs(theta) > Math.PI / 2 && distToRobot > myType.bodyRadius) {
            continue;
          }
          couldCollide = true;
          // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
          // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
          // This corresponds to the smallest radius circle centered at our location that would intersect with the
          // line that is the path of the bullet.
          float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta));
          boolean willCollide = (perpendicularDist <= myType.bodyRadius);
          if (willCollide) {
            directionWeights[angleIndex] -= (15000
                + 1000 * (EVASION_STRIDE_RADIUS + BULLET_DETECT_RADIUS - distToRobot));
            /*
            if (DEBUG) {
              System.out.println("Angle " + (angleIndex * 30) + " is unsafe.");
            }
            */
          }
        }
        if (couldCollide) {
          ++numBulletsAnalyzed;
        }
      }
      updateMapBoundaries();
      /*
      if (DEBUG) {
        System.out.println("minX: " + minX);
        System.out.println("maxX: " + maxX);
        System.out.println("minY: " + minY);
        System.out.println("maxY: " + maxY);
      }
      */
      // Avoid corners and edges
      if (minX != UNKNOWN && here.x - minX < EDGE_BIAS_RADIUS) {
        float weightOffset = 1000 * (EDGE_BIAS_RADIUS - (here.x - minX));
        for (int angleIndex = 4; angleIndex < 9; ++angleIndex) {
          directionWeights[angleIndex] -= weightOffset;
        }
      }
      if (minY != UNKNOWN && here.y - minY < EDGE_BIAS_RADIUS) {
        float weightOffset = 1000 * (EDGE_BIAS_RADIUS - (here.y - minY));
        for (int angleIndex = 7; angleIndex < 12; ++angleIndex) {
          directionWeights[angleIndex] -= weightOffset;
        }
      }
      if (maxX != UNKNOWN && maxX - here.x < EDGE_BIAS_RADIUS) {
        float weightOffset = 1000 * (EDGE_BIAS_RADIUS - (maxX - here.x));
        for (int angleIndex = 0; angleIndex < 3; ++angleIndex) {
          directionWeights[angleIndex] -= weightOffset;
        }
        for (int angleIndex = 10; angleIndex < 12; ++angleIndex) {
          directionWeights[angleIndex] -= weightOffset;
        }
      }
      if (maxY != UNKNOWN && maxY - here.y < EDGE_BIAS_RADIUS) {
        float weightOffset = 1000 * (EDGE_BIAS_RADIUS - (maxY - here.y));
        for (int angleIndex = 1; angleIndex < 6; ++angleIndex) {
          directionWeights[angleIndex] -= weightOffset;
        }
      }
      /*
      if (DEBUG) {
        for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
          System.out
              .println("Angle: " + (angleIndex * 30) + ", Weight: " + directionWeights[angleIndex]);
        }
      }
      */
      // TODO avoid corners using messaging
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

      if (unsafeFromUnit || unsafeFromBullet) {
        int moveAngleIndex = 0;
        int attempts = 0;
        boolean moved = false;

        int movementBiasSeed = Math.abs(rand.nextInt());
        do {
          // Prevent clockwise bias in movement angle starting from 0 degrees
          moveAngleIndex = movementBiasSeed % 12;
          movementBiasSeed /= 12;
          for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
            if (angleIndex == moveAngleIndex) {
              continue;
            }
            if (directionWeights[angleIndex] > directionWeights[moveAngleIndex]) {
              moveAngleIndex = angleIndex;
            }
          }
          /*
          if (DEBUG) {
            System.out.println("Trying to move in direction: " + angleDirections[moveAngleIndex]);
          }
          */
          moved = RobotUtils.tryMoveDist(angleDirections[moveAngleIndex], EVASION_STRIDE_RADIUS, 5, 3);
          /*
          if (DEBUG) {
            rc.setIndicatorLine(here, here.add(angleDirections[moveAngleIndex], 1), 0, 255, 0);
            rc.setIndicatorLine(here.add(angleDirections[moveAngleIndex], 1),
                here.add(angleDirections[moveAngleIndex], 1.5f), 255, 0, 0);
            rc.setIndicatorDot(here, 0, 0, 0);
            for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
              rc.setIndicatorDot(here.add(angleDirections[angleIndex], 2),
                  (int) Math.max(-25500, directionWeights[angleIndex]) / (-100),
                  (int) Math.max(-25500, directionWeights[angleIndex]) / (-100),
                  (int) Math.max(-25500, directionWeights[angleIndex]) / (-100));
            }
          }
          */
          directionWeights[moveAngleIndex] -= 999999;
        } while (!moved && ++attempts <= 6);
      }
      /*
      if (DEBUG) {
        System.out.println("Bytecodes left: " + Clock.getBytecodesLeft());
      }
      */
      //System.out.println("Bytecodes left: " + Clock.getBytecodesLeft());

    } catch (Exception e) {
      System.out.println("Scout Exception");
      e.printStackTrace();
    }
  }
}
