package evasion;

import battlecode.common.*;

public strictfp class RobotPlayer extends Globals {
  /**
   * run() is the method that is called when a robot is instantiated in the
   * Battlecode world. If this method returns, the robot dies!
   **/
  @SuppressWarnings("unused")
  public static void run(RobotController rc) throws GameActionException {
    Globals.init(rc);

    // Here, we've separated the controls into a different method for each RobotType.
    // You can add the missing ones or rewrite this into your own control structure.
    switch (rc.getType()) {
      case ARCHON:
        EvasiveArchon.init();
        while (true) {
          EvasiveArchon.move();
          Clock.yield();
        }
      case GARDENER:
        runGardener();
        break;
      case SOLDIER:
        runSoldier();
        break;
      case LUMBERJACK:
        runLumberjack();
        break;
    }
  }

  static void runGardener() throws GameActionException {
  }

  static void runSoldier() throws GameActionException {
  }

  static void runLumberjack() throws GameActionException {
  }

  public static float degreesBetween(Direction a, Direction b) {
    return (float) Math.toDegrees(radiansBetween(a, b));
  }

  public static float radiansBetween(Direction a, Direction b) {
    return reduce(b.radians - a.radians);
  }

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

  /**
   * Returns a random Direction
   * @return a random Direction
   */
  static Direction randomDirection() {
    return new Direction((float) Math.random() * 2 * (float) Math.PI);
  }

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

    // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
    if (Math.abs(theta) > Math.PI / 2) {
      return false;
    }

    // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
    // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
    // This corresponds to the smallest radius circle centered at our location that would intersect with the
    // line that is the path of the bullet.
    float distToRobot = bulletLocation.distanceTo(myLocation);
    float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

    return (perpendicularDist <= myType.bodyRadius);
  }

  static boolean willCollideWithMyLocation(MapLocation bulletLocation,
      Direction propagationDirection, MapLocation myLocation) {

    // Calculate bullet relations to this robot
    Direction directionToRobot = bulletLocation.directionTo(myLocation);
    float theta = propagationDirection.radiansBetween(directionToRobot);

    // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
    if (Math.abs(theta) > Math.PI / 2) {
      return false;
    }

    // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
    // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
    // This corresponds to the smallest radius circle centered at our location that would intersect with the
    // line that is the path of the bullet.
    float distToRobot = bulletLocation.distanceTo(myLocation);
    float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

    return (perpendicularDist <= myType.bodyRadius);
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
