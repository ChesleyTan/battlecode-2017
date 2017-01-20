package finalVersion;

import battlecode.common.*;
import utils.Globals;
import utils.RobotUtils;

public class Gardener extends Globals {

  private static float detectRadius = 3f;
  private static boolean shouldPlant = false;
  private static int numCheckSpaces = 0;
  private static int spawnRound;
  private static Direction startDirection = null;
  private static boolean production_gardener = false;
  private static boolean hasReportedDeath = false;
  private static MapLocation queuedMove = null;

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

  /*
   * Checks that there is enough space around the unit to begin planting
   */
  public static void checkspace() throws GameActionException {
    System.out.println("Calling checkspace()");
    if (rc.hasMoved()) {
      return;
    }
    float sumX = 0;
    float sumY = 0;

    // Opposing forces created by Robots
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots();
    for (RobotInfo r : nearbyRobots) {
      Direction their_direction = r.getLocation().directionTo(here);
      float their_distance = (float) Math
          .pow((RobotType.GARDENER.sensorRadius - here.distanceTo(r.location) + r.getRadius()), 2)
          / RobotType.GARDENER.sensorRadius * RobotType.GARDENER.strideRadius;
      rc.setIndicatorDot(here.add(their_direction, their_distance), 255, 51, 153);
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
    rc.setIndicatorLine(here, here.add(finalDir, 1.75f), 255, 51, 153);
    rc.setIndicatorLine(here.add(finalDir, 1.75f), here.add(finalDir, 2f), 0, 0, 0);
    RobotUtils.tryMove(finalDir, 5, 3);
  }

  private static Direction scoutOppDir(RobotInfo[] enemies, TreeInfo[] trees) {
    for (RobotInfo r : enemies) {
      if (r.getType() != RobotType.SCOUT) {
        continue;
      }
      else {
        for (TreeInfo t : trees) {
          if (r.getLocation().isWithinDistance(t.getLocation(), t.getRadius())) {
            return r.location.directionTo(here);
          }
        }
      }
    }
    return null;
  }

  private static boolean scoutInTree(RobotInfo[] enemies, TreeInfo tree) {
    for (RobotInfo r : enemies) {
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
    Direction randomDir = RobotUtils.randomDirection();
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

    try {
      EvasiveGardener.init();
      startDirection = RobotUtils.randomDirection();
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
        int unitCount = rc.readBroadcast(EARLY_UNITS_CHANNEL);
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(5, them);
        TreeInfo[] nearbyTrees = rc.senseNearbyTrees(3, us);
        if (queuedMove != null) {
          if (!rc.hasMoved() && rc.canMove(queuedMove)) {
            rc.move(queuedMove);
          }
          queuedMove = null;
        }
        if (!rc.hasMoved()) {
          if (currentRoundNum < 100 && unitCount < 3) {
            ++numCheckSpaces;
            checkspace();
          }
          else if (production_gardener) {
            checkspace();
          }
          else {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
            Direction d = scoutOppDir(enemies, nearbyTrees);
            if (d != null) {
              System.out.println("scouts in trees, moving");
              shouldPlant = false;
              numCheckSpaces = 0;
              //MapLocation openLoc = here.add(possibleTrees()[0]);
              //System.out.println(openLoc);
              if (!rc.hasMoved() && !RobotUtils.tryMove(d, 5, 18)) {
                MapLocation openLoc = here.add(possibleTrees()[0]);
                if (rc.canMove(openLoc)) {
                  rc.move(openLoc);
                }
              }
            }
            else {
              BulletInfo[] bullets = rc.senseNearbyBullets(EvasiveGardener.BULLET_DETECT_RADIUS);
              RobotInfo[] robots = rc.senseNearbyRobots(EvasiveGardener.ENEMY_DETECT_RADIUS, them);
              boolean nearbyShooter = false;
              for (RobotInfo ri : robots) {
                if (ri.getType().bulletSpeed > 0 && ri.getLocation().isWithinDistance(here,
                    1 + myType.bodyRadius + ri.getRadius())) {
                  nearbyShooter = true;
                }
              }
              boolean willGetHitByBullet = false;
              if (!nearbyShooter && (bullets.length != 0)) {
                TreeInfo[] trees = rc.senseNearbyTrees();
                for (BulletInfo i : bullets) {
                  System.out.println(i);
                  if (Clock.getBytecodesLeft() < 2000) {
                    break;
                  }
                  if (RobotUtils.willCollideWithMe(i) && !blockedByTree(i, trees)) {
                    System.out.println("in danger");
                    willGetHitByBullet = true;
                    break;
                  }
                }
              }
              if (nearbyShooter || willGetHitByBullet) {
                shouldPlant = false;
                numCheckSpaces = 0;
                System.out.println("dodging");
                //dodge(bullets, robots);
                EvasiveGardener.move(bullets, robots,
                    rc.senseNearbyTrees(EvasiveGardener.TREE_DETECT_RADIUS));
              }
              else {
                boolean clearSpace = rc.isCircleOccupiedExceptByThisRobot(here, detectRadius);
                if ((!clearSpace || !rc.onTheMap(here, detectRadius)) && !shouldPlant) {
                  checkspace();
                  ++numCheckSpaces;
                }
                else if (clearSpace) {
                  shouldPlant = true;
                }
              }
            }
          }
          // Either plant a tree or produce a unit
          // Initial setup moves to a clear spot and spawns 3 scouts
          if (unitCount < 3) {
            if (unitCount == 2) {
              if (spawnRobot(RobotType.SOLDIER)) {
                rc.broadcast(EARLY_UNITS_CHANNEL, 3);
              }
            }
            else if (spawnRobot(RobotType.SCOUT)) {
              rc.broadcast(EARLY_UNITS_CHANNEL, unitCount + 1);
            }
          }
          else if (production_gardener) {
            spawnRobot(RobotType.SCOUT);
          }
          else {
            Direction[] freeSpaces = possibleTrees();
            if ((shouldPlant || numCheckSpaces > 3) && freeSpaces[1] != null && rc.canPlantTree(freeSpaces[1])) {
              /*
              if (!rc.hasMoved() && rc.canMove(freeSpaces[1], 0.3f)) {
                rc.move(freeSpaces[1], 0.3f);
                queuedMove = here;
              }
              else {
                queuedMove = here.add(freeSpaces[1].opposite(), 0.3f);
              }
              */
              if (rc.canPlantTree(freeSpaces[1])) {
                rc.plantTree(freeSpaces[1]);
                shouldPlant = true;
              }
            }
            else {
              int division_factor = (int) (154 / (rc.getTreeCount() + 1));
              if (currentRoundNum % division_factor == 0 && freeSpaces[0] != null
                  && rc.canBuildRobot(RobotType.LUMBERJACK, freeSpaces[0])) {
                rc.buildRobot(RobotType.LUMBERJACK, freeSpaces[0]);
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
          if (myHP > RobotType.GARDENER.maxHealth / 2) {
            rc.setIndicatorDot(here, 255, 255, 255);
            boolean calledForBackup = false;
            for (int channel = DEFENSE_START_CHANNEL; channel < DEFENSE_END_CHANNEL; channel += DEFENSE_BLOCK_WIDTH) {
              if (rc.readBroadcast(channel) != 0 && rc.readBroadcast(channel + 1) == 0) {
                rc.broadcast(channel + 1, attacker.getID());
                rc.broadcast(channel + 2, (int) attacker.getLocation().x);
                rc.broadcast(channel + 3, (int) attacker.getLocation().y);
                calledForBackup = true;
                break;
              }
            }
            if (!calledForBackup) {
              rc.broadcast(DEFENSE_START_CHANNEL + 1, attacker.getID());
              rc.broadcast(DEFENSE_START_CHANNEL + 2, (int) attacker.getLocation().x);
              rc.broadcast(DEFENSE_START_CHANNEL + 3, (int) attacker.getLocation().y);
              calledForBackup = true;
            }
            // TODO call for scouts if no soldiers
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
        trackEnemyGardeners();
        Clock.yield();
      } catch (Exception e) {
        e.printStackTrace();
        Clock.yield();
      }
    }
  }
}