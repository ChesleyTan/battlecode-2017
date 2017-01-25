package v3_gardeners;

import battlecode.common.*;
import utils.BroadcastUtils;
import utils.Globals;
import utils.MathUtils;
import utils.RobotUtils;

public class Gardener extends Globals {

  private static float detectRadius = 3f;
  private static boolean shouldPlant = false;
  private static int numCheckSpaces = 0;
  private static Direction startDirection = null;
  private static boolean production_gardener = false;
  private static boolean hasReportedDeath = false;
  private static MapLocation queuedMove = null;
  private static boolean spawnedEarlySoldier = false;
  private static boolean spawnedEarlyScout = false;
  private static boolean spawnedLumberjack = false;
  private static boolean reportedTrees = false;
  private static boolean withinArchonRange = false;
  private static final boolean GARDENER_DEBUG = true;
  private static Direction[] treeDirections;
  private static final Direction UP = new Direction((float) Math.PI / 2f);
  private static final Direction DOWN = new Direction((float) Math.PI * 3f / 2f);
  private static final Direction RIGHT = new Direction(0);
  private static final Direction LEFT = new Direction((float) Math.PI);
  private static final boolean CLOCKWISE = false;
  private static final boolean COUNTERCLOCKWISE = true;
  private static boolean rotateDirection = CLOCKWISE;
  private static TreeInfo lastPivot = null;

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
    if (GARDENER_DEBUG) {
      System.out.println("Calling checkspace()");
    }
    if (rc.hasMoved()) {
      return;
    }
    float sumX = 0;
    float sumY = 0;

    // Opposing forces created by Robots
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(5f);
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
      if (r.getType() == RobotType.GARDENER) {
        theirDistance = theirDistance * 2;
      }
      if (GARDENER_DEBUG) {
        System.out.println(r);
        System.out.println(theirDistance);
      }
      sumX += theirDirection.getDeltaX(theirDistance);
      sumY += theirDirection.getDeltaY(theirDistance);
    }

    // Opposing forces created by Trees
    TreeInfo[] nearbyTrees = rc.senseNearbyTrees(5f);
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
    RobotUtils.tryMove(finalDir, 5, 3);
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
      Direction d = scoutOppDir(nearbyEnemies, nearbyTrees);
      if (d != null) {
        shouldPlant = false;
        numCheckSpaces = 0;
        //MapLocation openLoc = here.add(possibleTrees()[0]);
        //System.out.println(openLoc);
        BulletInfo[] bullets = rc.senseNearbyBullets(EvasiveGardener.BULLET_DETECT_RADIUS);
        if (!rc.hasMoved()) {
          EvasiveGardener.move(bullets, nearbyEnemies, nearbyTrees);
          here = rc.getLocation();
        }
        /*
        if (!rc.hasMoved() && !RobotUtils.tryMove(d, 5, 18)) {
          MapLocation openLoc = here.add(possibleTrees()[0]);
          if (rc.canMove(openLoc)) {
            rc.move(openLoc);
            here = rc.getLocation();
          }
        }
        */
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
          if (!shouldPlant) {
            boolean clearSpace = !rc.isCircleOccupiedExceptByThisRobot(here, detectRadius);
            if (GARDENER_DEBUG) {
              System.out.println("Clear space: " + clearSpace);
            }
            if (!clearSpace || !rc.onTheMap(here, detectRadius)) {
              checkspace();
              ++numCheckSpaces;
            }
            else if (clearSpace) {
              shouldPlant = true;
            }
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

  private static MapLocation[] pivotTo(TreeInfo pivotTree, TreeInfo targetTree) {
    MapLocation pivotLoc = pivotTree.getLocation();
    MapLocation targetLoc = targetTree.getLocation();
    Direction currentAngle = pivotLoc.directionTo(here);
    Direction pivotClockwise = currentAngle.rotateRightDegrees(15 + 10 * rand.nextFloat());
    Direction pivotCounterClockwise = currentAngle.rotateLeftDegrees(15 + 10 * rand.nextFloat());
    MapLocation pivotClockwiseLoc = pivotLoc.add(pivotClockwise,
        GameConstants.BULLET_TREE_RADIUS + RobotType.GARDENER.bodyRadius + 0.99f);
    MapLocation pivotCounterClockwiseLoc = pivotLoc.add(pivotCounterClockwise,
        GameConstants.BULLET_TREE_RADIUS + RobotType.GARDENER.bodyRadius + 0.99f);
    if (pivotTree.equals(targetTree)) {
      if (rotateDirection == CLOCKWISE) {
        return new MapLocation[] { pivotClockwiseLoc, pivotCounterClockwiseLoc };
      }
      else {
        return new MapLocation[] { pivotCounterClockwiseLoc, pivotClockwiseLoc };
      }
    }
    else {
      float pivotClockwiseDist = pivotClockwiseLoc.distanceTo(targetLoc);
      float pivotCounterClockwiseDist = pivotCounterClockwiseLoc.distanceTo(targetLoc);
      if (pivotClockwiseDist < pivotCounterClockwiseDist) {
        return new MapLocation[] { pivotClockwiseLoc, pivotCounterClockwiseLoc };
      }
      else {
        return new MapLocation[] { pivotCounterClockwiseLoc, pivotClockwiseLoc };
      }
    }
  }

  public static void loop() {
    try {
      EvasiveGardener.init();
      startDirection = RobotUtils.randomDirection();
      treeDirections = new Direction[6];
      treeDirections[0] = new Direction(0);
      treeDirections[1] = new Direction((float) Math.PI / 4f);
      treeDirections[2] = new Direction((float) Math.PI * 3f / 4f);
      treeDirections[3] = new Direction((float) Math.PI);
      treeDirections[4] = new Direction((float) Math.PI * 5f / 4f);
      treeDirections[5] = new Direction((float) Math.PI * 7f / 4f);
      int productionGardeners = rc.readBroadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL);
      int requiredProductionGardeners = rc.readBroadcast(PRODUCTION_GARDENERS_CHANNEL);
      if (productionGardeners < requiredProductionGardeners) {
        production_gardener = true;
        rc.broadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL, productionGardeners + 1);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    // Loop: Build trees and water them, and occasionally build scouts
    while (true) {
      try {
        Globals.update();
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(6, them);
        TreeInfo[] nearbyTrees = rc.senseNearbyTrees(-1, us);
        if (!production_gardener) {
          // Plant a tree if no nearby trees
          if (nearbyTrees.length == 0) {
            boolean plantedTree = false;
            for (Direction plantDir : treeDirections) {
              if (rc.canPlantTree(plantDir)) {
                rc.plantTree(plantDir);
                plantedTree = true;
                break;
              }
            }
            if (!plantedTree) {
              checkspace();
            }
          }
          else {
            TreeInfo[] nearbyNeutralTrees = rc.senseNearbyTrees(6, Team.NEUTRAL);
            if (nearbyNeutralTrees.length > 2 && !spawnedLumberjack) {
              if (spawnRobot(RobotType.LUMBERJACK)) {
                BroadcastUtils.addRegionDirective(7, 1, here, 6);
                spawnedLumberjack = true;
              }
            }
            TreeInfo nearestTree = nearbyTrees[0];
            TreeInfo minWaterableTree = null;
            TreeInfo minHealthTree = null;
            float minWaterableTreeHealth = 9999f;
            float minHealthTreeHealth = 9999f;
            for (TreeInfo ti : nearbyTrees) {
              float health = ti.getHealth();
              if (health < minWaterableTreeHealth && rc.canWater(ti.getID())) {
                minWaterableTree = ti;
                minWaterableTreeHealth = health;
              }
              if (health < minHealthTreeHealth) {
                minHealthTree = ti;
                minHealthTreeHealth = health;
              }
            }
            if (minWaterableTree != null) {
              rc.water(minWaterableTree.getID());
            }
            int treesPlanted = rc.getTreeCount();
            int numGardeners = Math.max(1, rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL)
                - rc.readBroadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL));
            if (treesPlanted / numGardeners < 4) {
              Direction nearestTreeDir = nearestTree.getLocation().directionTo(here);
              if (MathUtils.isNear(nearestTreeDir, treeDirections[0], 10)) {
                float theta = nearestTreeDir.radiansBetween(RIGHT);
                Direction plantDir = new Direction((float) Math.PI - theta);
                if (rc.canPlantTree(plantDir)) {
                  MapLocation exactLoc = nearestTree.getLocation().add(treeDirections[0],
                      GameConstants.BULLET_TREE_RADIUS + RobotType.GARDENER.bodyRadius + 1.5f);
                  if (!rc.hasMoved() && rc.canMove(exactLoc)) {
                    queuedMove = here;
                    rc.move(exactLoc);
                    if (rc.canPlantTree(RIGHT)) {
                      if (GARDENER_DEBUG) {
                        System.out.println("Planting right column tree");
                      }
                      rc.plantTree(plantDir);
                    }
                  }
                }
              }
              else if (MathUtils.isNear(nearestTreeDir, treeDirections[3], 10)) {
                float theta = nearestTreeDir.radiansBetween(LEFT);
                Direction plantDir = new Direction(theta);
                if (rc.canPlantTree(plantDir)) {
                  MapLocation exactLoc = nearestTree.getLocation().add(treeDirections[3],
                      GameConstants.BULLET_TREE_RADIUS + RobotType.GARDENER.bodyRadius + 1.5f);
                  if (!rc.hasMoved() && rc.canMove(exactLoc)) {
                    queuedMove = here;
                    rc.move(exactLoc);
                    if (rc.canPlantTree(LEFT)) {
                      if (GARDENER_DEBUG) {
                        System.out.println("Planting left column tree");
                      }
                      rc.plantTree(LEFT);
                    }
                  }
                }
              }
              else if (MathUtils.isNear(nearestTreeDir, treeDirections[1], 10)) {
                if (rc.canPlantTree(UP)) {
                  MapLocation exactLoc = nearestTree.getLocation().add(treeDirections[1],
                      GameConstants.BULLET_TREE_RADIUS + RobotType.GARDENER.bodyRadius + 1.5f);
                  if (!rc.hasMoved() && rc.canMove(exactLoc)) {
                    queuedMove = here;
                    rc.move(exactLoc);
                    if (rc.canPlantTree(UP)) {
                      if (GARDENER_DEBUG) {
                        System.out.println("Planting up row tree");
                      }
                      rc.plantTree(UP);
                    }
                  }
                }
              }
              else if (MathUtils.isNear(nearestTreeDir, treeDirections[2], 10)) {
                if (rc.canPlantTree(UP)) {
                  MapLocation exactLoc = nearestTree.getLocation().add(treeDirections[2],
                      GameConstants.BULLET_TREE_RADIUS + RobotType.GARDENER.bodyRadius + 1.5f);
                  if (!rc.hasMoved() && rc.canMove(exactLoc)) {
                    queuedMove = here;
                    rc.move(exactLoc);
                    if (rc.canPlantTree(UP)) {
                      if (GARDENER_DEBUG) {
                        System.out.println("Planting up row tree");
                      }
                      rc.plantTree(UP);
                    }
                  }
                }
              }
              else if (MathUtils.isNear(nearestTreeDir, treeDirections[4], 10)) {
                if (rc.canPlantTree(DOWN)) {
                  MapLocation exactLoc = nearestTree.getLocation().add(treeDirections[4],
                      GameConstants.BULLET_TREE_RADIUS + RobotType.GARDENER.bodyRadius + 1.5f);
                  if (!rc.hasMoved() && rc.canMove(exactLoc)) {
                    queuedMove = here;
                    rc.move(exactLoc);
                    if (rc.canPlantTree(DOWN)) {
                      if (GARDENER_DEBUG) {
                        System.out.println("Planting down row tree");
                      }
                      rc.plantTree(DOWN);
                    }
                  }
                }
              }
              else if (MathUtils.isNear(nearestTreeDir, treeDirections[5], 10)) {
                if (rc.canPlantTree(DOWN)) {
                  MapLocation exactLoc = nearestTree.getLocation().add(treeDirections[5],
                      GameConstants.BULLET_TREE_RADIUS + RobotType.GARDENER.bodyRadius + 1.5f);
                  if (!rc.hasMoved() && rc.canMove(exactLoc)) {
                    queuedMove = here;
                    rc.move(exactLoc);
                    if (rc.canPlantTree(DOWN)) {
                      if (GARDENER_DEBUG) {
                        System.out.println("Planting down row tree");
                      }
                      rc.plantTree(DOWN);
                    }
                  }
                }
              }
            }
            if (!rc.hasMoved()) {
              MapLocation[] nextLocs;
              if (minHealthTreeHealth < 30) {
                TreeInfo treeClosestToTarget = null;
                float treeClosestToTargetDist = 9999f;
                MapLocation minHealthTreeLoc = minHealthTree.getLocation();
                for (TreeInfo ti : nearbyTrees) {
                  if (ti.equals(minHealthTree)) {
                    continue;
                  }
                  float dist = ti.getLocation().distanceTo(minHealthTreeLoc);
                  if (dist < treeClosestToTargetDist) {
                    treeClosestToTarget = ti;
                    treeClosestToTargetDist = dist;
                  }
                }
                if (treeClosestToTarget != null) {
                  lastPivot = treeClosestToTarget;
                  nextLocs = pivotTo(treeClosestToTarget, minHealthTree);
                }
                else {
                  lastPivot = minHealthTree;
                  nextLocs = pivotTo(minHealthTree, minHealthTree);
                }
              }
              else {
                lastPivot = nearestTree;
                nextLocs = pivotTo(nearestTree, nearestTree);
              }
              // TODO if you're closer than the closest tree, move directly
              if (rc.canMove(nextLocs[0])) {
                rc.move(nextLocs[0]);
              }
              else {
                rotateDirection = !rotateDirection;
                RobotUtils.tryMove(here.directionTo(minHealthTree.getLocation()),
                    30 + 10 * rand.nextFloat(), 3);
              }
            }
            rc.setIndicatorLine(here, lastPivot.getLocation(), 0, 0, 255);
            rc.setIndicatorLine(here, minHealthTree.getLocation(), 0, 255, 0);
          }
        }
        else {
          checkspace();
          //spawnRobot(RobotType.SOLDIER);
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
              if (Clock.getBytecodesLeft() < 1000) {
                break;
              }
              if (rc.readBroadcast(channel) != 0 && rc.readBroadcast(channel + 1) == -1) {
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
          }
          else if (!hasReportedDeath && myHP < 3 && attacker != null) {
            int gardeners = rc.readBroadcast(PRODUCED_GARDENERS_CHANNEL);
            hasReportedDeath = true;
            rc.broadcast(PRODUCED_GARDENERS_CHANNEL, gardeners - 1);
            if (production_gardener) {
              int numProductionGardeners = rc.readBroadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL);
              rc.broadcast(PRODUCED_PRODUCTION_GARDENERS_CHANNEL, numProductionGardeners - 1);
            }
          }
        }
        RobotUtils.donateEverythingAtTheEnd();
        RobotUtils.shakeNearbyTrees();
        trackEnemyGardeners();
        RobotUtils.notifyBytecodeLimitBreach();
        Clock.yield();
      } catch (Exception e) {
        e.printStackTrace();
        Clock.yield();
      }
    }
  }
}