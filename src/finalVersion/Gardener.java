package finalVersion;

import battlecode.common.*;
import utils.Globals;
import utils.RobotUtils;

public class Gardener extends Globals {

  private static float detectRadius = 3f;
  private static boolean plant = false;
  private static int spawnRound;
  private static Direction startDirection = null;
  private static boolean production_gardener = false;
  private static boolean hasReportedDeath = false;

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
      float their_distance = (RobotType.GARDENER.sensorRadius - here.distanceTo(r.location) + r.getRadius())
          / RobotType.GARDENER.sensorRadius * RobotType.GARDENER.strideRadius;
      sumX += their_direction.getDeltaX(their_distance);
      sumY += their_direction.getDeltaY(their_distance);
    }

    // Opposing forces created by Trees
    TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
    for (TreeInfo t : nearbyTrees) {
      Direction their_direction = t.location.directionTo(here);
      float their_distance = (RobotType.GARDENER.sensorRadius - here.distanceTo(t.location) + t.getRadius())
          / RobotType.GARDENER.sensorRadius * RobotType.GARDENER.strideRadius;
      sumX += their_direction.getDeltaX(their_distance);
      sumY += their_direction.getDeltaY(their_distance);
    }

    // Opposing forces created by Edge of Map
    float radius = RobotType.GARDENER.bodyRadius;
    if (!rc.onTheMap(new MapLocation(here.x - 2 - radius, here.y))) {
      sumX += RobotType.GARDENER.strideRadius;
    }
    if (!rc.onTheMap(new MapLocation(here.x + 2 + radius, here.y))) {
      sumX -= RobotType.GARDENER.strideRadius;
    }
    if (!rc.onTheMap(new MapLocation(here.x, here.y - 2 - radius))) {
      sumY += RobotType.GARDENER.strideRadius;
    }
    if (!rc.onTheMap(new MapLocation(here.x, here.y + 2 + radius))) {
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

  private static boolean spawnRobot(RobotType t) throws GameActionException {
    Direction randomDir = new Direction(rand.nextFloat() * 2 * (float) (Math.PI));
    int attempts = 0;
    while (!rc.canBuildRobot(t, randomDir) && attempts < 36) {
      randomDir = randomDir.rotateLeftDegrees(10);
      attempts++;
    }
    if (rc.canBuildRobot(t, randomDir)) {
      rc.buildRobot(t, randomDir);
      return true;
    }
    return false;
  }

  private static Direction[] possibleTrees() throws GameActionException {
    Direction[] freeSpaces = new Direction[6];
    int index = 0;
    int rotateAmt = 0;
    while (rotateAmt < 6) {
      MapLocation treeDest = here.add(startDirection,
          RobotType.GARDENER.bodyRadius + GameConstants.BULLET_TREE_RADIUS);
      if (!rc.isCircleOccupiedExceptByThisRobot(treeDest, GameConstants.BULLET_TREE_RADIUS)) {
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
      startDirection = RobotUtils.randomDirection();
      int producedGardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
      int productionGardeners = rc.readBroadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL);
      int requiredProductionGardeners = rc.readBroadcast(PRODUCTION_GARDENERS_CHANNEL);
      if (productionGardeners < requiredProductionGardeners) {
        production_gardener = true;
        rc.broadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL, productionGardeners + 1);
      }
      spawnRound = rc.getRoundNum();
      // Loop: Build trees and water them, and occasionally build scouts
      while (true) {
        Globals.update();
        int scoutCount = rc.readBroadcast(EARLY_SCOUTS_CHANNEL);
        if (rc.getRoundNum() < 100 && scoutCount < 3) {
          checkspace();
          if (spawnRobot(RobotType.SCOUT)) {
            rc.broadcast(EARLY_SCOUTS_CHANNEL, scoutCount + 1);
          }
        }
        else {
          if (production_gardener) {
            checkspace();
            spawnRobot(RobotType.SCOUT);
            Clock.yield();
            continue;
          }
          if (rc.getRoundNum() - spawnRound < 30) {
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
            int division_factor = (int) (154 / (rc.getTreeCount() + 1));
            if (rc.getRoundNum() % division_factor == 0 && freeSpaces[0] != null
                && rc.canBuildRobot(RobotType.LUMBERJACK, freeSpaces[0])) {
              rc.buildRobot(RobotType.LUMBERJACK, freeSpaces[0]);
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
        }
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(5, them);
        RobotInfo attacker = null;
        for (RobotInfo ri : nearbyEnemies) {
          if (ri.type.canAttack()) {
            attacker = ri;
            break;
          }
        }
        // Call for defensive backup
        if (attacker != null) {
          float myHP = rc.getHealth();
          if (myHP > RobotType.GARDENER.maxHealth / 2) {
            rc.setIndicatorDot(here, 255, 0, 0);
            boolean calledForBackup = false;
            for (int channel = ATTACK_START_CHANNEL; channel < ATTACK_END_CHANNEL; channel += 4) {
              if (rc.readBroadcast(channel) != 0 && rc.readBroadcast(channel + 1) == 0) {
                rc.broadcast(channel + 1, attacker.ID);
                rc.broadcast(channel + 2, (int) attacker.location.x);
                rc.broadcast(channel + 3, (int) attacker.location.y);
                calledForBackup = true;
                break;
              }
            }
            if (!calledForBackup) {
              rc.broadcast(ATTACK_START_CHANNEL + 1, attacker.ID);
              rc.broadcast(ATTACK_START_CHANNEL + 2, (int) attacker.location.x);
              rc.broadcast(ATTACK_START_CHANNEL + 3, (int) attacker.location.y);
              calledForBackup = true;
            }
          }
          else if (!hasReportedDeath && myHP < 3 && attacker != null) {
            int gardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
            hasReportedDeath = true;
            rc.broadcast(PRODUCED_GARDENERS_CHANNEL, gardeners - 1);
            if(production_gardener){
              int numProductionGardeners = rc.readBroadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL);
              rc.broadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL, numProductionGardeners - 1);
            }
            //rc.disintegrate();
          }
        }
        RobotUtils.donateEverythingAtTheEnd();
        RobotUtils.shakeNearbyTrees();
        Clock.yield();
      }
    } catch (Exception e) {
      e.printStackTrace();
      Clock.yield();
    }
  }
}