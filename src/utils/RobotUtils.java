package utils;

import battlecode.common.*;

public class RobotUtils extends Globals {

  private static final int BUG = 1;
  private static final int DIRECT = 0;
  private static MapLocation bugStartLocation;
  private static MapLocation bugDestinationLocation;
  private static int bugState = DIRECT;
  private static boolean wallSideLeft;
  private static Direction bugStartDirection;
  private static int bugCount;
  private static Direction lastDirection;

  public static void bugStart(MapLocation finalLoc) throws GameActionException {
    bugStartLocation = here;
    bugState = BUG;
    bugStartDirection = here.directionTo(finalLoc);
    bugDestinationLocation = finalLoc;
    bugCount = 0;
    Direction leftDir = bugStartDirection;
    Direction rightDir = bugStartDirection;
    int attempts = 0;
    while (!canMove(leftDir) && attempts < 18) {
      leftDir = leftDir.rotateLeftDegrees(10);
      attempts++;
    }
    attempts = 0;
    while (!canMove(rightDir) && attempts < 18) {
      rightDir = rightDir.rotateRightDegrees(10);
      attempts++;
    }
    if (!canMove(leftDir)) {
      wallSideLeft = false;
      //lastDirection = rightDir;
    }
    else if (!canMove(rightDir)) {
      wallSideLeft = true;
      //lastDirection = leftDir;
    }
    else {
      float distanceLeft = here.add(leftDir, rc.getType().strideRadius).distanceTo(finalLoc);
      float distanceRight = here.add(rightDir, rc.getType().strideRadius).distanceTo(finalLoc);
      wallSideLeft = distanceLeft > distanceRight;
      //lastDirection = wallSideLeft? rightDir : leftDir;
    }
    /*if (bugStartDirection.getDeltaX(1) * bugStartDirection.getDeltaY(1) > 0){
      wallSideLeft = false;
    }
    else{
      wallSideLeft = true;
    }*/
  }

  public static int getBugCount() {
    return bugCount;
  }

  public static boolean bugMove() throws GameActionException {
    if (DEBUG) {
      System.out.println("bugging");
    }
    bugStartDirection = here.directionTo(bugDestinationLocation);
    if (DEBUG) {
      System.out.println("start direction: " + bugStartDirection.getAngleDegrees());
    }
    if (canMove(bugStartDirection) && bugCount > 1) {
      rc.move(bugStartDirection);
      endBug();
      return true;
    }
    int rotationAmount = wallSideLeft ? 10 : -10;
    //Direction startDir = lastDirection.rotateLeftDegrees(rotationAmount * 2);
    Direction startDir = bugStartDirection;
    int attempts = 0;
    while (!canMove(startDir) && attempts < 18) {
      System.out.println(startDir.getAngleDegrees());
      rc.setIndicatorLine(here.add(startDir), here.add(startDir, 2), 255, 70, 101);
      System.out.println(MathUtils.isNear(startDir, NORTH, 10));
      System.out.println("can move north: " + rc.canMove(NORTH));
      System.out.println(MathUtils.isNear(startDir, SOUTH, 10));
      System.out.println("can move south: " + rc.canMove(SOUTH));
      System.out.println(MathUtils.isNear(startDir, EAST, 10));
      System.out.println("can move east: " + rc.canMove(EAST));
      System.out.println(MathUtils.isNear(startDir, WEST, 10));
      System.out.println("can move west: " + rc.canMove(WEST));
      if (MathUtils.isNear(startDir, NORTH, 5)){
        if (canMove(NORTH)) {
          startDir = NORTH;
          break;
        }
      }
      else if (MathUtils.isNear(startDir, SOUTH, 5)){
        if (canMove(SOUTH)){
          startDir = SOUTH;
          break;
        }
      }
      else if (MathUtils.isNear(startDir, EAST, 5)){
        if (canMove(EAST)){
          startDir = EAST;
          break;
        }
      }
      else if (MathUtils.isNear(startDir, WEST, 5)){
        if (canMove(WEST)){
          startDir = WEST;
          break;
        }
      }
      startDir = startDir.rotateRightDegrees(rotationAmount);
      attempts++;
    }
    System.out.println("startdir:" + startDir.getAngleDegrees());
    System.out.println("canmove startDir" + canMove(startDir));
    if (!canMove(startDir)) {
      attempts = 0;
      startDir = bugStartDirection;
      wallSideLeft = !wallSideLeft;
      while (!canMove(startDir) && attempts < 18) {
        startDir = startDir.rotateLeftDegrees(rotationAmount);
        attempts++;
      }
    }
    if (canMove(startDir)) {
      rc.move(startDir);
      //lastDirection = startDir;
      bugCount++;
      return true;
    }
    else {
      if (DEBUG) {
        System.out.println("Stuck");
      }
      return false;
    }
  }

  /**
   * Returns a random Direction
   * @return a random Direction
   */
  public static Direction randomDirection() {
    return new Direction(rand.nextFloat() * 2 * (float) Math.PI);
  }

  public static void donateEverythingAtTheEnd() throws GameActionException {
    float bullets = rc.getTeamBullets();
    if (currentRoundNum == penultimateRound
        || rc.getTeamVictoryPoints() + (bullets / rc.getVictoryPointCost()) >= 1000) {
      rc.donate(bullets);
    }
    else if (bullets > 2000) {
      rc.donate(500);
    }
  }

  public static void shakeNearbyTrees() throws GameActionException {
    if (Clock.getBytecodesLeft() > 500) {
      TreeInfo[] nearbyTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
      for (TreeInfo ti : nearbyTrees) {
        if (rc.canShake(ti.getID()) && ti.getContainedBullets() > 0) {
          rc.shake(ti.getID());
          break;
        }
      }
    }
  }

  public static boolean canMove(Direction dir) throws GameActionException {
    if (myType != RobotType.TANK) {
      return rc.canMove(dir);
    }
    else {
      return tankCanMove(dir);
    }
  }

  public static boolean canMove(MapLocation loc) throws GameActionException {
    if (myType != RobotType.TANK) {
      return rc.canMove(loc);
    }
    else {
      return tankCanMove(loc);
    }
  }

  public static boolean canMove(Direction dir, float dist) throws GameActionException {
    if (myType != RobotType.TANK) {
      return rc.canMove(dir, dist);
    }
    else {
      return tankCanMove(dir, dist);
    }
  }

  public static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide)
      throws GameActionException {

    if (bugState == BUG) {
      return bugMove();
    }
    // First, try intended direction
    if (canMove(dir)) {
      rc.move(dir);
      return true;
    }

    // Now try a bunch of similar angles
    int currentCheck = 1;

    while (currentCheck <= checksPerSide) {
      if (Clock.getBytecodesLeft() < 1000) {
        return false;
      }
      // Try the offset of the left side
      //rc.setIndicatorLine(here, here.add(dir.rotateLeftDegrees(degreeOffset * currentCheck), 3), 255, 0, 0);
      if (canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
        rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
        return true;
      }
      // Try the offset on the right side
      //rc.setIndicatorLine(here, here.add(dir.rotateRightDegrees(degreeOffset * currentCheck), 3), 255, 0, 0);
      if (canMove(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
        rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
        return true;
      }
      // No move performed, try slightly further
      currentCheck++;
    }

    // A move never happened, so return false.
    return false;
  }

  public static boolean tryMoveIfSafe(Direction dir, BulletInfo[] nearbyBullets, float degreeOffset,
      int checksPerSide) throws GameActionException {
    // First, try intended direction
    //System.out.println("Called tryMove");
    MapLocation newLoc = here.add(dir, myType.strideRadius);
    rc.setIndicatorLine(here, newLoc, 0, 255, 0);
    if (canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
      //System.out.println("tryMove: " + newLoc);
      rc.move(newLoc);
      return true;
    }
    else if (Clock.getBytecodesLeft() < 2000) {
      return false;
    }

    // Now try a bunch of similar angles
    int currentCheck = 1;

    while (currentCheck <= checksPerSide) {
      // Try the offset of the left side
      float offset = degreeOffset * currentCheck;
      newLoc = here.add(dir.rotateLeftDegrees(offset), myType.strideRadius);
      rc.setIndicatorLine(here, newLoc, 255, 0, 0);
      if (canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
        rc.move(newLoc);
        //System.out.println("tryMove: " + newLoc);
        return true;
      }
      else if (Clock.getBytecodesLeft() < 2000) {
        return false;
      }
      newLoc = here.add(dir.rotateRightDegrees(offset), myType.strideRadius);
      rc.setIndicatorLine(here, newLoc, 255, 0, 0);
      // Try the offset on the right side
      if (canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
        rc.move(newLoc);
        //System.out.println("tryMove: " + newLoc);
        return true;
      }
      else if (Clock.getBytecodesLeft() < 2000) {
        return false;
      }
      // No move performed, try slightly further
      currentCheck++;
    }

    // A move never happened, so return false.
    return false;
  }

  public static boolean isLocationSafe(BulletInfo[] nearbyBullets, MapLocation loc) {
    for (BulletInfo bi : nearbyBullets) {
      if (Clock.getBytecodesLeft() < 2000) {
        return false;
      }
      if (RobotUtils.willCollideWithTargetLocation(bi.getLocation(), bi.getDir(), loc,
          myType.bodyRadius)) {
        return false;
      }
    }
    return true;
  }

  public static void endBug() {
    bugStartDirection = null;
    bugDestinationLocation = null;
    bugStartLocation = null;
    bugState = DIRECT;
    bugCount = 0;
    //lastDirection = null;
  }

  public static boolean tryMoveDestination(MapLocation target) throws GameActionException {
    if (DEBUG) {
      System.out.println("tryMoveDestination");
    }
    //System.out.println(target.x);
    //System.out.println(target.y);
    bugStartDirection = here.directionTo(target);
    if (canMove(bugStartDirection)) {
      rc.move(bugStartDirection);
      if (bugState == BUG) {
        endBug();
      }
    }
    else {
      if (bugState == BUG) {
        bugDestinationLocation = target;
      }
      else {
        bugStart(target);
      }
      bugMove();
    }
    return bugState == BUG;
  }

  public static boolean tryMoveDist(Direction dir, float distance, float degreeOffset,
      int checksPerSide) throws GameActionException {

    // First, try intended direction
    if (canMove(dir, distance)) {
      rc.move(dir, distance);
      return true;
    }

    // Now try a bunch of similar angles
    boolean moved = false;
    int currentCheck = 1;

    while (currentCheck <= checksPerSide) {
      if (Clock.getBytecodesLeft() < 1000) {
        return false;
      }
      // Try the offset of the left side
      //rc.setIndicatorLine(here, here.add(dir.rotateLeftDegrees(degreeOffset * currentCheck), 3), 255, 0, 0);
      if (canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck), distance)) {
        rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck), distance);
        return true;
      }
      // Try the offset on the right side
      //rc.setIndicatorLine(here, here.add(dir.rotateRightDegrees(degreeOffset * currentCheck), 3), 255, 0, 0);
      if (canMove(dir.rotateRightDegrees(degreeOffset * currentCheck), distance)) {
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
    Direction propagationDirection = bullet.getDir();
    MapLocation bulletLocation = bullet.getLocation();

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

  public static void notifyBytecodeLimitBreach() {
    if (rc.getRoundNum() != currentRoundNum) {
      rc.setIndicatorDot(rc.getLocation(), 102, 0, 102);
    }
  }

  public static boolean tankCanMove(MapLocation destination) throws GameActionException {
    if (!rc.canMove(destination)) {
      return false;
    }

    float distToDest = here.distanceTo(destination);
    MapLocation newLoc = here.add(here.directionTo(destination),
        Math.min(distToDest, RobotType.TANK.strideRadius));
    TreeInfo[] friendlyTreesBetween = rc.senseNearbyTrees(newLoc, RobotType.TANK.bodyRadius, us);
    if (friendlyTreesBetween.length != 0) {
      return false;
    }
    TreeInfo[] neutralTreesBetween = rc.senseNearbyTrees(newLoc, RobotType.TANK.bodyRadius,
        Team.NEUTRAL);
    if (neutralTreesBetween.length == 0
        || (neutralTreesBetween.length != 0 && neutralTreesBetween[0].getHealth() <= 200)) {
      return true;
    }
    return false;
  }

  public static boolean tankCanMove(Direction direction) throws GameActionException {
    if (!rc.canMove(direction)) {
      return false;
    }
    MapLocation newLoc = here.add(direction, RobotType.TANK.strideRadius);
    TreeInfo[] friendlyTreesBetween = rc.senseNearbyTrees(newLoc, RobotType.TANK.bodyRadius, us);
    if (friendlyTreesBetween.length != 0) {
      return false;
    }
    TreeInfo[] neutralTreesBetween = rc.senseNearbyTrees(newLoc, RobotType.TANK.bodyRadius,
        Team.NEUTRAL);
    if (neutralTreesBetween.length == 0
        || (neutralTreesBetween.length != 0 && neutralTreesBetween[0].getHealth() <= 200)) {
      return true;
    }
    return false;
  }

  public static boolean tankCanMove(Direction direction, float dist) throws GameActionException {
    if (!rc.canMove(direction, dist)) {
      return false;
    }
    MapLocation newLoc = here.add(direction, dist);
    TreeInfo[] friendlyTreesBetween = rc.senseNearbyTrees(newLoc, RobotType.TANK.bodyRadius, us);
    if (friendlyTreesBetween.length != 0) {
      return false;
    }
    TreeInfo[] neutralTreesBetween = rc.senseNearbyTrees(newLoc, RobotType.TANK.bodyRadius,
        Team.NEUTRAL);
    if (neutralTreesBetween.length == 0
        || (neutralTreesBetween.length != 0 && neutralTreesBetween[0].getHealth() <= 200)) {
      return true;
    }
    return false;
  }
}