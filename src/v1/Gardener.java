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
    System.out.println("SumX: " + sumX);
    System.out.println("SumY: " + sumY);
  }

  public static void loop() {

    // Initial setup moves to a clear spot and spawns 3 scouts
    try {
      checkspace();
      Clock.yield();
      int scoutCount = rc.readBroadcast(EARLY_SCOUTS_CHANNEL);
      if (scoutCount < 3) {
        initialSetup = true;
      }
      // Loop: Build trees and water them, and occasionally build scouts
      while (true) {
        scoutCount = rc.readBroadcast(EARLY_SCOUTS_CHANNEL);
        initialSetup = scoutCount < 3;
        Globals.update();
        if (!rc.onTheMap(here, detectRadius)
            || rc.isCircleOccupiedExceptByThisRobot(here, detectRadius) && !plant) {
          checkspace();
          Clock.yield();
          continue;
        }
        if (initialSetup) {
          if (rc.canBuildRobot(RobotType.SCOUT, NORTH)) {
            rc.buildRobot(RobotType.SCOUT, NORTH);
            rc.broadcast(EARLY_SCOUTS_CHANNEL, scoutCount + 1);
            Clock.yield();
            continue;
          }
        }
        if (numTreesBuilt < 5 && rc.canPlantTree(NORTH.rotateLeftDegrees(60 * numTreesBuilt))) {
          rc.plantTree(NORTH.rotateLeftDegrees(60 * numTreesBuilt));
          numTreesBuilt++;
          plant = true;
        }
        else if (numTreesBuilt == 0 && !rc.hasMoved()) {
          checkspace();
          Clock.yield();
          continue;
        }
        else {
          if (rc.getRoundNum() % 100 == 0
              && rc.canBuildRobot(RobotType.SCOUT, NORTH.rotateRightDegrees(60))) {
            rc.buildRobot(RobotType.SCOUT, NORTH.rotateRightDegrees(60));
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