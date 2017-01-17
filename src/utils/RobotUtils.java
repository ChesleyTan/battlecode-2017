package utils;

import battlecode.common.*;

public class RobotUtils extends Globals {

  /**
   * Returns a random Direction
   * @return a random Direction
   */
  public static Direction randomDirection() {
    return new Direction(rand.nextFloat() * 2 * (float) Math.PI);
  }

  public static void donateEverythingAtTheEnd() throws GameActionException {
    float bullets = rc.getTeamBullets();
    if (currentRoundNum == penultimateRound || bullets >= 10000f) {
      rc.donate(bullets);
    }
  }

  public static void shakeNearbyTrees() throws GameActionException {
    if (Clock.getBytecodesLeft() > 500) {
      TreeInfo[] nearbyTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
      for (TreeInfo ti : nearbyTrees) {
        if (rc.canShake(ti.ID)) {
          rc.shake(ti.ID);
          break;
        }
      }
    }
  }

  public static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide)
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

  public static boolean tryMoveDist(Direction dir, float distance, float degreeOffset,
      int checksPerSide) throws GameActionException {

    // First, try intended direction
    if (rc.canMove(dir, distance)) {
      rc.move(dir, distance);
      return true;
    }

    // Now try a bunch of similar angles
    boolean moved = false;
    int currentCheck = 1;

    while (currentCheck <= checksPerSide) {
      // Try the offset of the left side
      if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck), distance)) {
        rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck), distance);
        return true;
      }
      // Try the offset on the right side
      if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck), distance)) {
        rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck), distance);
        return true;
      }
      // No move performed, try slightly further
      currentCheck++;
    }

    // A move never happened, so return false.
    return false;
  }

  /**
   * A slightly more complicated example function, this returns true if the
   * given bullet is on a collision course with the current robot. Doesn't take
   * into account objects between the bullet and this robot.
   * @param bullet
   *          The bullet in question
   * @return True if the line of the bullet's path intersects with this robot's
   *         current position.
   */
  public static boolean willCollideWithMe(BulletInfo bullet) {
    MapLocation myLocation = here;

    // Get relevant bullet information
    Direction propagationDirection = bullet.dir;
    MapLocation bulletLocation = bullet.location;

    // Calculate bullet relations to this robot
    Direction directionToRobot = bulletLocation.directionTo(myLocation);
    float theta = propagationDirection.radiansBetween(directionToRobot);
    float distToRobot = bulletLocation.distanceTo(myLocation);

    // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
    if (distToRobot > myType.bodyRadius && Math.abs(theta) > Math.PI / 2) {
      return false;
    }

    // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
    // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
    // This corresponds to the smallest radius circle centered at our location that would intersect with the
    // line that is the path of the bullet.
    float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

    return (perpendicularDist <= myType.bodyRadius);
  }

  public static boolean willCollideWithTargetLocation(MapLocation bulletLocation,
      Direction propagationDirection, MapLocation TargetLocation, float bodyRadius) {

    // Calculate bullet relations to this robot
    Direction directionToRobot = bulletLocation.directionTo(TargetLocation);
    float theta = propagationDirection.radiansBetween(directionToRobot);
    float distToRobot = bulletLocation.distanceTo(TargetLocation);

    // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
    if (distToRobot > bodyRadius && Math.abs(theta) > Math.PI / 2) {
      return false;
    }

    // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
    // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
    // This corresponds to the smallest radius circle centered at our location that would intersect with the
    // line that is the path of the bullet.
    float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

    return (perpendicularDist <= bodyRadius);
  }

  public static boolean enemyNearby() throws GameActionException {
    RobotInfo[] robots = rc.senseNearbyRobots();
    for (RobotInfo x : robots) {
      if (x.getTeam() == them) {
        return true;
      }
    }
    return false;
  }
}