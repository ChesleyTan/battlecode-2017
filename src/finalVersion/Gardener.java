package finalVersion;

import battlecode.common.*;
import utils.BroadcastUtils;
import utils.Globals;
import utils.RobotUtils;

public class Gardener extends Globals {

  private static float detectRadius = 3f;
  private static boolean shouldPlant = false;
  private static boolean clearSpace = false;
  private static int numCheckSpaces = 0;
  private static Direction startDirection = null;
  private static boolean production_gardener = false;
  private static boolean hasReportedDeath = false;
  private static MapLocation queuedMove = null;
  private static int spawnedEarlySoldier = 0;
  private static boolean spawnedEarlyScout = false;
  private static boolean spawnedLumberjack = false;
  private static boolean reportedTrees = false;
  private static boolean withinArchonRange = false;
  private static final boolean GARDENER_DEBUG = true;
  private static int producedUnits = 0;
  private static int calledForBackupRound = -9999;
  private static int soldierHardCap = 25;
  private static int tankHardCap = 8;
  private static boolean adjustedCaps = false;
  private static int plantedTrees = 0;


  /*
  public static void dodge(BulletInfo[] bullets, RobotInfo[] robots) throws GameActionException {
    float sumX = 0;
    float sumY = 0;
    for (BulletInfo i : bullets) {
      if (Clock.getBytecodesLeft() < 3000) {
        break;
      }
      int startBytecodes = Clock.getBytecodeNum();
      MapLocation endLocation = i.getLocation().add(i.getDir(), i.getSpeed());
      float x0 = i.location.x;
      float y0 = i.location.y;
      float x1 = endLocation.x;
      float y1 = endLocation.y;
      float a = y0 - y1;
      float b = x1 - y0;
      if (a == 0 && b == 0) {
        a = 0.01f;
      }
      float c = x0 * y1 - y0 * x1;
      float distance = (float) (Math.abs(a * here.x + b * here.y + c)
          / Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2)));
      if (distance < 2) {
        float x2 = (float) ((b * (b * here.x - a * here.y) - a * c)
            / (Math.pow(a, 2) + Math.pow(b, 2)));
        float y2 = (float) ((a * (a * here.y - b * here.x) - b * c)
            / (Math.pow(a, 2) + Math.pow(b, 2)));
        Direction away = new MapLocation(x2, y2).directionTo(here);
        float weighted = (float) Math.pow((RobotType.GARDENER.bulletSightRadius - distance), 2)
            / RobotType.GARDENER.bulletSightRadius * RobotType.GARDENER.strideRadius;
        sumX += away.getDeltaX(weighted);
        sumY += away.getDeltaY(weighted);
      }
      System.out.println("Used: " + (Clock.getBytecodeNum() - startBytecodes));
    }
  
    for (RobotInfo r : robots) {
      if (Clock.getBytecodesLeft() < 3000) {
        break;
      }
      Direction their_direction = r.location.directionTo(here);
      float baseValue = (RobotType.GARDENER.sensorRadius - here.distanceTo(r.location)
          + r.getRadius())
          * (RobotType.GARDENER.sensorRadius - here.distanceTo(r.location) + r.getRadius());
      float their_distance = baseValue / RobotType.GARDENER.sensorRadius
          * RobotType.GARDENER.strideRadius;
      rc.setIndicatorDot(here.add(their_direction, their_distance), 255, 0, 0);
      //System.out.println(their_distance);
      if (r.getTeam() == us) {
        their_distance = their_distance / 2;
      }
      sumX += their_direction.getDeltaX(their_distance);
      sumY += their_direction.getDeltaY(their_distance);
    }
  
    // TODO add check to ensure NEUTRAL_TREE_MAX_RADIUS does not exceed sensorRadius
    TreeInfo[] nearbyTrees = rc.senseNearbyTrees(GameConstants.NEUTRAL_TREE_MAX_RADIUS);
    if (nearbyTrees.length <= 10) {
      for (TreeInfo t : nearbyTrees) {
        if (Clock.getBytecodesLeft() < 3000) {
          break;
        }
        Direction their_direction = t.location.directionTo(here);
        float baseValue = (RobotType.GARDENER.sensorRadius - here.distanceTo(t.location)
            + t.getRadius())
            * (RobotType.GARDENER.sensorRadius - here.distanceTo(t.location) + t.getRadius());
        float their_distance = baseValue / RobotType.GARDENER.sensorRadius
            * RobotType.GARDENER.strideRadius;
        sumX += their_direction.getDeltaX(their_distance);
        sumY += their_direction.getDeltaY(their_distance);
      }
    }
  
    if (Clock.getBytecodesLeft() >= 2000) {
      float sightRadius = RobotType.GARDENER.sensorRadius - 1;
      updateMapBoundaries();
      if (minX != UNKNOWN && !rc.onTheMap(new MapLocation(here.x - sightRadius, here.y))) {
        float distance = (here.x - minX) * (here.x - minX);
        float weightedDistance = distance / sightRadius * myType.strideRadius;
        sumX += weightedDistance;
      }
      if (maxX != UNKNOWN && !rc.onTheMap(new MapLocation(here.x + sightRadius, here.y))) {
        float distance = (maxX - here.x) * (maxX - here.x);
        float weightedDistance = distance / sightRadius * myType.strideRadius;
        sumX -= weightedDistance;
      }
      if (minY != UNKNOWN && !rc.onTheMap(new MapLocation(here.x, here.y - sightRadius))) {
        float distance = (here.y - minY) * (here.y - minY);
        float weightedDistance = distance / sightRadius * myType.strideRadius;
        sumY += weightedDistance;
      }
      if (maxY != UNKNOWN && !rc.onTheMap(new MapLocation(here.x, here.y + sightRadius))) {
        float distance = (maxY - here.y) * (maxY - here.y);
        float weightedDistance = distance / sightRadius * myType.strideRadius;
        sumY -= weightedDistance;
      }
    }
    float finaldist = (float) Math.sqrt(sumX * sumX + sumY * sumY);
  
    Direction finalDir = new Direction(sumX, sumY);
    RobotUtils.tryMoveDist(finalDir, finaldist, 10, 6);
  }
  */

  public static boolean shouldGoAhead(int soldierCount) throws GameActionException{
    if(plantedTrees >= 2){
      int producedGardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
      if (soldierCount / producedGardeners < 1 || !spawnedEarlyScout){
        return false;
      }
    }
    return true;
  }
  /*
   * Checks that there is enough space around the unit to begin planting
   */
  public static void checkspace() throws GameActionException {
    if (GARDENER_DEBUG) {
      System.out.println("Calling checkspace()");
    }
    if (rc.hasMoved()) {
      return;
    }
    float sumX = 0;
    float sumY = 0;

    // Opposing forces created by Robots
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1);
    float robotMaxDistance = RobotType.GARDENER.sensorRadius + GameConstants.MAX_ROBOT_RADIUS;
    for (RobotInfo r : nearbyRobots) {
      if (Clock.getBytecodesLeft() < 3000) {
        break;
      }
      Direction theirDirection = r.getLocation().directionTo(here);
      float theirDistance = (float) Math
          .pow(
              (robotMaxDistance
                  - (here.distanceTo(r.location) - r.getRadius() - RobotType.GARDENER.bodyRadius)),
              2);
      //rc.setIndicatorDot(here.add(theirDirection, theirDistance), 255, 51, 153);
      /*if (r.getTeam() == us) {
        theirDistance = theirDistance / 2;
      }*/
      if (r.getTeam() == us) {
        RobotType type = r.getType();
        if (type == RobotType.GARDENER || type == RobotType.ARCHON) {
          theirDistance = theirDistance * 2;
        }
      }
      if (GARDENER_DEBUG) {
        System.out.println(r);
        System.out.println(theirDistance);
      }
      sumX += theirDirection.getDeltaX(theirDistance);
      sumY += theirDirection.getDeltaY(theirDistance);
    }

    // Opposing forces created by Trees
    TreeInfo[] nearbyTrees = rc.senseNearbyTrees(5f, Team.NEUTRAL);
    float treeMaxDistance = RobotType.GARDENER.sensorRadius + GameConstants.NEUTRAL_TREE_MAX_RADIUS;
    for (TreeInfo t : nearbyTrees) {
      if (Clock.getBytecodesLeft() < 3000) {
        break;
      }
      Direction theirDirection = t.location.directionTo(here);
      float theirDistance = (float) Math.pow(
          ((treeMaxDistance
              - (here.distanceTo(t.location) - t.getRadius() - RobotType.GARDENER.bodyRadius)) / 3),
          2);
      if (GARDENER_DEBUG) {
        System.out.println(t);
        System.out.println(theirDistance);
      }
      sumX += theirDirection.getDeltaX(theirDistance);
      sumY += theirDirection.getDeltaY(theirDistance);
    }

    // Opposing forces created by Edge of Map
    updateMapBoundaries();
    float sensorRadius = RobotType.GARDENER.sensorRadius - 1;
    if (minX != UNKNOWN && !rc.onTheMap(new MapLocation(here.x - sensorRadius, here.y))) {
      float weightedDistance = 10 * (float) Math.pow(sensorRadius - (here.x - minX), 2);
      if (GARDENER_DEBUG) {
        System.out.println("minX: " + weightedDistance);
      }
      sumX += weightedDistance;
    }
    if (maxX != UNKNOWN && !rc.onTheMap(new MapLocation(here.x + sensorRadius, here.y))) {
      float weightedDistance = 10 * (float) Math.pow(sensorRadius - (maxX - here.x), 2);
      if (GARDENER_DEBUG) {
        System.out.println("maxX: " + weightedDistance);
      }
      sumX -= weightedDistance;
    }
    if (minY != UNKNOWN && !rc.onTheMap(new MapLocation(here.x, here.y - sensorRadius))) {
      float weightedDistance = 10 * (float) Math.pow(sensorRadius - (here.y - minY), 2);
      if (GARDENER_DEBUG) {
        System.out.println("minY: " + weightedDistance);
      }
      sumY += weightedDistance;
    }
    if (maxY != UNKNOWN && !rc.onTheMap(new MapLocation(here.x, here.y + sensorRadius))) {
      float weightedDistance = 10 * (float) Math.pow(sensorRadius - (maxY - here.y), 2);
      if (GARDENER_DEBUG) {
        System.out.println("maxY: " + weightedDistance);
      }
      sumY -= weightedDistance;
    }

    Direction finalDir = new Direction(sumX, sumY);
    rc.setIndicatorLine(here, here.add(finalDir, 1.75f), 255, 51, 153);
    rc.setIndicatorLine(here.add(finalDir, 1.75f), here.add(finalDir, 2f), 0, 0, 0);
    RobotUtils.tryMove(finalDir, 10 + 10 * rand.nextFloat(), 3);
  }

  private static Direction scoutOppDir(RobotInfo[] enemies, TreeInfo[] trees) {
    for (RobotInfo r : enemies) {
      if (Clock.getBytecodesLeft() < 3000) {
        return null;
      }
      if (r.getType() != RobotType.SCOUT) {
        continue;
      }
      else {
        for (TreeInfo t : trees) {
          if (Clock.getBytecodesLeft() < 3000) {
            return null;
          }
          if (r.getLocation().isWithinDistance(t.getLocation(), t.getRadius())) {
            return r.location.directionTo(here);
          }
        }
      }
    }
    return null;
  }

  private static void move(RobotInfo[] nearbyEnemies, TreeInfo[] nearbyTrees)
      throws GameActionException {
    if (production_gardener) {
      // TODO production gardener evasion
      checkspace();
    }
    else {
      BulletInfo[] bullets = rc.senseNearbyBullets(EvasiveGardener.BULLET_DETECT_RADIUS);
      boolean nearbyEnemyThreat = false;
      for (RobotInfo ri : nearbyEnemies) {
        if (Clock.getBytecodesLeft() < 8000) {
          break;
        }
        if (ri.getType().bulletSpeed > 0 && ri.getLocation().isWithinDistance(here,
            1 + RobotType.GARDENER.bodyRadius + ri.getRadius())) {
          nearbyEnemyThreat = true;
          break;
        }
        else if (ri.getType() == RobotType.LUMBERJACK && ri.getLocation().isWithinDistance(here,
            GameConstants.LUMBERJACK_STRIKE_RADIUS + RobotType.LUMBERJACK.strideRadius
                + RobotType.LUMBERJACK.bodyRadius + RobotType.GARDENER.bodyRadius)) {
          nearbyEnemyThreat = true;
          break;
        }
      }
      boolean willGetHitByBullet = false;
      if (!nearbyEnemyThreat && (bullets.length != 0)) {
        TreeInfo[] trees = rc.senseNearbyTrees();
        for (BulletInfo i : bullets) {
          //System.out.println(i);
          if (Clock.getBytecodesLeft() < 6000) {
            break;
          }
          if (RobotUtils.willCollideWithMe(i) && !blockedByTree(i, trees)) {
            if (GARDENER_DEBUG) {
              System.out.println("in danger");
            }
            willGetHitByBullet = true;
            break;
          }
        }
      }
      if (nearbyEnemyThreat || willGetHitByBullet) {
        shouldPlant = false;
        numCheckSpaces = 0;
        if (GARDENER_DEBUG) {
          System.out.println("dodging");
          System.out.println(Clock.getBytecodesLeft());
        }
        //dodge(bullets, robots);
        EvasiveGardener.move(bullets, nearbyEnemies, nearbyTrees);
        here = rc.getLocation();
      }
      else {
        if (!clearSpace && !shouldPlant) {
          clearSpace = !rc.isCircleOccupiedExceptByThisRobot(here, detectRadius);
          if (GARDENER_DEBUG) {
            System.out.println("Clear space: " + clearSpace);
          }
          if (!clearSpace) {
            checkspace();
            ++numCheckSpaces;
          }
        }
        if (!rc.hasMoved()) {
          if (!rc.onTheMap(here, detectRadius) || (!shouldPlant && !noNearbyGardeners())) {
            checkspace();
            ++numCheckSpaces;
          }
        }
      }
    }
  }

  private static boolean scoutInTree(RobotInfo[] enemies, TreeInfo tree) {
    for (RobotInfo r : enemies) {
      if (Clock.getBytecodesLeft() < 3000) {
        return true;
      }
      if (r.getType() != RobotType.SCOUT) {
        continue;
      }
      else {
        if (r.getLocation().isWithinDistance(tree.getLocation(), tree.getRadius())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean blockedByTree(BulletInfo i, TreeInfo[] trees) {
    Direction base = here.directionTo(i.location);
    float baseDistance = here.distanceTo(i.location);
    for (TreeInfo tree : trees) {
      if (Clock.getBytecodesLeft() < 3000) {
        return false;
      }
      MapLocation treeLoc = tree.getLocation();
      if (i.location.distanceTo(treeLoc) > baseDistance) {
        continue;
      }
      Direction t = here.directionTo(treeLoc);
      float radians = Math.abs(t.radiansBetween(base));
      float dist = (float) (here.distanceTo(treeLoc) * Math.sin(radians));
      if (dist < tree.getRadius()) {
        return true;
      }
    }
    return false;
  }

  private static boolean spawnRobot(RobotType t) throws GameActionException {
    if (rc.getBuildCooldownTurns() > 0) {
      return false;
    }
    Direction randomDir = RobotUtils.randomDirection();
    int attempts = 0;
    while (!rc.canBuildRobot(t, randomDir) && attempts < 36) {
      randomDir = randomDir.rotateLeftDegrees(10);
      attempts++;
    }
    if (rc.canBuildRobot(t, randomDir)) {
      rc.buildRobot(t, randomDir);
      if (t == RobotType.SOLDIER) {
        if (rc.getRoundNum() % 10 != 0) {
          int soldier_count = rc.readBroadcast(SOLDIER_PRODUCTION_CHANNEL);
          rc.broadcast(SOLDIER_PRODUCTION_CHANNEL, soldier_count + 1);
        }
        else {
          int soldier_count = rc.readBroadcast(SOLDIER_REPORT_CHANNEL);
          rc.broadcast(SOLDIER_REPORT_CHANNEL, soldier_count + 1);
        }
      }
      if (t == RobotType.LUMBERJACK) {
        if (rc.getRoundNum() % 10 != 0) {
          int lumber_count = rc.readBroadcast(LUMBERJACK_PRODUCTION_CHANNEL);
          rc.broadcast(LUMBERJACK_PRODUCTION_CHANNEL, lumber_count + 1);
        }
        else {
          int lumber_count = rc.readBroadcast(LUMBERJACK_REPORT_CHANNEL);
          rc.broadcast(LUMBERJACK_REPORT_CHANNEL, lumber_count + 1);
        }
      }
      if (t == RobotType.TANK) {
        if (rc.getRoundNum() % 10 != 0) {
          int tank_count = rc.readBroadcast(TANK_PRODUCTION_CHANNEL);
          rc.broadcast(TANK_PRODUCTION_CHANNEL, tank_count + 1);
        }
        else {
          int tank_count = rc.readBroadcast(TANK_REPORT_CHANNEL);
          rc.broadcast(TANK_REPORT_CHANNEL, tank_count + 1);
        }
      }
      return true;
    }
    return false;
  }

  private static Direction[] possibleTrees() throws GameActionException {
    Direction[] freeSpaces = new Direction[6];
    int index = 0;
    int rotateAmt = 0;
    while (rotateAmt < 6) {
      if (rc.canPlantTree(startDirection)) {
        freeSpaces[index] = startDirection;
        index++;
      }
      startDirection = startDirection.rotateLeftDegrees(60);
      rotateAmt++;
    }
    return freeSpaces;
  }

  private static boolean noNearbyGardeners() {
    RobotInfo[] nearbyFriendlies = rc.senseNearbyRobots(-1, us);
    for (RobotInfo ri : nearbyFriendlies) {
      if (ri.getType() == RobotType.GARDENER) {
        return false;
      }
    }
    return true;
  }

  public static void loop() {

    try {
      EvasiveGardener.init();
      startDirection = RobotUtils.randomDirection();
      int productionGardeners = rc.readBroadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL);
      int requiredProductionGardeners = rc.readBroadcast(PRODUCTION_GARDENERS_CHANNEL);
      if (productionGardeners < requiredProductionGardeners) {
        production_gardener = true;
        rc.broadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL, productionGardeners + 1);
      }
      if (rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL) != 1 || currentRoundNum > 100) {
        spawnedEarlySoldier = 3;
        spawnedEarlyScout = true;
        if (rc.readBroadcast(EARLY_UNITS_CHANNEL) <= 2){
          rc.broadcast(EARLY_UNITS_CHANNEL, 4);
        }
      }
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    // Loop: Build trees and water them, and occasionally build scouts
    while (true) {
      try {
        Globals.update();
        if (GARDENER_DEBUG) {
          System.out.println("Soldier count: " + rc.readBroadcast(SOLDIER_PRODUCTION_CHANNEL));
          System.out.println("Soldier hard cap: " + soldierHardCap);
        }
        int unitCount = rc.readBroadcast(EARLY_UNITS_CHANNEL);
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(6, them);
        TreeInfo[] nearbyTrees = rc.senseNearbyTrees(3, us);
        if (queuedMove != null) {
          if (!rc.hasMoved() && rc.canMove(queuedMove)) {
            rc.move(queuedMove);
            here = rc.getLocation();
          }
          queuedMove = null;
        }
        if (!rc.hasMoved()) {
          if (GARDENER_DEBUG) {
            System.out.println("Before: " + Clock.getBytecodesLeft());
          }
          move(nearbyEnemies, nearbyTrees);
          if (GARDENER_DEBUG) {
            System.out.println("After: " + Clock.getBytecodesLeft());
          }
        }
        // Either plant a tree or produce a unit
        // Initial setup moves to a clear spot and spawns 3 scouts
        int requiredEarlySoldiers = rc.readBroadcast(ARCHON_DISTANCE_CHANNEL) < 30? 1 : 2;
        System.out.println("spawned early soldier" + spawnedEarlySoldier);
        System.out.println("spawned early scout" + spawnedEarlyScout);
        if (spawnedEarlySoldier < requiredEarlySoldiers || !spawnedEarlyScout) {
          if (rc.senseNearbyTrees(6, Team.NEUTRAL).length > 2 && !spawnedLumberjack) {
            if (spawnRobot(RobotType.LUMBERJACK) && unitCount < 3) {
              BroadcastUtils.addRegionDirective(7, 1, here, 6);
              rc.broadcast(EARLY_UNITS_CHANNEL, ++unitCount);
              spawnedLumberjack = true;
            }
          }
          else if (spawnedEarlySoldier < requiredEarlySoldiers && unitCount < 2) {
            if (spawnRobot(RobotType.SOLDIER)) {
              rc.broadcast(EARLY_UNITS_CHANNEL, ++unitCount);
              spawnedEarlySoldier ++;
            }
          }
          else if (!spawnedEarlyScout && unitCount < 3) {
            if (spawnRobot(RobotType.SCOUT)) {
              rc.broadcast(EARLY_UNITS_CHANNEL, unitCount);
              spawnedEarlyScout = true;
            }
          }
        }
        withinArchonRange = false;
        if (!spawnedLumberjack) {
          RobotInfo[] friendlies = rc.senseNearbyRobots(3, us);
          for (RobotInfo r : friendlies) {
            if (r.getType() == RobotType.ARCHON) {
              withinArchonRange = true;
              break;
            }
          }
        }
        int soldierCount = rc.readBroadcast(SOLDIER_PRODUCTION_CHANNEL);
        if (soldierCount == 0) {
          spawnRobot(RobotType.SOLDIER);
        }
        else if (!spawnedLumberjack && withinArchonRange) {
          if (spawnRobot(RobotType.LUMBERJACK)) {
            spawnedLumberjack = true;
          }
        }
        if (production_gardener) {
          int tankCount = rc.readBroadcast(TANK_PRODUCTION_CHANNEL);
          boolean spawnedTank = false;
          if (tankCount < tankHardCap) {
            spawnedTank = spawnRobot(RobotType.TANK);
          }
          if (soldierCount < soldierHardCap && !spawnedTank && soldierCount < rc.getTreeCount() * 2) {
            spawnRobot(RobotType.SOLDIER);
          }
        }
        else {
          if ((!reportedTrees || !spawnedLumberjack)
              && rc.senseNearbyTrees(6, Team.NEUTRAL).length > 2) {
            if (!reportedTrees) {
              BroadcastUtils.addRegionDirective(7, 1, here, 6);
              reportedTrees = true;
            }
            if (!spawnedLumberjack) {
              if (spawnRobot(RobotType.LUMBERJACK)) {
                spawnedLumberjack = true;
              }
            }
          }
          Direction[] freeSpaces = possibleTrees();
          if (GARDENER_DEBUG) {
            System.out.println("shouldPlant: " + shouldPlant);
            System.out.println("clearSpace: " + clearSpace);
            System.out.println("numCheckspaces: " + numCheckSpaces);
            System.out.println("unitCount: " + unitCount);
          }
          if (((unitCount >= 2)
              && shouldGoAhead(soldierCount)
              && (shouldPlant || clearSpace || numCheckSpaces > 25)
              && (shouldPlant || noNearbyGardeners())) 
              && freeSpaces[1] != null
              && rc.canPlantTree(freeSpaces[1])) {
            /*
            if (!rc.hasMoved() && rc.canMove(freeSpaces[1], 0.3f)) {
              rc.move(freeSpaces[1], 0.3f);
              queuedMove = here;
            }
            else {
              queuedMove = here.add(freeSpaces[1].opposite(), 0.3f);
            }
            */
            rc.plantTree(freeSpaces[1]);
            shouldPlant = true;
            plantedTrees ++;
          }
          else {
            int division_factor = (int) (154 / (rc.getTreeCount() + 1));
            if (currentRoundNum % division_factor == 0) {
              if (freeSpaces[0] != null && rc.canBuildRobot(RobotType.LUMBERJACK, freeSpaces[0])) {
                int lumberCount = rc.readBroadcast(LUMBERJACK_PRODUCTION_CHANNEL);
                if (producedUnits % 10 == 2 && lumberCount < 15) {
                  rc.buildRobot(RobotType.LUMBERJACK, freeSpaces[0]);
                  if (rc.getRoundNum() % 10 != 0){
                    int lumberjack_count = rc.readBroadcast(LUMBERJACK_PRODUCTION_CHANNEL);
                    rc.broadcast(LUMBERJACK_PRODUCTION_CHANNEL, lumberjack_count + 1);
                  }
                  else{
                    int lumberjack_count = rc.readBroadcast(LUMBERJACK_REPORT_CHANNEL);
                    rc.broadcast(LUMBERJACK_REPORT_CHANNEL, lumberjack_count + 1);
                  }
                  producedUnits++;
                }
                else {
                  if (soldierCount < soldierHardCap && soldierCount < rc.getTreeCount() * 2) {
                    rc.buildRobot(RobotType.SOLDIER, freeSpaces[0]);
                    if (rc.getRoundNum() % 10 != 0){
                      rc.broadcast(SOLDIER_PRODUCTION_CHANNEL, soldierCount + 1);
                    }
                    else{
                      soldierCount = rc.readBroadcast(SOLDIER_REPORT_CHANNEL);
                      rc.broadcast(SOLDIER_REPORT_CHANNEL, soldierCount + 1);
                    }
                  }
                  producedUnits++;
                }
              }
              else {
                if (soldierCount < soldierHardCap && soldierCount < rc.getTreeCount() * 2) {
                  spawnRobot(RobotType.SOLDIER);
                  producedUnits++;
                }
              }
            }
          }
        }
        // Water nearby trees
        if (nearbyTrees.length != 0) {
          TreeInfo minWaterable = null;
          float minWaterableHp = 9999f;
          for (TreeInfo x : nearbyTrees) {
            if (rc.canWater(x.getID()) && x.getHealth() < minWaterableHp
                && !scoutInTree(nearbyEnemies, x)) {
              minWaterable = x;
              minWaterableHp = x.getHealth();
            }
          }
          if (minWaterable != null && rc.canWater(minWaterable.getID())) {
            rc.water(minWaterable.getID());
          }
        }
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
          if (currentRoundNum - calledForBackupRound > 100
              && myHP > RobotType.GARDENER.maxHealth / 2) {
            rc.setIndicatorDot(here, 255, 255, 255);
            boolean calledForBackup = false;
            for (int channel = DEFENSE_START_CHANNEL; channel < DEFENSE_END_CHANNEL; channel += DEFENSE_BLOCK_WIDTH) {
              if (Clock.getBytecodesLeft() < 1000) {
                break;
              }
              if (rc.readBroadcast(channel) != 0 && rc.readBroadcast(channel + 1) == -1) {
                rc.broadcast(channel + 1, attacker.getID());
                rc.broadcast(channel + 2, (int) attacker.getLocation().x);
                rc.broadcast(channel + 3, (int) attacker.getLocation().y);
                calledForBackup = true;
                calledForBackupRound = currentRoundNum;
                break;
              }
            }
            if (!calledForBackup) {
              rc.broadcast(DEFENSE_START_CHANNEL + 1, attacker.getID());
              rc.broadcast(DEFENSE_START_CHANNEL + 2, (int) attacker.getLocation().x);
              rc.broadcast(DEFENSE_START_CHANNEL + 3, (int) attacker.getLocation().y);
              calledForBackup = true;
              calledForBackupRound = currentRoundNum;
            }
          }
          else if (!hasReportedDeath && myHP < 5) {
            int gardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
            hasReportedDeath = true;
            rc.broadcast(PRODUCED_GARDENERS_CHANNEL, gardeners - 1);
            if (production_gardener) {
              int numProductionGardeners = rc.readBroadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL);
              rc.broadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL, numProductionGardeners - 1);
            }
          }
        }
        if (currentRoundNum % 10 == 0) {
          report();
        }
        RobotUtils.donateEverythingAtTheEnd();
        RobotUtils.shakeNearbyTrees();
        trackEnemyGardeners();
        RobotUtils.notifyBytecodeLimitBreach();
        if (!adjustedCaps) {
          updateMapBoundaries();
          if (minX != UNKNOWN && maxX != UNKNOWN && minY != UNKNOWN && maxY != UNKNOWN) {
            float diagonal = (float) Math
                .sqrt((maxX - minX) * (maxX - minX) + (maxY - minY) * (maxY - minY));
            soldierHardCap = (int) (diagonal / 2 * 0.8);
            tankHardCap = (int) soldierHardCap / 4;
            adjustedCaps = true;
          }
        }
        Clock.yield();
      } catch (Exception e) {
        e.printStackTrace();
        Clock.yield();
      }
    }
  }
}