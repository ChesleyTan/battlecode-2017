package v1;

import battlecode.common.*;
import utils.Globals;

public class Gardener extends Globals {

  private static float detectRadius = 3f;
  private static boolean plant = false;
  private static int spawnRound;
  private static Direction startDirection = null;

  /*
   * Checks that there is enough space around the unit to begin planting
   */
  public static void checkspace() throws GameActionException {
    Globals.update();
    float sumX = 0;
    float sumY = 0;

    // Opposing forces created by Robots
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
    for (RobotInfo r : nearbyRobots) {
      Direction their_direction = here.directionTo(r.location).opposite();
      float their_distance = (RobotType.GARDENER.sensorRadius - here.distanceTo(r.location) + myType.bodyRadius + r.getRadius())
          / RobotType.GARDENER.sensorRadius * RobotType.GARDENER.strideRadius;
      sumX += their_direction.getDeltaX(their_distance);
      sumY += their_direction.getDeltaY(their_distance);
    }

    // Opposing forces created by Trees
    TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
    for (TreeInfo t : nearbyTrees) {
      Direction their_direction = t.location.directionTo(here);
      float their_distance = (RobotType.GARDENER.sensorRadius - here.distanceTo(t.location))
          / RobotType.GARDENER.sensorRadius * RobotType.GARDENER.strideRadius;
      sumX += their_direction.getDeltaX(their_distance);
      sumY += their_direction.getDeltaY(their_distance);
    }

    // Opposing forces created by Edge of Map
    float sightRadius = RobotType.GARDENER.sensorRadius;
    updateMapBoundaries();
    if (minX != UNKNOWN && !rc.onTheMap(new MapLocation(here.x - sightRadius, here.y))) {
      float distance = here.x - minX;
      float weightedDistance = distance / sightRadius * myType.strideRadius;
      sumX += weightedDistance;
    }
    if (maxX != UNKNOWN && !rc.onTheMap(new MapLocation(here.x + sightRadius, here.y))) {
      float distance = maxX - here.x;
      float weightedDistance = distance / sightRadius * myType.strideRadius;
      sumX -= weightedDistance;
    }
    if (minY != UNKNOWN && !rc.onTheMap(new MapLocation(here.x, here.y - sightRadius))) {
      float distance = here.y - minY;
      float weightedDistance = distance / sightRadius * myType.strideRadius;
      sumY += weightedDistance;
    }
    if (maxY != UNKNOWN && !rc.onTheMap(new MapLocation(here.x, here.y + sightRadius))) {
      float distance = maxY - here.y;
      float weightedDistance = distance / sightRadius * myType.strideRadius;
      sumY -= weightedDistance;
    }
    float finaldist = (float) Math.sqrt(sumX * sumX + sumY * sumY);

    Direction finalDir = new Direction(sumX, sumY);
    if (rc.canMove(finalDir) && !rc.hasMoved()) {
      rc.move(finalDir);
    }
    else {
      while (!rc.canMove(finalDir)) {
        finalDir = finalDir.rotateLeftDegrees(20);
      }
      rc.move(finalDir);
    }
  }
  
  private static void spawnScout() throws GameActionException{
    Direction randomDir = new Direction(rand.nextFloat() * 2 * (float)(Math.PI));
    while (!rc.canBuildRobot(RobotType.SCOUT, randomDir)){
      randomDir = randomDir.rotateLeftDegrees(10);
    }
    rc.buildRobot(RobotType.SCOUT, randomDir);
  }
  
  private static Direction[] possibleTrees() throws GameActionException{
    Direction[] freeSpaces = new Direction[6];
    int index = 0;
    int rotateAmt = 0;
    while (rotateAmt < 6){
      MapLocation treeDest = here.add(startDirection, RobotType.GARDENER.bodyRadius + GameConstants.BULLET_TREE_RADIUS);
      if (!rc.isCircleOccupiedExceptByThisRobot(treeDest, GameConstants.BULLET_TREE_RADIUS)){
        freeSpaces[index] = startDirection;
        index++;
      }
      startDirection = startDirection.rotateLeftDegrees(60);
      rotateAmt++;
    }
    return freeSpaces;
  }
  
  public static void loop() {

    // Initial setup moves to a clear spot and spawns 3 scouts
    try {
      startDirection = RobotPlayer.randomDirection();
      int scoutCount = rc.readBroadcast(EARLY_UNITS_CHANNEL);
      if (scoutCount == 0) {
        while(scoutCount < 3){
          checkspace();
          spawnScout();
          rc.broadcast(EARLY_UNITS_CHANNEL, scoutCount + 1);
          Clock.yield();
          scoutCount = rc.readBroadcast(EARLY_UNITS_CHANNEL);
          continue;
        }
      }
      spawnRound = rc.getRoundNum();
      // Loop: Build trees and water them, and occasionally build scouts
      while (true) {
        Globals.update();
        if (rc.getRoundNum() - spawnRound < 30){
          if (!rc.onTheMap(here, detectRadius)
              || rc.isCircleOccupiedExceptByThisRobot(here, detectRadius) && !plant) {
            checkspace();
            Clock.yield();
            continue;
          }
        }
        Direction[] freeSpaces = possibleTrees();
        if (freeSpaces[1] != null && rc.canPlantTree(freeSpaces[1])) {
          rc.plantTree(freeSpaces[1]);
          plant = true;
        }
        else {
          int division_factor = (int)(154/(rc.getTreeCount() + 1));
          if (rc.getRoundNum() % division_factor == 0
              && freeSpaces[0] != null
              && rc.canBuildRobot(RobotType.SCOUT, freeSpaces[0])) {
            rc.buildRobot(RobotType.SCOUT, freeSpaces[0]);
          }
        }
        TreeInfo[] nearbyTrees = rc.senseNearbyTrees(2, us);
        if (nearbyTrees != null && nearbyTrees.length != 0) {
          TreeInfo minWaterable = nearbyTrees[0];
          for (TreeInfo x : nearbyTrees) {
            if (rc.canWater(x.ID) && x.health < minWaterable.health) {
              minWaterable = x;
            }
          }
          if (rc.canWater(minWaterable.ID)) {
            rc.water(minWaterable.ID);
          }
        }
        if (rc.getHealth() < 3 && rc.senseNearbyRobots(1, them).length > 0){
          int gardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
          rc.broadcast(PRODUCED_GARDENERS_CHANNEL, gardeners - 1);
          rc.disintegrate();
        }
        if (rc.getTeamBullets() > 1000){
          float donateAmt = ((int)(rc.getTeamBullets() / 10) * 10) - 640;
          rc.donate(donateAmt);
        }
        Clock.yield();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}