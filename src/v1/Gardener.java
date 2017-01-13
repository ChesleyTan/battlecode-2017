package v1;

import battlecode.common.*;

public class Gardener extends Globals {

  private static int numTreesBuilt = 0;
  private static Direction lastDir = null;
  private static float detectRadius = 3f;
  private static boolean initialSetup = false;
  private static boolean plant = false;
  private static int spawnRound;

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
      float their_distance = (RobotType.GARDENER.sensorRadius - here.distanceTo(r.location))
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
    if (!rc.onTheMap(new MapLocation(here.x - 1, here.y))) {
      sumX += RobotType.GARDENER.strideRadius;
    }
    if (!rc.onTheMap(new MapLocation(here.x + 1, here.y))) {
      sumX -= RobotType.GARDENER.strideRadius;
    }
    if (!rc.onTheMap(new MapLocation(here.x, here.y - 1))) {
      sumY += RobotType.GARDENER.strideRadius;
    }
    if (!rc.onTheMap(new MapLocation(here.x, here.y + 1))) {
      sumY -= RobotType.GARDENER.strideRadius;
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
    Direction startDirection = NORTH;
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
      int scoutCount = rc.readBroadcast(EARLY_SCOUTS_CHANNEL);
      if (scoutCount == 0) {
        while(scoutCount < 3){
          checkspace();
          spawnScout();
          rc.broadcast(EARLY_SCOUTS_CHANNEL, scoutCount + 1);
          Clock.yield();
          scoutCount = rc.readBroadcast(EARLY_SCOUTS_CHANNEL);
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
          if (rc.getRoundNum() % 154/(rc.getTreeCount() + 1) == 0
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
        Clock.yield();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}