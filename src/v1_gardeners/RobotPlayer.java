package v1_gardeners;

import battlecode.common.*;

public class RobotPlayer extends Globals {

  public static void run(RobotController rc) throws GameActionException {
    Globals.init(rc);
    switch (rc.getType()) {
      case ARCHON:
        Archon.loop();
        break;
      case GARDENER:
        Gardener.loop();
        break;
      case SCOUT:
        Scout.loop();
        break;
      case SOLDIER:
        runSoldier();
        break;
      case TANK:
        //Run Tank
        break;
    }
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

  static boolean tryMoveDist(Direction dir, float distance, float degreeOffset, int checksPerSide)
      throws GameActionException {

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

  static boolean willCollideWithTargetLocation(MapLocation bulletLocation,
      Direction propagationDirection, MapLocation TargetLocation, float bodyRadius) {

    // Calculate bullet relations to this robot
    Direction directionToRobot = bulletLocation.directionTo(TargetLocation);
    float theta = propagationDirection.radiansBetween(directionToRobot);
    float distToRobot = bulletLocation.distanceTo(TargetLocation);

    // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
    if (distToRobot > myType.bodyRadius && Math.abs(theta) > Math.PI / 2) {
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
  
  // ------------ Example Func Player code ----------------
  
  static void runSoldier() throws GameActionException {
    System.out.println("I'm an soldier!");
    Team enemy = rc.getTeam().opponent();

    // The code you want your robot to perform every round should be in this loop
    while (true) {

        // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
        try {
            MapLocation myLocation = rc.getLocation();

            // See if there are any nearby enemy robots
            RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

            // If there are some...
            if (robots.length > 0) {
                // And we have enough bullets, and haven't attacked yet this turn...
                if (robots.length == 1 && rc.canFireSingleShot()) {
                    // ...Then fire a bullet in the direction of the enemy.
                    rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                }
                else if (robots.length > 1 && rc.canFireTriadShot()) {
                    rc.fireTriadShot(rc.getLocation().directionTo(robots[0].location));
                }
            }

            // Move randomly
            tryMove(randomDirection(), 10, 3);

            // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
            Clock.yield();

        } catch (Exception e) {
            System.out.println("Soldier Exception");
            e.printStackTrace();
        }
    }
}

}