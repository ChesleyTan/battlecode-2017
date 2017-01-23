package utils;

import battlecode.common.*;

import static utils.Globals.here;
import static utils.Globals.rc;

/**
 * Created by Ryan on 1/20/2017.
 */
public class MovementUtils {

  /** Number of tests PER DIRECTION from the intended direction of movement */
  public static final int NUM_ANGLE_CHECKS = 4; //Number of tests PER DIRECTION from the intended direction
  public static final float SIZE_ANGLE_CHECKS = (float) Math.PI/2/NUM_ANGLE_CHECKS;


  /**
   * Attempts to move to a location, or the direction closest to it if not possible.
   * @param loc The MapLocation to move to
   * @return The MapLocation of the new position, or null if unmoved
   */
  public static MapLocation tryMove(MapLocation loc) {
    try{
      if (rc.canMove(loc)) {
        rc.move(loc);
        return loc;
      } else {
        Direction dir = here.directionTo(loc);
        if (dir == null) return null;
        return tryMove(dir);
      }
    } catch (GameActionException e) {
      return null;
    }
  }

  /**
   * Attempts to move 1 stride in a direction, or the direction closest to it if not possible.
   * @param dir The Direction to move in
   * @return The MapLocation of the new position, or null if unmoved
   */
  public static MapLocation tryMove(Direction dir) {
    try {
      if (rc.canMove(dir)) { //Short Circuit the check
        rc.move(dir);
        return here.add(dir, Globals.myType.strideRadius);
      } else {
        Direction right = dir;
        Direction left = dir;
        for (float theta = SIZE_ANGLE_CHECKS; theta<=(Math.PI/2)*1.001; theta+=SIZE_ANGLE_CHECKS ) {
          right = right.rotateRightRads(theta);
          if (rc.canMove(right)) {
            rc.move(right);
            return here.add(right, Globals.myType.strideRadius);
          }
          left = left.rotateLeftRads(theta);
          if (rc.canMove(left)) {
            rc.move(left);
            return here.add(left, Globals.myType.strideRadius);
          }
        }
        return null;
      }
    } catch (GameActionException e) {
      return null;
    }
  }

  /*public static MapLocation closestPointTo(MapLocation loc) {
    SensorUtils.updateRobots();
    SensorUtils.updateTrees();
    RobotInfo[] robots = SensorUtils.getNearbyRobots();
    TreeInfo[] trees = SensorUtils.getNearbyTrees();
    double netX, netY;
    for (int i=0; i<robots.length; i++) {
      MapLocation otherLoc = robots[i].location;
      float otherRadius = robots[i].getRadius();
      float dx = otherLoc.x - here.x;
      float dy = otherLoc.y - here.y;
      double distance = Math.sqrt(dx*dx + dy*dy);
      double collisionAmnt = Globals.myType.bodyRadius + otherRadius - distance;
      if (collisionAmnt <)
    }
  }*/

  public static boolean intersectsWithCircle(MapLocation center, float radius) {
    return here.distanceTo(center) >= (radius + Globals.myType.bodyRadius);
  }


  /*public static void setMoveTarget(MapLocation target) {
    MovementUtils.target = target;
  }

  /*public static void simpleMove() throws GameActionException {
    Direction targetDirection = here.directionTo(target);
    if (targetDirection == null) return; //We are already at the target, so do nothing
    if (rc.canMove(target)) { //Short circuit the move algorithm
      rc.move(target);
      return;
    }
    SensorUtils.updateTrees();
    SensorUtils.updateRobots();
    RobotInfo[] robots = SensorUtils.getNearbyRobots();
    TreeInfo[] trees = SensorUtils.getNearbyTrees();

    double dx = Math.cos(targetDirection.radians);
    double dy = Math.sin(targetDirection.radians);
    for (int i = 0; i<robots.length; i++) {

    }

  }*/

}
