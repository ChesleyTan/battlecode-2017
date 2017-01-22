package v2_soldier;

import battlecode.common.*;
import utils.RobotUtils;
import utils.Globals;

public class EvasiveSoldier extends Globals {
  // Change to RobotType enums
  static Direction[] angleDirections = new Direction[8];
  static final int EDGE_BIAS_RADIUS = 8;
  static final int BULLET_DETECT_RADIUS = 6;
  static final int ENEMY_DETECT_RADIUS = 6;
  static int lastMoveAngleIndex = -1;
  static final float EVASION_STRIDE_RADIUS = RobotType.SOLDIER.strideRadius;
  private static MapLocation[] moveLocations = new MapLocation[8];

  /*static void init() {
    for (int angle = 0; angle < 8; ++angle) {
      angleDirections[angle] = new Direction((float) (angle * Math.PI / 4));
    }
  }*/

  static boolean move(BulletInfo[] nearbyBullets, RobotInfo[] nearbyRobots, RobotInfo target, MapLocation destination) {
    // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
    try {
      Globals.update();
      //int startBytecodes = Clock.getBytecodeNum();
      /*
      if (DEBUG) {
        System.out.println("========== Round: " + rc.getRoundNum() + "==========");
        System.out.println(here);
      }
      */
      if (target == null || 
          target.getType() == RobotType.SOLDIER || 
          target.getType() == RobotType.LUMBERJACK || 
          target.getType() == RobotType.TANK){
        for (int angle = 0; angle < 8; ++angle) {
          angleDirections[angle] = new Direction((float) (angle * Math.PI / 4));
        }
      }
      else{
        float toTarget = here.directionTo(target.location).radians;
        for (int angle = 0; angle < 8; ++angle){
          angleDirections[angle] = new Direction((float) (angle * Math.PI / 8 - Math.PI / 2 + toTarget));
        }
      }
      for (int angleIndex = 0; angleIndex < 8; ++angleIndex) {
        moveLocations[angleIndex] = here.add(angleDirections[angleIndex], EVASION_STRIDE_RADIUS);
      }
      float[] directionWeights = new float[8];

      boolean unsafeFromUnit = false;
      for (RobotInfo ri : nearbyRobots) {
        // Only avoid lumberjacks if within 3 units away
        if (ri.type.canAttack() && (ri.type != RobotType.LUMBERJACK) || (here.distanceTo(ri.location) < 3f)) {
          unsafeFromUnit = true;
          Direction enemyAngle = here.directionTo(ri.location);
          /*
          if (DEBUG) {
            System.out.println("Enemy angle: " + enemyAngle.getAngleDegrees());
          }
          */
          for (int angleIndex = 0; angleIndex < 8; ++angleIndex) {
            // TODO add weight for other units or optimize this loop
            float angleDelta = Math.abs(enemyAngle.degreesBetween(angleDirections[angleIndex]));
            if (angleDelta > 70) {
              continue;
            }
            float distBetween = moveLocations[angleIndex].distanceTo(ri.location);
            if (ri.type == RobotType.LUMBERJACK) {
              float weightOffset = (150 * (70 - angleDelta))
                  + 1000 * (EVASION_STRIDE_RADIUS + ENEMY_DETECT_RADIUS - distBetween);
              directionWeights[angleIndex] -= weightOffset;
            }
            else if (ri.type == RobotType.SOLDIER) {
              float weightOffset = (150 * (70 - angleDelta))
                  + 1000 * (EVASION_STRIDE_RADIUS + ENEMY_DETECT_RADIUS - distBetween);
              directionWeights[angleIndex] -= weightOffset;
            }
            else if (ri.type == RobotType.SCOUT) {
              float weightOffset = (-10 * (70 - angleDelta))
                  + 1000 * (EVASION_STRIDE_RADIUS + ENEMY_DETECT_RADIUS - distBetween);
              directionWeights[angleIndex] -= weightOffset;
            }
            else if (ri.type == RobotType.TANK) {
              float weightOffset = (200 * (70 - angleDelta))
                  + 1000 * (EVASION_STRIDE_RADIUS + ENEMY_DETECT_RADIUS - distBetween);
              directionWeights[angleIndex] -= weightOffset;
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
      boolean unsafeFromBullet = false;
      for (int i = 0; i < nearbyBullets.length; ++i) {
        if (Clock.getBytecodesLeft() < 2000) {
          break;
        }
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
        for (int angleIndex = 0; angleIndex < 8; ++angleIndex) {
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
            directionWeights[angleIndex] -= (15000
                + 1000 * (EVASION_STRIDE_RADIUS + BULLET_DETECT_RADIUS - distToRobot)
                + 1000 * (bi.damage * bi.damage));
            /*
            if (DEBUG) {
              System.out.println("Angle " + (angleIndex * 30) + " is unsafe.");
            }
            */
          }
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
      /*
      if (DEBUG) {
        for (int angleIndex = 0; angleIndex < 8; ++angleIndex) {
          System.out
              .println("Angle: " + (angleIndex * 45) + ", Weight: " + directionWeights[angleIndex]);
        }
      }
      */
      if (unsafeFromUnit || unsafeFromBullet || (target == null && destination == null)) {
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
        int moveAngleIndex = 0;
        int attempts = 0;
        boolean moved = false;
        
        int movementBiasSeed = Math.abs(rand.nextInt());
        do {
          // Prevent clockwise bias in movement angle starting from 0 degrees
          moveAngleIndex = movementBiasSeed % 8;
          movementBiasSeed /= 8;
          for (int angleIndex = 0; angleIndex < 8; ++angleIndex) {
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
          moved = RobotUtils.tryMoveDist(angleDirections[moveAngleIndex], EVASION_STRIDE_RADIUS, 5,
              3);
          /*
          if (DEBUG) {
            rc.setIndicatorLine(here, here.add(angleDirections[moveAngleIndex], 1), 0, 255, 0);
            rc.setIndicatorLine(here.add(angleDirections[moveAngleIndex], 1),
                here.add(angleDirections[moveAngleIndex], 1.5f), 255, 0, 0);
            rc.setIndicatorDot(here, 0, 0, 0);
            for (int angleIndex = 0; angleIndex < 8; ++angleIndex) {
              rc.setIndicatorDot(here.add(angleDirections[angleIndex], 2),
                  (int) Math.max(-25500, directionWeights[angleIndex]) / (-100),
                  (int) Math.max(-25500, directionWeights[angleIndex]) / (-100),
                  (int) Math.max(-25500, directionWeights[angleIndex]) / (-100));
            }
          }
          */
          directionWeights[moveAngleIndex] -= 999999;
        } while (!moved && ++attempts <= 6);
        return moved;
      }
      else{
        if (destination != null){
          RobotUtils.tryMoveDestination(destination);
        }
      }
      //System.out.println("Used: " + (Clock.getBytecodeNum() - startBytecodes));
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
    return false;
  }
}
