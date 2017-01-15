package finalVersion;

import battlecode.common.*;
import utils.Globals;
import utils.RobotUtils;

import static utils.TargetingUtils.clearShot;

public class Scout extends Globals {

  private final static int ROAM = 0;
  private final static int ATTACK = 1;
  private static int current_mode = ROAM;
  private static final float KEEPAWAY_RADIUS = 3f;
  private static final float GARDENER_KEEPAWAY_RADIUS = 2.00001f;
  private static Direction[] GARDENER_PENETRATION_ANGLES = new Direction[6];
  private static Direction targetDirection = null;
  private static int squad_channel;
  private static int attackTarget;
  private static boolean hasReportedDeath = false;
  private static boolean priorityTarget = false;
  private static boolean engagingTarget = false;

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

  public static void findSquad() throws GameActionException {
    int i = ATTACK_START_CHANNEL;
    while (i < ATTACK_END_CHANNEL) {
      int squad_count = rc.readBroadcast(i);
      //System.out.println(squad_count);
      if (squad_count < 10) {
        squad_channel = i;
        rc.broadcast(i, squad_count + 1);
        return;
      }
      i = i + 4;
    }
    squad_channel = ATTACK_START_CHANNEL;
  }

  public static void alert() throws GameActionException {
    if (current_mode != ROAM) {
      return;
    }
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets(EvasiveScout.BULLET_DETECT_RADIUS);
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, them);
    if (nearbyBullets.length != 0 || nearbyRobots.length != 0) {
      EvasiveScout.move(nearbyBullets, nearbyRobots);
    }
    if (nearbyRobots.length == 0) {
      return;
    }
    else {
      if (rc.getRoundNum() < 200) {
        RobotInfo[] nearbyFriendlies = rc.senseNearbyRobots(-1, us);
        boolean friendlyGardener = false;
        for (RobotInfo ri : nearbyFriendlies) {
          if (ri.type == RobotType.GARDENER) {
            friendlyGardener = true;
            break;
          }
        }
        for (RobotInfo enemy : nearbyRobots) {
          // Prioritize killing enemy gardeners or defending our own gardeners
          // TODO also defend against soldiers and lumberjacks?
          // TODO is this necessary, because we have gardeners defense calls?
          if ((enemy.type == RobotType.SCOUT && friendlyGardener)
              || enemy.type == RobotType.GARDENER) {
            rc.broadcast(squad_channel + 1, enemy.ID);
            rc.broadcast(squad_channel + 2, (int) (enemy.location.x));
            rc.broadcast(squad_channel + 3, (int) (enemy.location.y));
            targetDirection = here.directionTo(enemy.location);
            if (rc.canFireSingleShot() && clearShot(here, enemy.location)) {
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
        if (rc.getRoundNum() < 1000) {
          for (RobotInfo ri : nearbyRobots) {
            if (ri.type == RobotType.GARDENER) {
              enemy = ri;
              break;
            }
          }
        }
        if (enemy == null) {
          enemy = nearbyRobots[0];
        }
        // Avoid wasting time/bullets attacking archons below round 1000
        if (enemy.type == RobotType.ARCHON && rc.getRoundNum() < 1000) {
          return;
        }
        rc.broadcast(squad_channel + 1, enemy.ID);
        rc.broadcast(squad_channel + 2, (int) (enemy.location.x));
        rc.broadcast(squad_channel + 3, (int) (enemy.location.y));
        targetDirection = here.directionTo(enemy.location);
        if (rc.canFireSingleShot() && clearShot(here, enemy.location)) {
          rc.fireSingleShot(targetDirection);
          rc.setIndicatorDot(enemy.location, 255, 0, 0);
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
    MapLocation newLoc = here.add(dir, myType.strideRadius);
    if (rc.canMove(dir) && isLocationSafe(nearbyBullets, newLoc)) {
      rc.move(dir);
      return true;
    }

    // Now try a bunch of similar angles
    boolean moved = false;
    int currentCheck = 1;

    while (currentCheck <= checksPerSide) {
      // Try the offset of the left side
      float offset = degreeOffset * currentCheck;
      newLoc = here.add(dir.rotateLeftDegrees(offset), myType.strideRadius);
      if (rc.canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
        rc.move(newLoc);
        return true;
      }
      newLoc = here.add(dir.rotateRightDegrees(offset), myType.strideRadius);
      // Try the offset on the right side
      if (rc.canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
        rc.move(newLoc);
        return true;
      }
      // No move performed, try slightly further
      currentCheck++;
    }

    // A move never happened, so return false.
    return false;
  }

  private static boolean isLocationSafe(BulletInfo[] nearbyBullets, MapLocation loc) {
    for (BulletInfo bi : nearbyBullets) {
      if (RobotUtils.willCollideWithTargetLocation(bi.location, bi.dir, loc, myType.bodyRadius)) {
        return false;
      }
    }
    return true;
  }

  public static RobotInfo engage(int target, boolean priorityTarget) throws GameActionException {
    RobotInfo targetRobot = rc.senseRobot(target);
    int broadcastTarget = rc.readBroadcast(squad_channel + 1);
    if (!priorityTarget) {
      if (broadcastTarget == target) {
        rc.broadcast(squad_channel + 2, (int) targetRobot.location.x);
        rc.broadcast(squad_channel + 3, (int) targetRobot.location.y);
      }
      else {
        // Handle external target change
        target = broadcastTarget;
        targetRobot = rc.senseRobot(target);
        attackTarget = broadcastTarget;
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
    if (!rc.hasMoved() && ((nearbyBullets != null && nearbyBullets.length != 0)
        || (nearbyRobots != null && nearbyRobots.length != 0))) {
      EvasiveScout.move(nearbyBullets, nearbyRobots);
    }
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
            if (rc.canMove(ti.location)) {
              float newDist = here.distanceTo(ti.location);
              if (newDist < optimalDist) {
                optimalLoc = ti.location;
                optimalDist = newDist;
              }
            }
          }
          if (optimalLoc == null) {
            for (int i = 0; i < GARDENER_PENETRATION_ANGLES.length; ++i) {
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
            rc.move(optimalLoc);
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
            TreeInfo[] nearbyTrees = rc.senseNearbyTrees(-1);
            for (TreeInfo ti : nearbyTrees) {
              if (!ti.team.isPlayer() && ti.radius > 1f) {
                continue;
              }
              if (rc.canMove(ti.location) && ti.location.distanceTo(targetRobot.location) < 8f
                  && clearShot(ti.location, targetRobot.location)) {
                float newDist = here.distanceTo(ti.location);
                if (newDist < optimalDist) {
                  optimalLoc = ti.location;
                  optimalDist = newDist;
                }
              }
            }
          }
          if (optimalLoc != null) {
            rc.move(optimalLoc);
          }
          else {
            boolean currentlyHasClearShot = clearShot(here, targetRobot.location);
            Direction rotated30 = direction.opposite().rotateLeftDegrees(30);
            MapLocation newLoc = targetRobot.location.add(rotated30, KEEPAWAY_RADIUS);
            if (rc.canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
              if (currentlyHasClearShot && clearShot(newLoc, targetRobot.location)) {
                //System.out.println("d");
                rc.move(newLoc);
              }
              else if (!currentlyHasClearShot) {
                rc.move(newLoc);
              }
            }
            else {
              rotated30 = direction.opposite().rotateRightDegrees(30);
              newLoc = targetRobot.location.add(rotated30, KEEPAWAY_RADIUS);
              if (rc.canMove(newLoc) && isLocationSafe(nearbyBullets, newLoc)) {
                if (currentlyHasClearShot && clearShot(newLoc, targetRobot.location)) {
                  //System.out.println("e");
                  rc.move(newLoc);
                }
                else if (!currentlyHasClearShot) {
                  rc.move(newLoc);
                }
              }
            }
          }
        }
      }
    }
    Globals.update();
    direction = here.directionTo(targetRobot.location);
    if (shouldShoot && rc.canFireSingleShot() && clearShot(here, targetRobot.location)) {
      rc.fireSingleShot(direction);
      if (DEBUG) {
        System.out.println("CLEARSHOT!");
        rc.setIndicatorDot(targetRobot.location, (us == Team.A) ? 255 : 0, 0,
            (us == Team.B ? 255 : 0));
      }
    }
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
      if (rc.getRoundNum() < 100) {
        findSquad();
        MapLocation[] enemies = rc.getInitialArchonLocations(them);
        MapLocation first = enemies[0];
        targetDirection = here.directionTo(first);
      }
      // Later scouts move in random directions
      else {
        findSquad();
        targetDirection = RobotUtils.randomDirection();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    while (true) {
      try {
        if (current_mode == ROAM) {
          Globals.update();
          System.out.println("Roaming");
          //rc.setIndicatorDot(here, 0, 0, 255);
          // Look for target in broadcast
          int target = rc.readBroadcast(squad_channel + 1);
          if (target != 0) {
            System.out.println("Found target in broadcast");
            current_mode = ATTACK;
            int xLoc = rc.readBroadcast(squad_channel + 2);
            int yLoc = rc.readBroadcast(squad_channel + 3);
            targetDirection = here.directionTo(new MapLocation(xLoc, yLoc));
            BulletInfo[] nearbyBullets = rc.senseNearbyBullets(EvasiveScout.BULLET_DETECT_RADIUS);
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(KEEPAWAY_RADIUS, them);
            if (nearbyBullets != null && nearbyBullets.length != 0
                || nearbyRobots != null && nearbyRobots.length != 0) {
              EvasiveScout.move(nearbyBullets, nearbyRobots);
            }
            else if (rc.canMove(targetDirection)) {
              //System.out.println("f");
              rc.move(targetDirection);
            }
          }
          else {
            System.out.println("Searching for target");
            alert();
            if (!rc.hasMoved()) {
              System.out.println("Has not moved");
              // Move towards target in a straight line
              // TODO better pathfinding
              if (rc.canMove(targetDirection)) {
                //System.out.println("g");
                System.out.println("Moving towards target");
                rc.move(targetDirection);
              }
              else if (!rc.onTheMap(here.add(targetDirection,
                  RobotType.SCOUT.strideRadius + RobotType.SCOUT.bodyRadius))) {
                // Change direction when hitting border,
                // Note: should not happen when chasing a newly found target
                targetDirection = targetDirection
                    .rotateRightRads((float) (rand.nextFloat() * Math.PI));
                System.out.println("Turning randomly " + targetDirection);
                if (rc.canMove(targetDirection)) {
                  rc.move(targetDirection);
                }
              }
              else {
                System.out.println("Can't move towards targetDirection: " + targetDirection);
                RobotUtils.tryMove(targetDirection, 20, 6);
              }
            }
            // If mode changed, yield before beginning attack logic
            if (current_mode == ATTACK) {
              System.out.println("Changed from ROAM to ATTACK");
              Clock.yield();
            }
          }
        }
        if (current_mode == ATTACK) {
          Globals.update();
          System.out.println("ATTACK");
          // Currently on attack mode
          int target = rc.readBroadcast(squad_channel + 1);
          // Read assigned target from broadcast
          attackTarget = target;
          if (rc.canSenseRobot(target)) {
            // Engage target if it is in range
            System.out.println("Can sense target");
            engagingTarget = true;
            // TODO fix disengagement
            // Allow re-engagement on priority targets
            // TODO unroll loop and re-position death tracking
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
          else {
            System.out.println("Cannot sense target");
            // We are out of range of our target,
            // so try to move in known direction of target to find target
            if (engagingTarget) {
              engagingTarget = false;
              // Target is assumed to be killed, so update broadcast target
              if (!priorityTarget && attackTarget == target) {
                System.out.println("Target killed");
                int broadcastTarget = rc.readBroadcast(squad_channel + 1);
                if (broadcastTarget == target) {
                  rc.broadcast(squad_channel + 1, 0);
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
            int broadcastTarget = rc.readBroadcast(squad_channel + 1);
            if (broadcastTarget != 0) {
              System.out.println("Moving towards target");
              int xLoc = rc.readBroadcast(squad_channel + 2);
              int yLoc = rc.readBroadcast(squad_channel + 3);
              MapLocation targetLoc = new MapLocation(xLoc, yLoc);
              float distToTarget = here.distanceTo(targetLoc);
              if (distToTarget < RobotType.ARCHON.sensorRadius && !rc.canSenseRobot(broadcastTarget)) {
                rc.broadcast(squad_channel + 1, 0);
              }
              else {
                targetDirection = here.directionTo(targetLoc);
                if (!rc.hasMoved() && rc.canMove(targetDirection)) {
                  //System.out.println("j");
                  rc.move(targetDirection);
                }
              }
            }
            // Disengage if no target
            else {
              System.out.println("Disengaging: no boradcast target");
              current_mode = ROAM;
              targetDirection = RobotUtils.randomDirection();
              if (!rc.hasMoved() && rc.canMove(targetDirection)) {
                //System.out.println("k");
                rc.move(targetDirection);
              }
              //System.out.println(direction.getAngleDegrees());
            }
          }
        }
        if (!hasReportedDeath && rc.getHealth() < 3f) {
          int squad_count = rc.readBroadcast(squad_channel);
          hasReportedDeath = true;
          rc.broadcast(squad_channel, squad_count - 1);
        }
        RobotUtils.donateEverythingAtTheEnd();
        Clock.yield();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}