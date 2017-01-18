package finalVersion;

import battlecode.common.*;
import utils.Globals;
import utils.RobotUtils;

import static utils.TargetingUtils.clearShot;

public class Scout extends Globals {

  private final static int ROAM = 0;
  private final static int ATTACK = 1;
  private static int current_mode = ROAM;
  private static final float KEEPAWAY_RADIUS = EvasiveScout.ENEMY_DETECT_RADIUS;
  private static final float GARDENER_KEEPAWAY_RADIUS = 2.00001f;
  private static Direction[] GARDENER_PENETRATION_ANGLES = new Direction[6];
  private static Direction targetDirection = null;
  private static int squad_channel;
  private static int attackTarget;
  private static boolean hasReportedDeath = false;
  private static boolean priorityTarget = false;
  private static boolean engagingTarget = false;
  private static int roundsEngaging = 0;
  private static boolean isPerchedInTree = false;
  private static float hpWhenPerched = 0f;
  private static int targetBlacklist = -1;
  private static int targetBlacklistPeriodStart = -1;

  //public static void dodge(BulletInfo[] nearbyBullets, RobotInfo[] nearbyRobots)
  //    throws GameActionException {
  //  boolean willHit = false;
  //  MapLocation[] startLocs = new MapLocation[nearbyBullets.length];
  //  MapLocation[] endLocs = new MapLocation[nearbyBullets.length];
  //  int index = 0;
  //  for (BulletInfo b : nearbyBullets) {
  //    MapLocation finalLoc = b.location.add(b.dir, b.speed);
  //    startLocs[index] = b.location;
  //    endLocs[index] = finalLoc;
  //    index++;
  //    /*
  //    float dist = (float)(Math.sqrt(Math.pow(here.x - finalLoc.x, 2) + Math.pow(here.y - finalLoc.y, 2)));
  //    if(dist < RobotType.SCOUT.bodyRadius){
  //      willHit = true;
  //    }*/
  //    if (RobotUtils.willCollideWithMe(b)) {
  //      willHit = true;
  //    }
  //  }
  //  float sumX = 0;
  //  float sumY = 0;
  //  if (willHit) {
  //    for (int i = 0; i < index; i++) {
  //      float x0 = startLocs[i].x;
  //      float y0 = startLocs[i].y;
  //      float x1 = endLocs[i].x;
  //      float y1 = endLocs[i].y;
  //      float a = x1 - x0;
  //      float b = y0 - y1;
  //      float c = x0 * y1 - y0 * x1;
  //      float distance = (float) (Math.abs(a * here.x + b * here.y + c)
  //          / Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2)));
  //      float x2 = (float) ((b * (b * here.x - a * here.y) - a * c)
  //          / (Math.pow(a, 2) + Math.pow(b, 2)));
  //      float y2 = (float) ((a * (a * here.y - b * here.x) - b * c)
  //          / (Math.pow(a, 2) + Math.pow(b, 2)));
  //      Direction away = here.directionTo(new MapLocation(x2, y2)).opposite();
  //      float weighted = (RobotType.SCOUT.bulletSightRadius - distance)
  //          / RobotType.SCOUT.bulletSightRadius * RobotType.SCOUT.strideRadius;
  //      sumX += away.getDeltaX(weighted);
  //      sumY += away.getDeltaY(weighted);
  //    }
  //  }
  //  for (RobotInfo r : nearbyRobots) {
  //    if (r.getType() == RobotType.LUMBERJACK) {
  //      Direction their_direction = here.directionTo(r.location).opposite();
  //      float their_distance = (RobotType.SCOUT.sensorRadius - here.distanceTo(r.location))
  //          / RobotType.SCOUT.sensorRadius * RobotType.SCOUT.strideRadius;
  //      sumX += their_direction.getDeltaX(their_distance);
  //      sumY += their_direction.getDeltaY(their_distance);
  //      index++;
  //    }
  //  }
  //  float finaldist = (float) Math.sqrt(Math.pow(sumX, 2) + Math.pow(sumY, 2));
  //  if (finaldist <= RobotType.SCOUT.strideRadius) {
  //    MapLocation destination = new MapLocation(here.x + sumX, here.y + sumY);
  //    if (rc.canMove(destination) && !rc.hasMoved()) {
  //      //System.out.println("a");
  //      rc.move(destination);
  //    }
  //  }
  //  else {
  //    Direction finalDir = new Direction(sumX, sumY);
  //    if (rc.canMove(finalDir) && !rc.hasMoved()) {
  //      //System.out.println("b");
  //      rc.move(finalDir);
  //    }
  //  }
  //}
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

  public static void findSquad() throws GameActionException {
    int i = ATTACK_START_CHANNEL;
    while (i < ATTACK_END_CHANNEL) {
      int squad_count = rc.readBroadcast(i);
      //System.out.println(squad_count);
      if (squad_count == 0) {
        // Clear out target field
        rc.broadcast(i+1, -1);
        // Clear out blacklist field
        rc.broadcast(i+4, -1);
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

  private static void readBlacklist() throws GameActionException {
    // First 16 bits is ID of blacklisted target
    // Second 16 bits is starting round of blacklist
    int data = rc.readBroadcast(squad_channel + 4);
    targetBlacklist = data & (0xFFFF0000) >>> 16;
    targetBlacklistPeriodStart = data & (0x0000FFFF);
  }

  private static void writeBlacklist(int blacklistTarget, int startRound) throws GameActionException {
    int data = (blacklistTarget << 16) | startRound;
    rc.broadcast(squad_channel + 4, data);
  }

  private static boolean isBlacklisted(int target) {
    return target == targetBlacklist && (currentRoundNum - targetBlacklistPeriodStart < 30);
  }

  public static void alert() throws GameActionException {
    if (current_mode != ROAM) {
      return;
    }
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets(EvasiveScout.BULLET_DETECT_RADIUS);
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, them);
    if ((nearbyBullets.length != 0 || nearbyRobots.length != 0) && !isPerchedInTree) {
      EvasiveScout.move(nearbyBullets, nearbyRobots);
    }
    if (nearbyRobots.length == 0) {
      // TODO Use message broadcast location to try to find enemies
      //rc.senseBroadcastingRobotLocations();
      return;
    }
    else {
      if (currentRoundNum < 200) {
        for (RobotInfo enemy : nearbyRobots) {
          // Prioritize killing enemy gardeners or defending our own gardeners
          // TODO also defend against soldiers and lumberjacks?
          // TODO is this necessary, because we have gardeners defense calls?
          if (isBlacklisted(enemy.ID)) {
            continue;
          }
          if (enemy.type != RobotType.ARCHON) {
            rc.broadcast(squad_channel + 1, enemy.ID);
            rc.broadcast(squad_channel + 2, (int) (enemy.location.x));
            rc.broadcast(squad_channel + 3, (int) (enemy.location.y));
            targetDirection = here.directionTo(enemy.location);
            if (rc.canFireSingleShot() && clearShot(here, enemy)) {
              rc.fireSingleShot(targetDirection);
            }
            current_mode = ATTACK;
            break;
          }
        }
      }
      else {
        RobotInfo enemy = null;
        // Preferred targets: Enemy gardeners if round < 1000
        if (currentRoundNum < 2000) {
          for (RobotInfo ri : nearbyRobots) {
            if (isBlacklisted(ri.ID)) {
              continue;
            }
            if (ri.type == RobotType.GARDENER) {
              enemy = ri;
              break;
            }
          }
        }
        // Choose any target that is not blacklisted
        if (enemy == null) {
          for (RobotInfo ri : nearbyRobots) {
            if (isBlacklisted(ri.ID)) {
              continue;
            }
            else {
              enemy = ri;
            }
          }
        }
        // Avoid wasting time/bullets attacking archons below round 1000
        if (enemy == null || (enemy.type == RobotType.ARCHON && currentRoundNum < 1000)) {
          return;
        }
        rc.broadcast(squad_channel + 1, enemy.ID);
        rc.broadcast(squad_channel + 2, (int) (enemy.location.x));
        rc.broadcast(squad_channel + 3, (int) (enemy.location.y));
        targetDirection = here.directionTo(enemy.location);
        if (rc.canFireSingleShot() && clearShot(here, enemy)) {
          rc.fireSingleShot(targetDirection);
        }
        current_mode = ATTACK;
      }
    }
  }

  /*
   * Returns True if I have a clear shot at the person, false otherwise;
   */

  private static boolean noBulletsAtLocation(BulletInfo[] nearbyBullets, MapLocation loc) {
    for (BulletInfo bi : nearbyBullets) {
      if (loc.distanceTo(bi.location) < myType.bodyRadius) {
        return false;
      }
    }
    return true;
  }

  private static boolean tryMoveIfSafe(Direction dir, BulletInfo[] nearbyBullets,
      float degreeOffset, int checksPerSide) throws GameActionException {
    // First, try intended direction
    //System.out.println("Called tryMove");
    MapLocation newLoc = here.add(dir, myType.strideRadius);
    if (rc.canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
      //System.out.println("tryMove: " + newLoc);
      rc.move(newLoc);
      isPerchedInTree = false;
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
      if (rc.canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
        rc.move(newLoc);
        isPerchedInTree = false;
        //System.out.println("tryMove: " + newLoc);
        return true;
      }
      else if (Clock.getBytecodesLeft() < 2000) {
        return false;
      }
      newLoc = here.add(dir.rotateRightDegrees(offset), myType.strideRadius);
      // Try the offset on the right side
      if (rc.canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
        rc.move(newLoc);
        isPerchedInTree = false;
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

  private static boolean isLocationSafe(BulletInfo[] nearbyBullets, MapLocation loc) {
    for (BulletInfo bi : nearbyBullets) {
      if (Clock.getBytecodesLeft() < 2000) {
        return false;
      }
      if (RobotUtils.willCollideWithTargetLocation(bi.location, bi.dir, loc, myType.bodyRadius)) {
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
        rc.broadcast(squad_channel + 2, (int) targetRobot.location.x);
        rc.broadcast(squad_channel + 3, (int) targetRobot.location.y);
      }
    }
    /*
    System.out.println(squad_channel);
    System.out.println(forceReEngage);
    System.out.println("Targeting " + target);
    System.out.println("Broadcast target " + broadcastTarget);
    */
    Direction direction = here.directionTo(targetRobot.location);
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets(EvasiveScout.BULLET_DETECT_RADIUS);
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(EvasiveScout.ENEMY_DETECT_RADIUS, them);
    if (!rc.hasMoved() && (nearbyBullets.length != 0 || nearbyRobots.length != 0) && !isPerchedInTree) {
      EvasiveScout.move(nearbyBullets, nearbyRobots);
    }
    //System.out.println("Before engage: " + Clock.getBytecodesLeft());
    //System.out.println(target);
    boolean shouldShoot = true;
    if (!rc.hasMoved()) {
      float absolute_dist = (float) here.distanceTo(targetRobot.location);
      if (absolute_dist > KEEPAWAY_RADIUS + RobotType.SCOUT.strideRadius) {
        shouldShoot = false;
        tryMoveIfSafe(direction, nearbyBullets, 15, 3);
      }
      else {
        if (targetRobot.type == RobotType.GARDENER) {
          MapLocation optimalLoc = null;
          float optimalDist = 9999f;
          TreeInfo[] nearbyTrees = rc.senseNearbyTrees(targetRobot.location, 2.2f, them);
          for (TreeInfo ti : nearbyTrees) {
            if (Clock.getBytecodesLeft() < 2000) {
              break;
            }
            if (rc.canMove(ti.location)) {
              float newDist = here.distanceTo(ti.location);
              if (newDist < optimalDist) {
                optimalLoc = ti.location;
                optimalDist = newDist;
              }
            }
          }
          if (optimalLoc == null && absolute_dist > GARDENER_KEEPAWAY_RADIUS) {
            for (int i = 0; i < GARDENER_PENETRATION_ANGLES.length; ++i) {
              if (Clock.getBytecodesLeft() < 2000) {
                break;
              }
              MapLocation newLoc = targetRobot.location.add(GARDENER_PENETRATION_ANGLES[i],
                  GARDENER_KEEPAWAY_RADIUS);
              // TODO optimize
              /*
              System.out.println(rc.canMove(newLoc));
              System.out.println(clearShot(newLoc, targetRobot.location));
              */
              float newDist = here.distanceTo(newLoc);
              if (rc.canMove(newLoc) && newDist < optimalDist) {
                optimalLoc = newLoc;
                optimalDist = newDist;
              }
            }
          }
          if (optimalLoc != null) {
            if (optimalDist > RobotType.SCOUT.strideRadius) {
              Direction optimalDir = here.directionTo(optimalLoc);
              MapLocation scaledLoc = here.add(optimalDir, RobotType.SCOUT.strideRadius);
              if (rc.canMove(scaledLoc) && isLocationSafe(nearbyBullets, scaledLoc)) {
                rc.move(scaledLoc);
                isPerchedInTree = false;
              }
            }
            else if (isLocationSafe(nearbyBullets, optimalLoc)) {
              rc.move(optimalLoc);
              isPerchedInTree = true;
              hpWhenPerched = rc.getHealth();
            }
            if (DEBUG) {
              Globals.update();
              rc.setIndicatorDot(here, 255, 0, 0);
            }
          }
        }
        else {
          MapLocation optimalLoc = null;
          float optimalDist = 9999f;
          if (targetRobot.type != RobotType.LUMBERJACK) {
            TreeInfo[] nearbyTrees = rc.senseNearbyTrees(targetRobot.location, 8f, Team.NEUTRAL);
            for (TreeInfo ti : nearbyTrees) {
              if (Clock.getBytecodesLeft() < 2000) {
                break;
              }
              if (ti.radius > 1f) {
                continue;
              }
              if (rc.canMove(ti.location) && clearShot(ti.location, targetRobot)) {
                float newDist = here.distanceTo(ti.location);
                if (newDist < optimalDist) {
                  optimalLoc = ti.location;
                  optimalDist = newDist;
                }
              }
            }
          }
          if (optimalLoc != null) {
            if (optimalDist > RobotType.SCOUT.strideRadius) {
              Direction optimalDir = here.directionTo(optimalLoc);
              MapLocation scaledLoc = here.add(optimalDir, RobotType.SCOUT.strideRadius);
              if (rc.canMove(scaledLoc) && isLocationSafe(nearbyBullets, scaledLoc)) {
                rc.move(scaledLoc);
                isPerchedInTree = false;
              }
            }
            else if (rc.canMove(optimalLoc) && isLocationSafe(nearbyBullets, optimalLoc)) {
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
            MapLocation newLoc = targetRobot.location.add(rotated30, KEEPAWAY_RADIUS);
            if (rc.canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
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
              newLoc = targetRobot.location.add(rotated30, KEEPAWAY_RADIUS);
              if (rc.canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
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
    direction = here.directionTo(targetRobot.location);
    if (shouldShoot && rc.canFireSingleShot() && clearShot(here, targetRobot)) {
      rc.fireSingleShot(direction);
      if (DEBUG) {
        //System.out.println("CLEARSHOT!");
        rc.setIndicatorDot(targetRobot.location, (us == Team.A) ? 255 : 0, 0,
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
      EvasiveScout.init();
      // Early scouts should move towards the archon
      findSquad();
      if (currentRoundNum < 100) {
        int numUnits = rc.readBroadcast(EARLY_UNITS_CHANNEL);
        if (numUnits <= 3) {
          MapLocation[] enemies = rc.getInitialArchonLocations(them);
          MapLocation targetArchonLoc;
          if (numUnits <= enemies.length) {
            targetArchonLoc = enemies[numUnits - 1];
          }
          else {
            targetArchonLoc = enemies[0];
          }
          targetDirection = here.directionTo(targetArchonLoc);
        }
      }
      // Later scouts move in random directions
      if (targetDirection == null) {
        targetDirection = RobotUtils.randomDirection();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    while (true) {
      try {
        // If damage is taken while perched in a tree, allow evading
        if (isPerchedInTree && rc.getHealth() != hpWhenPerched) {
          isPerchedInTree = false;
        }
        if (current_mode == ROAM) {
          Globals.update();
          readBlacklist();
          //System.out.println("Roaming");
          //rc.setIndicatorDot(here, 0, 0, 255);
          // Look for target in broadcast
          int target = rc.readBroadcast(squad_channel + 1);
          if (target != -1) {
            //System.out.println("Found target in broadcast" + target);
            current_mode = ATTACK;
            int xLoc = rc.readBroadcast(squad_channel + 2);
            int yLoc = rc.readBroadcast(squad_channel + 3);
            targetDirection = here.directionTo(new MapLocation(xLoc, yLoc));
            BulletInfo[] nearbyBullets = rc.senseNearbyBullets(EvasiveScout.BULLET_DETECT_RADIUS);
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(KEEPAWAY_RADIUS, them);
            if ((nearbyBullets.length != 0 || nearbyRobots.length != 0) && !isPerchedInTree) {
              EvasiveScout.move(nearbyBullets, nearbyRobots);
            }
            else if (rc.canMove(targetDirection)) {
              //System.out.println("f");
              rc.move(targetDirection);
              isPerchedInTree = false;
            }
          }
          else {
            System.out.println("Searching for target");
            alert();
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
              tryMoveIfSafe(targetDirection, nearbyBullets, 15, 3);
            }
            // If mode changed, yield before beginning attack logic
            if (current_mode == ATTACK) {
              System.out.println("Changed from ROAM to ATTACK");
              Clock.yield();
            }
          }
        }
        else if (current_mode == ATTACK) {
          Globals.update();
          //int startBytecodes = Clock.getBytecodeNum();
          //System.out.println("ATTACK");
          // Currently on attack mode
          int target = rc.readBroadcast(squad_channel + 1);
          // Read assigned target from broadcast
          // Handle external target change
          if (attackTarget != target) {
            attackTarget = target;
            roundsEngaging = 0;
          }
          System.out.println(roundsEngaging);
          if (rc.canSenseRobot(target)) {
            // Engage target if it is in range
            //System.out.println("Can sense target: " + target);
            engagingTarget = true;
            ++roundsEngaging;
            // TODO fix disengagement
            // Allow re-engagement on priority targets
            if (roundsEngaging > 100) {
              rc.broadcast(squad_channel + 1, -1);
              targetBlacklist = target;
              targetBlacklistPeriodStart = currentRoundNum;
              writeBlacklist(targetBlacklist, targetBlacklistPeriodStart);
              System.out.println("Blacklisting target: " + target);
            }
            else {
              RobotInfo targetRobot = engage(target, priorityTarget);
              if ((targetRobot != null && targetRobot.type != RobotType.GARDENER)
                  || targetRobot == null) {
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(6, them);
                for (RobotInfo ri : nearbyRobots) {
                  if (ri.type == RobotType.GARDENER) {
                    target = ri.ID;
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
            if ((nearbyBullets.length != 0 || nearbyRobots.length != 0) && !isPerchedInTree) {
              EvasiveScout.move(nearbyBullets, nearbyRobots);
            }
            //System.out.println("Cannot sense target: " + target);
            // We are out of range of our target,
            // so try to move in known direction of target to find target
            if (engagingTarget) {
              engagingTarget = false;
              roundsEngaging = 0;
              // Target is assumed to be killed, so update broadcast target
              if (!priorityTarget && attackTarget == target) {
                //System.out.println("Target killed");
                int broadcastTarget = rc.readBroadcast(squad_channel + 1);
                if (broadcastTarget == target) {
                  rc.broadcast(squad_channel + 1, -1);
                }
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
            if (target != -1) {
              int xLoc = rc.readBroadcast(squad_channel + 2);
              int yLoc = rc.readBroadcast(squad_channel + 3);
              MapLocation targetLoc = new MapLocation(xLoc, yLoc);
              //System.out.println("Moving towards target: " + targetLoc);
              float distToTarget = here.distanceTo(targetLoc);
              if (distToTarget < RobotType.ARCHON.sensorRadius && !rc.canSenseRobot(target)) {
                System.out.println("Could not find target at last known location");
                rc.broadcast(squad_channel + 1, -1);
              }
              else {
                targetDirection = here.directionTo(targetLoc);
                if (!rc.hasMoved()) {
                  //System.out.println("j");
                  tryMoveIfSafe(targetDirection, nearbyBullets, 15, 3);
                }
                else {
                  // We had to evade, so we couldn't move towards the target
                  if (nearbyRobots.length != 0) {
                    RobotInfo targetRobot = nearbyRobots[0];
                    if (rc.canFireSingleShot() && clearShot(here, targetRobot)) {
                      MapLocation obstacleLocation = targetRobot.location;
                      rc.fireSingleShot(here.directionTo(obstacleLocation));
                    }
                  }
                }
              }
            }
            // Disengage if no target
            else {
              //System.out.println("Disengaging: no boradcast target");
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
          }
          //System.out.println("Used: " + (Clock.getBytecodeNum() - startBytecodes));
        }
        if (!hasReportedDeath && rc.getHealth() < 3f) {
          int squad_count = rc.readBroadcast(squad_channel);
          hasReportedDeath = true;
          rc.broadcast(squad_channel, squad_count - 1);
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