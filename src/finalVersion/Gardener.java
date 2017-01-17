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

  public static void dodge(BulletInfo[] bullets, RobotInfo[] robots) throws GameActionException {
    float sumX = 0;
    float sumY = 0;
    for (BulletInfo i : bullets) {
      MapLocation endLocation = i.location.add(i.getDir(), i.getSpeed());
      float x0 = i.location.x;
      float y0 = i.location.y;
      float x1 = endLocation.x;
      float y1 = endLocation.y;
      float a = x1 - x0;
      float b = y0 - y1;
      if (a == 0 & b == 0){
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
        Direction away = here.directionTo(new MapLocation(x2, y2)).opposite();
        float weighted = (float) Math.pow((RobotType.SCOUT.bulletSightRadius - distance), 2)
            / RobotType.SCOUT.bulletSightRadius * RobotType.SCOUT.strideRadius;
        sumX += away.getDeltaX(weighted);
        sumY += away.getDeltaY(weighted);
      }
    }

    for (RobotInfo r : robots) {
      Direction their_direction = here.directionTo(r.location).opposite();
      System.out.println(r.ID);
      float their_distance = (float) Math
          .pow((RobotType.GARDENER.sensorRadius - here.distanceTo(r.location) + r.getRadius()), 2)
          / RobotType.GARDENER.sensorRadius * RobotType.GARDENER.strideRadius;
      rc.setIndicatorDot(here.add(their_direction,  their_distance), 255, 0, 0);
      //System.out.println(their_distance);
      if (r.getTeam() == us){
        their_distance = their_distance / 2;
      }
      sumX += their_direction.getDeltaX(their_distance);
      sumY += their_direction.getDeltaY(their_distance);
    }

    TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
    for (TreeInfo t : nearbyTrees) {
      Direction their_direction = t.location.directionTo(here);
      float their_distance = (float) Math
          .pow((RobotType.GARDENER.sensorRadius - here.distanceTo(t.location) + t.getRadius()), 2)
          / RobotType.GARDENER.sensorRadius * RobotType.GARDENER.strideRadius;
      sumX += their_direction.getDeltaX(their_distance);
      sumY += their_direction.getDeltaY(their_distance);
    }    
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
    float finaldist = (float) Math.sqrt(sumX * sumX + sumY * sumY);

    Direction finalDir = new Direction(sumX, sumY);
    RobotUtils.tryMoveDist(finalDir, finaldist, 10, 6);
  }

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
      System.out.println(r.ID);
      float their_distance = (float) Math
          .pow((RobotType.GARDENER.sensorRadius - here.distanceTo(r.location) + r.getRadius()), 2)
          / RobotType.GARDENER.sensorRadius * RobotType.GARDENER.strideRadius;
      rc.setIndicatorDot(here.add(their_direction, their_distance), 255, 0, 0);
      System.out.println(their_distance);
      if (r.getTeam() == us) {
        their_distance = their_distance / 2;
      }
      sumX += their_direction.getDeltaX(their_distance);
      sumY += their_direction.getDeltaY(their_distance);
    }

    // Opposing forces created by Trees
    TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
    for (TreeInfo t : nearbyTrees) {
      Direction their_direction = t.location.directionTo(here);
      float their_distance = (float) Math
          .pow((RobotType.GARDENER.sensorRadius - here.distanceTo(t.location) + t.getRadius()), 2)
          / RobotType.GARDENER.sensorRadius * RobotType.GARDENER.strideRadius;
      sumX += their_direction.getDeltaX(their_distance);
      sumY += their_direction.getDeltaY(their_distance);
    }

    // Opposing forces created by Edge of Map
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
  
  private static Direction scoutOppDir(){
    RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
    TreeInfo[] trees = rc.senseNearbyTrees(3, us);
    for(RobotInfo r: enemies){
      if (r.getType() != RobotType.SCOUT){
        continue;
      }
      else{
        for (TreeInfo t: trees){
          if (r.getLocation().isWithinDistance(t.getLocation(), t.getRadius())){
            return r.location.directionTo(here);
          }
        }
      }
    }
    return null;
  }
  
  private static boolean blockedByTree(BulletInfo i, TreeInfo[] trees){
    Direction base = here.directionTo(i.location);
    float baseDistance = here.distanceTo(i.location);
    for (TreeInfo tree: trees){
      if (i.location.distanceTo(tree.location) > baseDistance){
        continue;
      }
      Direction t = here.directionTo(tree.location);
      float radians = Math.abs(t.radiansBetween(base));
      float dist = (float)Math.sin(radians);
      if (dist < tree.getRadius()){
        return true;
      }
    }
    return false;
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
    } catch (Exception e) {
      e.printStackTrace();
    }
    spawnRound = currentRoundNum;
    // Loop: Build trees and water them, and occasionally build scouts
    while (true) {
      try {
        Globals.update();
        int scoutCount = rc.readBroadcast(EARLY_SCOUTS_CHANNEL);
        if (currentRoundNum < 100 && scoutCount < 3) {
          checkspace();
          if (spawnRobot(RobotType.SCOUT)) {
            rc.broadcast(EARLY_SCOUTS_CHANNEL, scoutCount + 1);
          }
        }
        else if (production_gardener) {
          checkspace();
          spawnRobot(RobotType.SCOUT);
        }
        else{
          Direction d = scoutOppDir();
          if (d != null){
            System.out.println("scouts in trees, moving");
            if (plant){
              plant = false;
            }
            System.out.println(here.add(possibleTrees()[0]));
            RobotUtils.tryMove(d, 10, 18);
          }
          else{
            BulletInfo[] bullets = rc.senseNearbyBullets();
            if (rc.getRoundNum() - spawnRound < 30 || bullets.length != 0) {
              boolean willGetHitByBullet = false;
              TreeInfo[] trees = rc.senseNearbyTrees();
              for (BulletInfo i: bullets){
                if (RobotUtils.willCollideWithMe(i) && !blockedByTree(i, trees)){
                  System.out.println("in danger");
                  willGetHitByBullet = true;
                  break;
                }
              }
              if (willGetHitByBullet){
                RobotInfo[] robots = rc.senseNearbyRobots();
                System.out.println("dodging");               
                dodge(bullets, robots);
              }
              else{
                if ((!rc.onTheMap(here, detectRadius)
                  || rc.isCircleOccupiedExceptByThisRobot(here, detectRadius)) && !plant) {
                    checkspace();
                }
              }
            }
          }
          Direction[] freeSpaces = possibleTrees();
          if (freeSpaces[1] != null && rc.canPlantTree(freeSpaces[1])) {
            rc.plantTree(freeSpaces[1]);
            plant = true;
          }
          else {
            int division_factor = (int) (154 / (rc.getTreeCount() + 1));
            if (currentRoundNum % division_factor == 0 && freeSpaces[0] != null
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
            if (production_gardener) {
              int numProductionGardeners = rc.readBroadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL);
              rc.broadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL, numProductionGardeners - 1);
            }
            //rc.disintegrate();
          }
        }
        RobotUtils.donateEverythingAtTheEnd();
        RobotUtils.shakeNearbyTrees();
        Clock.yield();
      } catch (Exception e) {
        e.printStackTrace();
        Clock.yield();
      }
    }
  }
}