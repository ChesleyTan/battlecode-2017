package finalVersion;

import battlecode.common.*;
import utils.RobotUtils;
import utils.Globals;

public class EvasiveGardener extends Globals {
  // Change to RobotType enums
  static Direction[] angleDirections = new Direction[12];
  static final int EDGE_BIAS_RADIUS = 5;
  static final int BULLET_DETECT_RADIUS = 6;
  static final int ENEMY_DETECT_RADIUS = 6;
  static final int TREE_DETECT_RADIUS = 5;
  static final float EVASION_STRIDE_RADIUS = RobotType.GARDENER.strideRadius;
  private static MapLocation[] moveLocations = new MapLocation[12];

  static void init() {
    for (int angle = 0; angle < 12; ++angle) {
      angleDirections[angle] = new Direction((float) (angle * Math.PI / 6));
    }
  }

  static boolean move(BulletInfo[] nearbyBullets, RobotInfo[] nearbyRobots, TreeInfo[] nearbyTrees) {
    // Assumes the gardener will be hit by one of the nearby bullets if it does not move
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

      for (RobotInfo ri : nearbyRobots) {
        if (ri.getType().canAttack()) {
          Direction enemyAngle = here.directionTo(ri.getLocation());
          /*
          if (DEBUG) {
            System.out.println("Enemy angle: " + enemyAngle.getAngleDegrees());
          }
          */
          for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
            float angleDelta = Math.abs(enemyAngle.degreesBetween(angleDirections[angleIndex]));
            if (angleDelta > 70) {
              continue;
            }
            float distBetween = moveLocations[angleIndex].distanceTo(ri.location);
            float weightOffset;
            switch (ri.getType()) {
              case LUMBERJACK:
                weightOffset = (150 * (70 - angleDelta))
                    + 1000 * (EVASION_STRIDE_RADIUS + ENEMY_DETECT_RADIUS - distBetween);
                directionWeights[angleIndex] -= weightOffset;
                break;
              case SOLDIER:
                weightOffset = (200 * (70 - angleDelta))
                    + 1000 * (EVASION_STRIDE_RADIUS + ENEMY_DETECT_RADIUS - distBetween);
                directionWeights[angleIndex] -= weightOffset;
                break;
              case SCOUT:
                weightOffset = (150 * (70 - angleDelta))
                    + 1000 * (EVASION_STRIDE_RADIUS + ENEMY_DETECT_RADIUS - distBetween);
                directionWeights[angleIndex] -= weightOffset;
                break;
              case TANK:
                weightOffset = (200 * (70 - angleDelta))
                    + 1000 * (EVASION_STRIDE_RADIUS + ENEMY_DETECT_RADIUS - distBetween);
                directionWeights[angleIndex] -= weightOffset;
                break;
              default:
                break;
            }
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
      for (int i = 0; i < nearbyBullets.length; ++i) {
        if (Clock.getBytecodesLeft() < 2000) {
          break;
        }
        //int startBytecodes = Clock.getBytecodeNum();
        BulletInfo bi = nearbyBullets[i];
        // Get relevant bullet information
        Direction propagationDirection = bi.getDir();
        MapLocation bulletLocation = bi.getLocation();
        /*
        if (DEBUG) {
          System.out.println("Bullet direction: " + propagationDirection);
        }
        */
        for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
          // Calculate bullet relations to this robot
          boolean willCollide = false;
          float distToRobot = bulletLocation.distanceTo(moveLocations[angleIndex]);
          if (distToRobot < myType.bodyRadius) {
            willCollide = true;
          }
          else {
            Direction directionToRobot = bulletLocation.directionTo(moveLocations[angleIndex]);
            float theta = propagationDirection.radiansBetween(directionToRobot);
            // Make sure we don't collide into our own bullets!
            // If theta > 90 degrees, then the bullet is traveling away from us and we can continue
            if (Math.abs(theta) > Math.PI / 2) {
              continue;
            }
            // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
            // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
            // This corresponds to the smallest radius circle centered at our location that would intersect with the
            // line that is the path of the bullet.
            float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta));
            willCollide = (perpendicularDist <= myType.bodyRadius);
          }
          if (willCollide) {
            directionWeights[angleIndex] -= (10000
                + 1000 * (EVASION_STRIDE_RADIUS + BULLET_DETECT_RADIUS - distToRobot)
                + 1000 * (bi.damage * bi.damage));
            /*
            if (DEBUG) {
              System.out.println("Angle " + (angleIndex * 30) + " is unsafe.");
            }
            */
          }
        }
        //System.out.println("Used: " + (Clock.getBytecodeNum() - startBytecodes));
      }
      for (TreeInfo ti : nearbyTrees) {
        Direction treeAngle = here.directionTo(ti.getLocation());
        /*
        if (DEBUG) {
          System.out.println("Tree angle: " + treeAngle.getAngleDegrees());
        }
        */
        int nearestAngle = (int) treeAngle.getAngleDegrees() / 30;
        float treeDistance = here.distanceTo(ti.getLocation()) - myType.bodyRadius - ti.getRadius();
        for (int angleIndexOffset = 0; angleIndexOffset < 3; ++angleIndexOffset) {
          float weightOffset = (70 * (60 - angleIndexOffset * 30)) + Math.max(0, 500 * (TREE_DETECT_RADIUS - treeDistance));
          //int startBytecodes = Clock.getBytecodeNum();
          System.out.println("Angle: " + (Math.floorMod(nearestAngle + angleIndexOffset, 12) * 30));
          System.out.println("Angle: " + (Math.floorMod(nearestAngle - angleIndexOffset, 12) * 30));
          System.out.println("Weight: " + weightOffset);
          directionWeights[Math.floorMod(nearestAngle + angleIndexOffset, 12)] -= weightOffset;
          if (angleIndexOffset != 0) {
            directionWeights[Math.floorMod(nearestAngle - angleIndexOffset, 12)] -= weightOffset;
          }
          //System.out.println("Used: " + (Clock.getBytecodeNum() - startBytecodes));
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
        directionWeights[3] -= weightOffset;
        directionWeights[4] -= weightOffset;
        directionWeights[5] -= weightOffset;
      }
      if (minY != UNKNOWN && here.y - minY < EDGE_BIAS_RADIUS) {
        float weightOffset = 1000 * (EDGE_BIAS_RADIUS - (here.y - minY));
        directionWeights[5] -= weightOffset;
        directionWeights[6] -= weightOffset;
        directionWeights[7] -= weightOffset;
      }
      if (maxX != UNKNOWN && maxX - here.x < EDGE_BIAS_RADIUS) {
        float weightOffset = 1000 * (EDGE_BIAS_RADIUS - (maxX - here.x));
        directionWeights[0] -= weightOffset;
        directionWeights[1] -= weightOffset;
        directionWeights[7] -= weightOffset;
      }
      if (maxY != UNKNOWN && maxY - here.y < EDGE_BIAS_RADIUS) {
        float weightOffset = 1000 * (EDGE_BIAS_RADIUS - (maxY - here.y));
        directionWeights[1] -= weightOffset;
        directionWeights[2] -= weightOffset;
        directionWeights[3] -= weightOffset;
      }
      for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
        System.out
            .println("Angle: " + (angleIndex * 30) + ", Weight: " + directionWeights[angleIndex]);
      }
      /*
      if (DEBUG) {
        for (int angleIndex = 0; angleIndex < 12; ++angleIndex) {
          System.out
              .println("Angle: " + (angleIndex * 30) + ", Weight: " + directionWeights[angleIndex]);
        }
      }
      */
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
        System.out.println("Trying to move in direction: " + angleDirections[moveAngleIndex]);
        moved = RobotUtils.tryMoveDist(angleDirections[moveAngleIndex], EVASION_STRIDE_RADIUS, 5,
            3);
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
      } while (!moved && ++attempts <= 9);
      return moved;
      /*
      if (DEBUG) {
        System.out.println("Bytecodes left: " + Clock.getBytecodesLeft());
      }
      */
      //System.out.println("Bytecodes left: " + Clock.getBytecodesLeft());

    } catch (Exception e) {
      e.printStackTrace();
    }
    return false;
  }
}
