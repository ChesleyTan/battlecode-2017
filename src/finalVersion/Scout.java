package finalVersion;

import battlecode.common.*;
import utils.Globals;
import utils.MathUtils;
import utils.RobotUtils;

import static utils.TargetingUtils.clearShot;

public class Scout extends Globals {

  private static final int ROAM = 0;
  private static final int ATTACK = 1;
  private static final int DISCOVER = 2;
  private static int current_mode = DISCOVER;
  private static final float KEEPAWAY_RADIUS = EvasiveScout.ENEMY_DETECT_RADIUS;
  private static final float GARDENER_KEEPAWAY_RADIUS = 2.00001f;
  private static Direction[] GARDENER_PENETRATION_ANGLES = new Direction[6];
  private static Direction targetDirection = null;
  private static int squad_channel = -1;
  private static int attackTarget;
  private static boolean hasReportedDeath = false;
  private static boolean priorityTarget = false;
  private static boolean engagingTarget = false;
  private static int roundsEngaging = 0;
  private static boolean isPerchedInTree = false;
  private static float hpWhenPerched = 0f;
  private static int targetBlacklist = -1;
  private static int targetBlacklistPeriodStart = -1;
  private static boolean gardenerOnlyMode = false;
  private static MapLocation[] enemyArchons;

  /*
  public static boolean isPerchedInTree() {
    TreeInfo[] nearbyTrees = rc.senseNearbyTrees(0.1f);
    for (TreeInfo ti : nearbyTrees) {
      if (ti.team.isPlayer()) {
        return true;
      }
      else if (ti.radius <= 1f) {
        return true;
      }
    }
    return false;
  }
  */

  public static boolean shouldUnperch(RobotInfo[] nearbyEnemies) {
    if (!isPerchedInTree) {
      return false;
    }
    for (RobotInfo ri : nearbyEnemies) {
      if (Clock.getBytecodesLeft() < 2000) {
        return true;
      }
      else {
        switch (ri.getType()) {
          case LUMBERJACK:
            if (ri.getLocation().isWithinDistance(here,
                1 + RobotType.LUMBERJACK.bodyRadius + RobotType.SCOUT.bodyRadius)) {
              return true;
            }
            break;
          case SCOUT:
            if (ri.getLocation().isWithinDistance(here,
                1 + RobotType.SCOUT.bodyRadius + RobotType.SCOUT.bodyRadius)) {
              return true;
            }
            break;
          case SOLDIER:
            if (ri.getLocation().isWithinDistance(here,
                4 + RobotType.SOLDIER.bodyRadius + RobotType.SCOUT.bodyRadius)) {
              return true;
            }
            break;
          case TANK:
            if (ri.getLocation().isWithinDistance(here,
                6 + RobotType.TANK.bodyRadius + RobotType.SCOUT.bodyRadius)) {
              return true;
            }
            break;
          default:
            continue;
        }
      }
    }
    return false;
  }

  public static void findSquad() throws GameActionException {
    int numGardenerOnly = rc.readBroadcast(ATTACK_END_CHANNEL - ATTACK_BLOCK_WIDTH);
    if (numGardenerOnly == 0) {
      gardenerOnlyMode = true;
      squad_channel = ATTACK_END_CHANNEL - ATTACK_BLOCK_WIDTH;
      rc.broadcast(ATTACK_END_CHANNEL - ATTACK_BLOCK_WIDTH, 1);
      // Clear out target field
      rc.broadcast(ATTACK_END_CHANNEL - ATTACK_BLOCK_WIDTH + 1, -1);
      // Clear out blacklist field
      rc.broadcast(ATTACK_END_CHANNEL - ATTACK_BLOCK_WIDTH + 3, -1);
      return;
    }
    int i = ATTACK_START_CHANNEL;
    while (i < ATTACK_END_CHANNEL) {
      int squad_count = rc.readBroadcast(i);
      //System.out.println(squad_count);
      if (squad_count == 0) {
        // Clear out target field
        rc.broadcast(i + 1, -1);
        // Clear out blacklist field
        rc.broadcast(i + 3, -1);
      }
      if (squad_count < 10) {
        squad_channel = i;
        rc.broadcast(i, squad_count + 1);
        return;
      }
      i += ATTACK_BLOCK_WIDTH;
    }
    squad_channel = ATTACK_START_CHANNEL;
  }

  private static int readTargetX(int data) throws GameActionException {
    return (data & 0xFFFF0000) >>> 16;
  }

  private static int readTargetY(int data) throws GameActionException {
    return (data & 0x0000FFFF);
  }

  private static void writeTargetXY(int x, int y) throws GameActionException {
    rc.broadcast(squad_channel + 2, (x << 16) | y);
  }

  private static void readBlacklist() throws GameActionException {
    // First 16 bits is ID of blacklisted target
    // Second 16 bits is starting round of blacklist
    int data = rc.readBroadcast(squad_channel + 3);
    targetBlacklist = (data & (0xFFFF0000)) >>> 16;
    targetBlacklistPeriodStart = data & (0x0000FFFF);
  }

  private static void writeBlacklist(int blacklistTarget, int startRound)
      throws GameActionException {
    int data = (blacklistTarget << 16) | startRound;
    rc.broadcast(squad_channel + 3, data);
  }

  private static boolean isBlacklisted(int target) {
    return target == targetBlacklist && (currentRoundNum - targetBlacklistPeriodStart < 30);
  }

  public static boolean alert() throws GameActionException {
    if (current_mode != ROAM) {
      return false;
    }
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets(EvasiveScout.BULLET_DETECT_RADIUS);
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, them);
    if ((nearbyBullets.length != 0 || nearbyRobots.length != 0)
        && (!isPerchedInTree || shouldUnperch(nearbyRobots))) {
      if (EvasiveScout.move(nearbyBullets, nearbyRobots)) {
        isPerchedInTree = false;
        here = rc.getLocation();
      }
    }
    if (nearbyRobots.length == 0) {
      // TODO Use message broadcast location to try to find enemies
      //rc.senseBroadcastingRobotLocations();
      return false;
    }
    else {
      // TODO needs code cleanup
      if (currentRoundNum < 200) {
        for (RobotInfo enemy : nearbyRobots) {
          // Prioritize killing enemy gardeners
          if (isBlacklisted(enemy.getID())) {
            continue;
          }
          if (enemy.type == RobotType.GARDENER) {
            MapLocation enemyLoc = enemy.getLocation();
            rc.broadcast(squad_channel + 1, enemy.getID());
            writeTargetXY((int) (enemyLoc.x), (int) (enemyLoc.y));
            targetDirection = here.directionTo(enemyLoc);
            if (rc.canFireSingleShot() && clearShot(here, enemy)) {
              rc.fireSingleShot(targetDirection);
            }
            attackTarget = enemy.getID();
            current_mode = ATTACK;
            return true;
          }
        }
        if (gardenerOnlyMode) {
          return false;
        }
        for (RobotInfo enemy : nearbyRobots) {
          if (isBlacklisted(enemy.getID())) {
            continue;
          }
          if (enemy.type != RobotType.ARCHON) {
            MapLocation enemyLoc = enemy.getLocation();
            rc.broadcast(squad_channel + 1, enemy.getID());
            writeTargetXY((int) (enemyLoc.x), (int) (enemyLoc.y));
            targetDirection = here.directionTo(enemyLoc);
            if (rc.canFireSingleShot() && clearShot(here, enemy)) {
              rc.fireSingleShot(targetDirection);
            }
            attackTarget = enemy.getID();
            current_mode = ATTACK;
            return true;
          }
        }
      }
      else {
        RobotInfo enemy = null;
        // Preferred targets: Enemy gardeners if round < 2000
        if (currentRoundNum < 2000) {
          for (RobotInfo ri : nearbyRobots) {
            if (isBlacklisted(ri.getID())) {
              continue;
            }
            if (ri.getType() == RobotType.GARDENER) {
              enemy = ri;
              break;
            }
          }
        }
        if (gardenerOnlyMode && enemy == null) {
          return false;
        }
        // Choose any target that is not blacklisted
        if (enemy == null) {
          for (RobotInfo ri : nearbyRobots) {
            if (isBlacklisted(ri.getID())) {
              continue;
            }
            else {
              enemy = ri;
            }
          }
        }
        // Avoid wasting time/bullets attacking archons below round 1000
        if (enemy == null || (enemy.getType() == RobotType.ARCHON && currentRoundNum < 1000)) {
          return false;
        }
        MapLocation enemyLoc = enemy.getLocation();
        rc.broadcast(squad_channel + 1, enemy.getID());
        writeTargetXY((int) (enemyLoc.x), (int) (enemyLoc.y));
        targetDirection = here.directionTo(enemyLoc);
        if (rc.canFireSingleShot() && clearShot(here, enemy)) {
          rc.fireSingleShot(targetDirection);
        }
        attackTarget = enemy.getID();
        current_mode = ATTACK;
        return true;
      }
      return false;
    }
  }

  /*
   * Returns True if I have a clear shot at the person, false otherwise;
   */

  private static boolean noBulletsAtLocation(BulletInfo[] nearbyBullets, MapLocation loc) {
    for (BulletInfo bi : nearbyBullets) {
      if (loc.distanceTo(bi.getLocation()) < myType.bodyRadius) {
        return false;
      }
    }
    return true;
  }

  private static RobotInfo engage(int target, boolean priorityTarget) throws GameActionException {
    RobotInfo targetRobot = rc.senseRobot(target);
    if (!priorityTarget) {
      int broadcastTarget = rc.readBroadcast(squad_channel + 1);
      if (broadcastTarget == target) {
        MapLocation targetLoc = targetRobot.getLocation();
        writeTargetXY((int) targetLoc.x, (int) targetLoc.y);
      }
    }
    /*
    System.out.println(squad_channel);
    System.out.println(forceReEngage);
    System.out.println("Targeting " + target);
    System.out.println("Broadcast target " + broadcastTarget);
    */
    Direction direction = here.directionTo(targetRobot.getLocation());
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets(EvasiveScout.BULLET_DETECT_RADIUS);
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(EvasiveScout.ENEMY_DETECT_RADIUS, them);
    System.out.println("Engaging " + targetRobot);
    if (!rc.hasMoved() && (nearbyBullets.length != 0 || nearbyRobots.length != 0)
        && (!isPerchedInTree || shouldUnperch(nearbyRobots))) {
      if (EvasiveScout.move(nearbyBullets, nearbyRobots)) {
        isPerchedInTree = false;
        here = rc.getLocation();
      }
    }
    //System.out.println("Before engage: " + Clock.getBytecodesLeft());
    //System.out.println(target);
    boolean shouldShoot = true;
    if (!rc.hasMoved()) {
      float absolute_dist = (float) here.distanceTo(targetRobot.getLocation());
      if (absolute_dist > KEEPAWAY_RADIUS + RobotType.SCOUT.strideRadius) {
        shouldShoot = false;
        if (RobotUtils.tryMoveIfSafe(direction, nearbyBullets, 30, 3)) {
          isPerchedInTree = false;
        }
      }
      else {
        if (targetRobot.getType() == RobotType.GARDENER) {
          MapLocation optimalLoc = null;
          float optimalDist = 9999f;
          TreeInfo[] nearbyTrees = rc.senseNearbyTrees(targetRobot.getLocation(), 3f, them);
          System.out.println("Targeting gardener");
          boolean currentlyHasClearShot = clearShot(here, targetRobot);
          System.out.println("Currently has clear shot: " + currentlyHasClearShot);
          System.out.println("Is perched: " + isPerchedInTree);
          for (TreeInfo ti : nearbyTrees) {
            if (Clock.getBytecodesLeft() < 2000) {
              break;
            }
            // Try to avoid staying in 1 tree
            if (isPerchedInTree && here.equals(ti.getLocation())) {
              continue;
            }
            if (rc.canMove(ti.getLocation()) && clearShot(ti.getLocation(), targetRobot)) {
              float newDist = here.distanceTo(ti.getLocation());
              if (newDist < optimalDist) {
                optimalLoc = ti.getLocation();
                optimalDist = newDist;
              }
            }
          }
          boolean inTree = optimalLoc != null;
          if (inTree) {
            System.out.println("Found tree to perch in");
          }
          // TODO how is this affected by perching?
          if (optimalLoc == null && (!isPerchedInTree || !currentlyHasClearShot)) {
            for (int i = 0; i < GARDENER_PENETRATION_ANGLES.length; ++i) {
              if (Clock.getBytecodesLeft() < 2000) {
                break;
              }
              MapLocation newLoc = targetRobot.getLocation().add(GARDENER_PENETRATION_ANGLES[i],
                  GARDENER_KEEPAWAY_RADIUS);
              // TODO optimize
              /*
              System.out.println(rc.canMove(newLoc));
              System.out.println(clearShot(newLoc, targetRobot.location));
              */
              // Avoid staying in one position
              if (here.equals(newLoc)) {
                continue;
              }
              float newDist = here.distanceTo(newLoc);
              if (rc.canMove(newLoc) && newDist < optimalDist) {
                optimalLoc = newLoc;
                optimalDist = newDist;
              }
            }
          }
          if (optimalLoc != null) {
            if ((!isPerchedInTree || !currentlyHasClearShot)
                && optimalDist > RobotType.SCOUT.strideRadius) {
              Direction optimalDir = here.directionTo(optimalLoc);
              MapLocation scaledLoc = here.add(optimalDir, RobotType.SCOUT.strideRadius);
              shouldShoot = false;
              if (rc.canMove(scaledLoc) && RobotUtils.isLocationSafe(nearbyBullets, scaledLoc)) {
                rc.move(scaledLoc);
                isPerchedInTree = false;
                System.out.println("Moved towards optimal location");
              }
            }
            else if (optimalDist <= RobotType.SCOUT.strideRadius
                && RobotUtils.isLocationSafe(nearbyBullets, optimalLoc)) {
              rc.move(optimalLoc);
              if (inTree) {
                System.out.println("Moved to tree!");
                isPerchedInTree = true;
                hpWhenPerched = rc.getHealth();
              }
              System.out.println("Moved to optimal location");
            }
            /*
            if (DEBUG) {
              Globals.update();
              rc.setIndicatorDot(here, 255, 0, 0);
            }
            */
          }
        }
        else {
          MapLocation optimalLoc = null;
          float optimalDist = 9999f;
          if (targetRobot.type != RobotType.LUMBERJACK) {
            TreeInfo[] nearbyTrees = rc.senseNearbyTrees(5f);
            for (TreeInfo ti : nearbyTrees) {
              if (Clock.getBytecodesLeft() < 2000) {
                break;
              }
              if (!MathUtils.isInRange(0.99f, 1.1f, ti.radius)) {
                continue;
              }
              if (rc.canMove(ti.getLocation()) && clearShot(ti.getLocation(), targetRobot)) {
                float newDist = here.distanceTo(ti.getLocation());
                if (newDist < optimalDist) {
                  optimalLoc = ti.getLocation();
                  optimalDist = newDist;
                }
              }
            }
          }
          if (optimalLoc != null) {
            if (optimalDist > RobotType.SCOUT.strideRadius) {
              Direction optimalDir = here.directionTo(optimalLoc);
              MapLocation scaledLoc = here.add(optimalDir, RobotType.SCOUT.strideRadius);
              shouldShoot = false;
              if (rc.canMove(scaledLoc) && RobotUtils.isLocationSafe(nearbyBullets, scaledLoc)) {
                rc.move(scaledLoc);
                isPerchedInTree = false;
              }
            }
            else if (rc.canMove(optimalLoc) && RobotUtils.isLocationSafe(nearbyBullets, optimalLoc)) {
              rc.move(optimalLoc);
              isPerchedInTree = true;
              hpWhenPerched = rc.getHealth();
            }
            if (DEBUG) {
              Globals.update();
              rc.setIndicatorDot(here, 255, 0, 0);
            }
          }
          else {
            boolean currentlyHasClearShot = clearShot(here, targetRobot);
            Direction rotated30 = direction.opposite().rotateLeftDegrees(30);
            MapLocation newLoc = targetRobot.getLocation().add(rotated30, KEEPAWAY_RADIUS);
            if (rc.canMove(newLoc) && RobotUtils.isLocationSafe(nearbyBullets, newLoc)) {
              if (currentlyHasClearShot && clearShot(newLoc, targetRobot)) {
                //System.out.println("d");
                rc.move(newLoc);
                isPerchedInTree = false;
              }
              else if (!currentlyHasClearShot) {
                rc.move(newLoc);
                isPerchedInTree = false;
              }
            }
            else {
              rotated30 = direction.opposite().rotateRightDegrees(30);
              newLoc = targetRobot.getLocation().add(rotated30, KEEPAWAY_RADIUS);
              if (rc.canMove(newLoc) && RobotUtils.isLocationSafe(nearbyBullets, newLoc)) {
                if (currentlyHasClearShot && clearShot(newLoc, targetRobot)) {
                  //System.out.println("e");
                  rc.move(newLoc);
                  isPerchedInTree = false;
                }
                else if (!currentlyHasClearShot) {
                  rc.move(newLoc);
                  isPerchedInTree = false;
                }
              }
            }
          }
        }
      }
    }
    Globals.update();
    direction = here.directionTo(targetRobot.getLocation());
    if (shouldShoot && rc.canFireSingleShot() && clearShot(here, targetRobot)) {
      rc.fireSingleShot(direction);
      if (DEBUG) {
        //System.out.println("CLEARSHOT!");
        rc.setIndicatorDot(targetRobot.getLocation(), (us == Team.A) ? 255 : 0, 0,
            (us == Team.B ? 255 : 0));
      }
    }
    //System.out.println("After engage: " + Clock.getBytecodesLeft());
    return targetRobot;
  }

  public static void loop() {
    try {
      Globals.update();
      GARDENER_PENETRATION_ANGLES[0] = new Direction((float) Math.toRadians(0));
      GARDENER_PENETRATION_ANGLES[1] = new Direction((float) Math.toRadians(60));
      GARDENER_PENETRATION_ANGLES[2] = new Direction((float) Math.toRadians(120));
      GARDENER_PENETRATION_ANGLES[3] = new Direction((float) Math.toRadians(180));
      GARDENER_PENETRATION_ANGLES[4] = new Direction((float) Math.toRadians(240));
      GARDENER_PENETRATION_ANGLES[5] = new Direction((float) Math.toRadians(300));
      enemyArchons = rc.getInitialArchonLocations(them);
      EvasiveScout.init();
      findSquad();
      /*
      // Early scouts should move towards the archon
      if (currentRoundNum < 100) {
        int numUnits = rc.readBroadcast(EARLY_UNITS_CHANNEL);
        if (numUnits <= 3) {
          MapLocation targetArchonLoc;
          if (numUnits <= enemyArchons.length) {
            targetArchonLoc = enemyArchons[numUnits - 1];
          }
          else {
            targetArchonLoc = enemyArchons[0];
          }
          targetDirection = here.directionTo(targetArchonLoc);
        }
      }
      // Later scouts move in random directions
      if (targetDirection == null) {
        targetDirection = RobotUtils.randomDirection();
      }
      */
      targetDirection = RobotUtils.randomDirection();
    } catch (Exception e) {
      e.printStackTrace();
    }
    while (true) {
      try {
        Globals.update();
        // If damage is taken while perched in a tree, allow evading
        if (isPerchedInTree && rc.getHealth() != hpWhenPerched) {
          isPerchedInTree = false;
        }
        if (current_mode == ROAM) {
          readBlacklist();
          //System.out.println("Roaming");
          //rc.setIndicatorDot(here, 0, 0, 255);
          // Look for target in broadcast
          int target = rc.readBroadcast(squad_channel + 1);
          if (target != -1) {
            System.out.println("Found target in broadcast: " + target);
            current_mode = ATTACK;
            attackTarget = target;
            int data = rc.readBroadcast(squad_channel + 2);
            int xLoc = readTargetX(data);
            int yLoc = readTargetY(data);
            targetDirection = here.directionTo(new MapLocation(xLoc, yLoc));
            BulletInfo[] nearbyBullets = rc.senseNearbyBullets(EvasiveScout.BULLET_DETECT_RADIUS);
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(KEEPAWAY_RADIUS, them);
            if ((nearbyBullets.length != 0 || nearbyRobots.length != 0)
                && (!isPerchedInTree || shouldUnperch(nearbyRobots))) {
              if (EvasiveScout.move(nearbyBullets, nearbyRobots)) {
                isPerchedInTree = false;
                here = rc.getLocation();
              }
            }
            else if (rc.canMove(targetDirection)) {
              //System.out.println("f");
              rc.move(targetDirection);
              isPerchedInTree = false;
            }
          }
          else {
            System.out.println("Searching for target");
            boolean foundTarget = alert();
            if (!foundTarget) {
              int cacheTarget = rc.readBroadcast(GARDENER_TARGET_CACHE_CHANNEL);
              if (cacheTarget != 0) {
                int data = rc.readBroadcast(GARDENER_TARGET_CACHE_CHANNEL + 1);
                int cacheTargetX = readGardenerCacheX(data);
                int cacheTargetY = readGardenerCacheY(data);
                attackTarget = cacheTarget;
                targetDirection = here.directionTo(new MapLocation(cacheTargetX, cacheTargetY));
                rc.broadcast(squad_channel + 1, cacheTarget);
                writeTargetXY(cacheTargetX, cacheTargetY);
                rc.broadcast(GARDENER_TARGET_CACHE_CHANNEL, 0);
              }
            }
            if (!rc.hasMoved()) {
              System.out.println("Has not moved");
              // Move towards target in a straight line
              // TODO better pathfinding
              if (!rc.onTheMap(here.add(targetDirection,
                  RobotType.SCOUT.strideRadius + RobotType.SCOUT.bodyRadius))) {
                // Change direction when hitting border,
                // Note: should not happen when chasing a newly found target
                targetDirection = targetDirection
                    .rotateRightRads((float) (rand.nextFloat() * Math.PI));
                //System.out.println("Turning randomly " + targetDirection);
              }
              BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
              System.out.println("Moving towards target");
              if (RobotUtils.tryMoveIfSafe(targetDirection, nearbyBullets, 30, 3)) {
                isPerchedInTree = false;
              }
            }
          }
        }
        else if (current_mode == ATTACK) {
          //int startBytecodes = Clock.getBytecodeNum();
          //System.out.println("ATTACK");
          // Currently on attack mode
          int target = rc.readBroadcast(squad_channel + 1);
          // Read assigned target from broadcast
          // Handle external target change
          if (!priorityTarget && attackTarget != target) {
            System.out.println("Read target from broadcast: " + target);
            attackTarget = target;
            roundsEngaging = 0;
          }
          if (rc.canSenseRobot(attackTarget)) {
            // Engage target if it is in range
            System.out.println("Can sense target: " + attackTarget);
            engagingTarget = true;
            ++roundsEngaging;
            // TODO fix disengagement
            // Allow re-engagement on priority targets
            System.out.println(roundsEngaging);
            if (roundsEngaging > 200) {
              rc.broadcast(squad_channel + 1, -1);
              targetBlacklist = attackTarget;
              targetBlacklistPeriodStart = currentRoundNum;
              writeBlacklist(targetBlacklist, targetBlacklistPeriodStart);
              System.out.println("Blacklisting target: " + attackTarget);
              attackTarget = -1;
              roundsEngaging = 0;
              engagingTarget = false;
              current_mode = ROAM;
            }
            else {
              RobotInfo targetRobot = engage(attackTarget, priorityTarget);
              if ((targetRobot != null && targetRobot.type != RobotType.GARDENER)
                  || targetRobot == null) {
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, them);
                for (RobotInfo ri : nearbyRobots) {
                  if (ri.type == RobotType.GARDENER) {
                    target = ri.getID();
                    attackTarget = target;
                    System.out.println("Switching to priority target: " + target);
                    priorityTarget = true;
                    break;
                  }
                }
              }
            }
          }
          else {
            BulletInfo[] nearbyBullets = rc.senseNearbyBullets(EvasiveScout.BULLET_DETECT_RADIUS);
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, them);
            if ((nearbyBullets.length != 0 || nearbyRobots.length != 0)
                && (!isPerchedInTree || shouldUnperch(nearbyRobots))) {
              if (EvasiveScout.move(nearbyBullets, nearbyRobots)) {
                isPerchedInTree = false;
                here = rc.getLocation();
              }
            }
            System.out.println("Cannot sense target: " + attackTarget);
            // We are out of range of our target,
            // so try to move in known direction of target to find target
            if (engagingTarget) {
              engagingTarget = false;
              roundsEngaging = 0;
              // Target is assumed to be killed, so update broadcast target
              if (!priorityTarget && attackTarget == target) {
                System.out.println("Target killed");
                rc.broadcast(squad_channel + 1, -1);
                target = -1;
                attackTarget = -1;
                current_mode = ROAM;
                targetDirection = RobotUtils.randomDirection();
                /*
                if (!rc.hasMoved() && rc.canMove(targetDirection)) {
                  //System.out.println("i");
                  rc.move(targetDirection);
                }
                */
              }
              else if (priorityTarget) {
                priorityTarget = false;
              }
            }
            for (RobotInfo ri : nearbyRobots) {
              if (ri.type == RobotType.GARDENER) {
                target = ri.getID();
                attackTarget = target;
                System.out.println("Switching to priority target: " + target);
                priorityTarget = true;
                break;
              }
            }
            if (!priorityTarget && attackTarget != -1) {
              int data = rc.readBroadcast(squad_channel + 2);
              int xLoc = readTargetX(data);
              int yLoc = readTargetY(data);
              MapLocation targetLoc = new MapLocation(xLoc, yLoc);
              System.out.println("Moving towards target: " + targetLoc);
              float distToTarget = here.distanceTo(targetLoc);
              if (distToTarget < RobotType.ARCHON.sensorRadius + MIN_ROBOT_RADIUS && !rc.canSenseRobot(attackTarget)) {
                System.out.println("Could not find target at last known location");
                rc.broadcast(squad_channel + 1, -1);
              }
              else {
                targetDirection = here.directionTo(targetLoc);
                if (!rc.hasMoved()) {
                  //System.out.println("j");
                  if (RobotUtils.tryMoveIfSafe(targetDirection, nearbyBullets, 30, 3)) {
                    isPerchedInTree = false;
                  }
                }
                else {
                  // We had to evade, so we couldn't move towards the target
                  if (nearbyRobots.length != 0) {
                    RobotInfo targetRobot = nearbyRobots[0];
                    if (rc.canFireSingleShot() && clearShot(here, targetRobot)) {
                      MapLocation obstacleLocation = targetRobot.getLocation();
                      rc.fireSingleShot(here.directionTo(obstacleLocation));
                    }
                  }
                }
              }
            }
            // Disengage attack mode if no target
            else if (attackTarget == -1) {
              System.out.println("Disengaging attack mode: no broadcast target");
              current_mode = ROAM;
              targetDirection = RobotUtils.randomDirection();
              /*
              if (!rc.hasMoved() && rc.canMove(targetDirection)) {
                //System.out.println("k");
                rc.move(targetDirection);
              }
              */
              //System.out.println(direction.getAngleDegrees());
            }
            else if (priorityTarget && !rc.canSenseRobot(attackTarget)) {
              System.out.println("Disengaging priority target");
              current_mode = ROAM;
              attackTarget = -1;
              targetDirection = RobotUtils.randomDirection();
              priorityTarget = false;
              engagingTarget = false;
              roundsEngaging = 0;
            }
          }
          //System.out.println("Used: " + (Clock.getBytecodeNum() - startBytecodes));
        }
        else if (current_mode == DISCOVER) {
          BulletInfo[] nearbyBullets = rc.senseNearbyBullets(EvasiveScout.BULLET_DETECT_RADIUS);
          RobotInfo[] nearbyRobots = rc.senseNearbyRobots(EvasiveScout.ENEMY_DETECT_RADIUS, them);
          if ((nearbyBullets.length != 0 || nearbyRobots.length != 0)
              && (!isPerchedInTree || shouldUnperch(nearbyRobots))) {
            if (EvasiveScout.move(nearbyBullets, nearbyRobots)) {
              isPerchedInTree = false;
              here = rc.getLocation();
            }
          }
          if (!rc.hasMoved()) {
            TreeInfo[] nearbyTrees = rc.senseNearbyTrees(-1, Team.NEUTRAL);
            TreeInfo preferredTree = null;
            float preferredTreeDist = 9999f;
            for (TreeInfo ti : nearbyTrees) {
              if (Clock.getBytecodesLeft() < 2000) {
                break;
              }
              MapLocation treeLoc = ti.getLocation();
              /*
              if (!rc.canMove(treeLoc)) {
                continue;
              }
              */
              float treeDist = treeLoc.distanceTo(here);
              if (treeDist < preferredTreeDist && ((preferredTree == null && ti.getContainedBullets() > 0)
                  || (preferredTree != null && ti.getContainedBullets() >= preferredTree.getContainedBullets()))) {
                preferredTreeDist = treeDist;
                preferredTree = ti;
              }
            }
            if (preferredTree != null) {
              System.out.println("Preferred tree: " + preferredTree);
              System.out.println("Distance: " + preferredTreeDist);
              if (preferredTreeDist <= RobotType.SCOUT.strideRadius) {
                if (rc.canMove(preferredTree.getLocation())) {
                  rc.move(preferredTree.getLocation());
                }
              }
              else {
                System.out.println("Trying to move towards tree");
                if (RobotUtils.tryMoveIfSafe(here.directionTo(preferredTree.getLocation()), nearbyBullets, 30, 4)) {
                  isPerchedInTree = false;
                }
              }
            }
            else {
              System.out.println("No trees with bullets--moving randomly");
              if (!rc.onTheMap(here.add(targetDirection,
                  1 + RobotType.SCOUT.strideRadius + RobotType.SCOUT.bodyRadius))) {
                // Change direction when hitting border
                targetDirection = targetDirection
                    .rotateRightRads((float) (rand.nextFloat() * Math.PI));
              }
              // Handle case when blocked by both obstacle and border
              if (!RobotUtils.tryMoveIfSafe(targetDirection, nearbyBullets, rand.nextFloat() * 10 + 30, 3)) {
                targetDirection = targetDirection
                    .rotateRightRads((float) (rand.nextFloat() * Math.PI));
              }
              else {
                isPerchedInTree = false;
              }
            }
          }
          if (currentRoundNum > 400) {
            current_mode = ROAM;
          }
        }
        if (!hasReportedDeath && rc.getHealth() < 3f) {
          int squad_count = rc.readBroadcast(squad_channel);
          hasReportedDeath = true;
          rc.broadcast(squad_channel, squad_count - 1);
        }
        RobotUtils.donateEverythingAtTheEnd();
        RobotUtils.shakeNearbyTrees();
        trackEnemyGardeners();
        if (gardenerOnlyMode) {
          rc.setIndicatorDot(rc.getLocation(), 0, 0, 255);
        }
        Clock.yield();
      } catch (Exception e) {
        e.printStackTrace();
        Clock.yield();
      }
    }
  }
}