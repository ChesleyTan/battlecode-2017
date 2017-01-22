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
  private static Direction lastDirection;
  
  
  public static void bugStart(MapLocation finalLoc){
    bugStartLocation = here;
    bugState = BUG;
    bugStartDirection = here.directionTo(finalLoc);
    bugDestinationLocation = finalLoc;
    if (bugStartDirection.getDeltaX(1) * bugStartDirection.getDeltaY(1) > 0){
      wallSideLeft = false;
    }
    else{
      wallSideLeft = true;
    }
  }
  
  public static boolean bugMove() throws GameActionException{
    System.out.println("bugging");
    bugStartDirection = here.directionTo(bugDestinationLocation);
    System.out.println("start direction: " + bugStartDirection.getAngleDegrees());
    if (rc.canMove(here.directionTo(bugDestinationLocation))){
      rc.move(bugStartDirection);
      bugStartLocation = null;
      bugState = DIRECT;
      bugStartDirection = null;
      return true;
    }
    Direction startDir = bugStartDirection;
    int rotationAmount = wallSideLeft? 10 : -10;
    int attempts = 0;
    while(!rc.canMove(startDir) && attempts < 18){
      startDir = startDir.rotateLeftDegrees(rotationAmount);
      attempts++;
    }
    if (!rc.canMove(startDir)){
      attempts = 0;
      startDir = bugStartDirection;
      wallSideLeft = !wallSideLeft;
      while(!rc.canMove(startDir) && attempts < 18){
        startDir = startDir.rotateRightDegrees(rotationAmount);
        attempts ++;
      }
    }
    if (rc.canMove(startDir)){
      rc.move(startDir);
      return true;
    }
    else{
      System.out.println("Stuck");
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
    if (rc.getTeamVictoryPoints() + (bullets / rc.getVictoryPointCost()) >= 1000) {
      rc.donate(bullets);
    }
    else if (currentRoundNum == penultimateRound) {
      rc.donate(bullets);
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


  public static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide)
      throws GameActionException {
    
    if (bugState == BUG){
      return bugMove();
    }
    // First, try intended direction
    if (rc.canMove(dir)) {
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
      if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
        rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
        return true;
      }
      // Try the offset on the right side
      //rc.setIndicatorLine(here, here.add(dir.rotateRightDegrees(degreeOffset * currentCheck), 3), 255, 0, 0);
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
  
  public static boolean tryMoveIfSafe(Direction dir, BulletInfo[] nearbyBullets,
      float degreeOffset, int checksPerSide) throws GameActionException {
    // First, try intended direction
    //System.out.println("Called tryMove");
    MapLocation newLoc = here.add(dir, myType.strideRadius);
    rc.setIndicatorLine(here, newLoc, 0, 255, 0);
    if (rc.canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
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
      if (rc.canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
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
      if (rc.canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
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

  public static void endBug(){
    bugStartDirection = null;
    bugDestinationLocation = null;
    bugStartLocation = null;
    bugState = DIRECT;
  }
  
  public static boolean tryMoveDestination(MapLocation target) throws GameActionException{
    System.out.println("tryMoveDestination");
    //System.out.println(target.x);
    //System.out.println(target.y);
    bugStartDirection = here.directionTo(target);
    if(rc.canMove(bugStartDirection)){
      rc.move(bugStartDirection);
      if (bugState == BUG){
        endBug();
      }
    }
    else{
      if (bugState == BUG){
        bugDestinationLocation = target;
      }
      else{
        bugStart(target);
      }
      bugMove();
    }
    return bugState==BUG;
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
      if (Clock.getBytecodesLeft() < 1000) {
        return false;
      }
      // Try the offset of the left side
      //rc.setIndicatorLine(here, here.add(dir.rotateLeftDegrees(degreeOffset * currentCheck), 3), 255, 0, 0);
      if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck), distance)) {
        rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck), distance);
        return true;
      }
      // Try the offset on the right side
      //rc.setIndicatorLine(here, here.add(dir.rotateRightDegrees(degreeOffset * currentCheck), 3), 255, 0, 0);
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
}