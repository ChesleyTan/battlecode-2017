package v2_scouts;

import battlecode.common.*;
import utils.Globals;
import utils.RobotUtils;


import static utils.TargetingUtils.clearShot;

public class Scout extends Globals {

  private final static int ROAM = 0;
  private final static int ATTACK = 1;
  private static int current_mode = ROAM;
  private static final float KEEPAWAY_RADIUS = 3f;
  private static final float GARDENER_KEEPAWAY_RADIUS = 2.01f;
  private static Direction[] GARDENER_PENETRATION_ANGLES = new Direction[6];
  private static Direction targetDirection = null;
  private static int squad_channel;
  private static int attackTarget;

  public static void dodge(BulletInfo[] nearbyBullets, RobotInfo[] nearbyRobots)
      throws GameActionException {
    boolean willHit = false;
    MapLocation[] startLocs = new MapLocation[nearbyBullets.length];
    MapLocation[] endLocs = new MapLocation[nearbyBullets.length];
    int index = 0;
    for (BulletInfo b : nearbyBullets) {
      MapLocation finalLoc = b.location.add(b.dir, b.speed);
      startLocs[index] = b.location;
      endLocs[index] = finalLoc;
      index++;
      /*
      float dist = (float)(Math.sqrt(Math.pow(here.x - finalLoc.x, 2) + Math.pow(here.y - finalLoc.y, 2)));
      if(dist < RobotType.SCOUT.bodyRadius){
        willHit = true;
      }*/
      if (RobotUtils.willCollideWithMe(b)) {
        willHit = true;
      }
    }
    float sumX = 0;
    float sumY = 0;
    if (willHit) {
      for (int i = 0; i < index; i++) {
        float x0 = startLocs[i].x;
        float y0 = startLocs[i].y;
        float x1 = endLocs[i].x;
        float y1 = endLocs[i].y;
        float a = x1 - x0;
        float b = y0 - y1;
        float c = x0 * y1 - y0 * x1;
        float distance = (float) (Math.abs(a * here.x + b * here.y + c)
            / Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2)));
        float x2 = (float) ((b * (b * here.x - a * here.y) - a * c)
            / (Math.pow(a, 2) + Math.pow(b, 2)));
        float y2 = (float) ((a * (a * here.y - b * here.x) - b * c)
            / (Math.pow(a, 2) + Math.pow(b, 2)));
        Direction away = here.directionTo(new MapLocation(x2, y2)).opposite();
        float weighted = (RobotType.SCOUT.bulletSightRadius - distance)
            / RobotType.SCOUT.bulletSightRadius * RobotType.SCOUT.strideRadius;
        sumX += away.getDeltaX(weighted);
        sumY += away.getDeltaY(weighted);
      }
    }
    for (RobotInfo r : nearbyRobots) {
      if (r.getType() == RobotType.LUMBERJACK) {
        Direction their_direction = here.directionTo(r.location).opposite();
        float their_distance = (RobotType.SCOUT.sensorRadius - here.distanceTo(r.location))
            / RobotType.SCOUT.sensorRadius * RobotType.SCOUT.strideRadius;
        sumX += their_direction.getDeltaX(their_distance);
        sumY += their_direction.getDeltaY(their_distance);
        index++;
      }
    }
    float finaldist = (float) Math.sqrt(Math.pow(sumX, 2) + Math.pow(sumY, 2));
    if (finaldist <= RobotType.SCOUT.strideRadius) {
      MapLocation destination = new MapLocation(here.x + sumX, here.y + sumY);
      if (rc.canMove(destination) && !rc.hasMoved()) {
        //System.out.println("a");
        rc.move(destination);
      }
    }
    else {
      Direction finalDir = new Direction(sumX, sumY);
      if (rc.canMove(finalDir) && !rc.hasMoved()) {
        //System.out.println("b");
        rc.move(finalDir);
      }
    }
  }

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
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets(EvasiveScout.BULLET_DETECT_RADIUS);
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, them);
    if ((nearbyBullets != null && nearbyBullets.length != 0)
        || (nearbyRobots != null && nearbyRobots.length != 0)) {
      EvasiveScout.move(nearbyBullets, nearbyRobots);
    }
    if (nearbyRobots == null || nearbyRobots.length == 0) {
      return;
    }
    else {
      RobotInfo[] nearbyFriendlies = rc.senseNearbyRobots(-1, us);
      boolean friendlyGardener = false;
      for (RobotInfo ri : nearbyFriendlies) {
        if (ri.type == RobotType.GARDENER) {
          friendlyGardener = true;
          break;
        }
      }
      if (rc.getRoundNum() < 200) {
        for (RobotInfo enemy : nearbyRobots) {
          // TODO also prioritize defending our gardeners
          if ((enemy.type == RobotType.SCOUT && friendlyGardener) || enemy.type == RobotType.GARDENER) {
            rc.broadcast(squad_channel + 1, enemy.ID);
            rc.broadcast(squad_channel + 2, (int) (enemy.location.x));
            rc.broadcast(squad_channel + 3, (int) (enemy.location.y));
            MapLocation center = enemy.location;
            if (rc.canFireSingleShot() && clearShot(here, enemy.location)) {
              rc.fireSingleShot(here.directionTo(center));
            }
            current_mode = ATTACK;
            break;
          }
          else {
            return;
          }
        }
      }
      else {
        RobotInfo enemy = null;
        // Preferred targets
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
        if (enemy.type == RobotType.ARCHON && rc.getRoundNum() < 1000) {
          return;
        }
        rc.broadcast(squad_channel + 1, enemy.ID);
        rc.broadcast(squad_channel + 2, (int) (enemy.location.x));
        rc.broadcast(squad_channel + 3, (int) (enemy.location.y));
        MapLocation center = enemy.location;
        if (rc.canFireSingleShot() && clearShot(here, center)) {
          rc.fireSingleShot(here.directionTo(center));
          rc.setIndicatorDot(center, 255, 0, 0);
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

  private static boolean isLocationSafe(BulletInfo[] nearbyBullets, MapLocation loc) {
    for (BulletInfo bi : nearbyBullets) {
      if (RobotUtils.willCollideWithTargetLocation(bi.location, bi.dir, loc, myType.bodyRadius)) {
        return false;
      }
    }
    return true;
  }

  public static RobotInfo engage(int target, boolean forceReEngage) throws GameActionException {
    RobotInfo targetRobot = rc.senseRobot(target);
    int broadcastTarget = rc.readBroadcast(squad_channel + 1);
    if (!forceReEngage) {
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
        MapLocation newLoc = here.add(direction);
        if (rc.canMove(newLoc)) {
          if (isLocationSafe(nearbyBullets, newLoc)) {
            //System.out.println("c");
            rc.move(newLoc);
          }
        }
      }
      else {
        if (targetRobot.type == RobotType.GARDENER) {
          MapLocation optimalLoc = null;
          for (int i = 0; i < GARDENER_PENETRATION_ANGLES.length; ++i) {
            MapLocation newLoc = targetRobot.location.add(GARDENER_PENETRATION_ANGLES[i],
                GARDENER_KEEPAWAY_RADIUS);
            // TODO optimize
            /*
            System.out.println(rc.canMove(newLoc));
            System.out.println(clearShot(newLoc, targetRobot.location));
            */
            float optimalDist = 9999f;
            float newDist = here.distanceTo(newLoc);
            if (rc.canMove(newLoc) && newDist < optimalDist
                && clearShot(newLoc, targetRobot.location)) {
              optimalLoc = newLoc;
              optimalDist = newDist;
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
        targetDirection = new Direction((float) (Math.random() * 2 * Math.PI));
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    while (true) {
      try {
        Globals.update();
        if (current_mode == ROAM) {
          //rc.setIndicatorDot(here, 0, 0, 255);
          // Look for target in broadcast
          int target = rc.readBroadcast(squad_channel + 1);
          if (target != 0) {
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
            alert();
            if (!rc.hasMoved()) {
              if (rc.canMove(targetDirection)) {
                //System.out.println("g");
                rc.move(targetDirection);
              }
              else if (!rc.onTheMap(here.add(targetDirection, RobotType.SCOUT.strideRadius))
                  || rc.senseNearbyRobots(2.5f, us) != null) {
                targetDirection = new Direction((float) (Math.random() * 2 * Math.PI));
                if (rc.canMove(targetDirection)) {
                  rc.move(targetDirection);
                }
                /*
                Direction newDir = targetDirection.rotateRightDegrees(10);
                while (!rc.canMove(newDir)) {
                  newDir = newDir.rotateRightDegrees(10);
                }
                targetDirection = newDir;
                //System.out.println("h");
                rc.move(targetDirection);
                */
                //System.out.println(direction.getAngleDegrees());
              }
            }
          }
        }
        if (current_mode == ATTACK) {
          // Currently on attack mode
          //rc.setIndicatorDot(here, 0, 255, 0);
          int target = rc.readBroadcast(squad_channel + 1);
          attackTarget = target;
          if (rc.canSenseRobot(target)) {
            // TODO fix disengagement
            boolean forceReEngage = false;
            while (rc.canSenseRobot(target) && (forceReEngage || attackTarget == target)) {
              Globals.update();
              RobotInfo targetRobot = engage(target, forceReEngage);
              if ((targetRobot != null && targetRobot.type != RobotType.GARDENER) || targetRobot == null) {
                RobotInfo[] nearbyRobots = rc.senseNearbyRobots(6, them);
                for (RobotInfo ri : nearbyRobots) {
                  if (ri.type == RobotType.GARDENER) {
                    target = ri.ID;
                    forceReEngage = true;
                    break;
                  }
                }
              }
              Clock.yield();
            }
            if (!forceReEngage && attackTarget == target) {
              rc.broadcast(squad_channel + 1, 0);
              current_mode = ROAM;
              targetDirection = new Direction((float) (Math.random() * 2 * Math.PI));
              if (!rc.hasMoved() && rc.canMove(targetDirection)) {
                //System.out.println("i");
                rc.move(targetDirection);
              }
            }
            else {
              int targetX = rc.readBroadcast(squad_channel + 2);
              int targetY = rc.readBroadcast(squad_channel + 3);
              MapLocation targetLoc = new MapLocation(targetX, targetY);
              targetDirection = here.directionTo(targetLoc);
              if (!rc.hasMoved() && rc.canMove(targetDirection)) {
                //System.out.println("i");
                rc.move(targetDirection);
              }
            }
          }
          else {
            // Find target
            if (rc.readBroadcast(squad_channel + 1) != 0) {
              int xLoc = rc.readBroadcast(squad_channel + 2);
              int yLoc = rc.readBroadcast(squad_channel + 3);
              targetDirection = here.directionTo(new MapLocation(xLoc, yLoc));
              if (!rc.hasMoved() && rc.canMove(targetDirection)) {
                //System.out.println("j");
                rc.move(targetDirection);
              }
            }
            else {
              current_mode = ROAM;
              targetDirection = new Direction((float) (Math.random() * 2 * Math.PI));
              if (!rc.hasMoved() && rc.canMove(targetDirection)) {
                //System.out.println("k");
                rc.move(targetDirection);
              }
              //System.out.println(direction.getAngleDegrees());
            }
          }
        }
        if (rc.getHealth() < 3f) {
          int squad_count = rc.readBroadcast(squad_channel);
          rc.broadcast(squad_channel, squad_count - 1);
        }
        Clock.yield();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}