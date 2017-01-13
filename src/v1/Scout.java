package v1;

import battlecode.common.*;

public class Scout extends Globals {

  private final static int ROAM = 0;
  private final static int ATTACK = 1;
  private static int current_mode = ROAM;
  private static final int KEEPAWAY_RADIUS = 3;
  private static Direction targetDirection = null;
  private static int squad_channel;

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
      if (RobotPlayer.willCollideWithMe(b)) {
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
    while (i < 900) {
      int squad_count = rc.readBroadcast(i);
      //System.out.println(squad_count);
      if (squad_count < 5) {
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
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(KEEPAWAY_RADIUS, them);
    if ((nearbyBullets != null && nearbyBullets.length != 0)
        || (nearbyRobots != null && nearbyRobots.length != 0)) {
      EvasiveScout.move(nearbyBullets, nearbyRobots);
    }
    if (nearbyRobots == null || nearbyRobots.length == 0) {
      return;
    }
    else {
      if (rc.getRoundNum() < 200) {
        for (RobotInfo enemy : nearbyRobots) {
          if (enemy.getType() != RobotType.ARCHON) {
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
        /*
        RobotInfo enemy = null;
        // Preferred targets
        for (RobotInfo ri : nearbyRobots) {
          if (ri.type == RobotType.GARDENER) {
            enemy = ri;
            break;
          }
        }
        if (enemy == null) {
          enemy = nearbyRobots[0];
        }
        */
        RobotInfo enemy = nearbyRobots[0];
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
  public static boolean clearShot(MapLocation shooterLoc, MapLocation target) {
    Direction targetDir = shooterLoc.directionTo(target);
    float distanceTarget = shooterLoc.distanceTo(target);
    RobotInfo[] friendlies = rc.senseNearbyRobots(distanceTarget, us);
    MapLocation outerEdge = shooterLoc.add(targetDir, RobotType.SCOUT.bodyRadius + 0.1f);
    for (RobotInfo r : friendlies) {
      if (RobotPlayer.willCollideWithTargetLocation(outerEdge, targetDir, r.location,
          r.getRadius())) {
        return false;
      }
    }
    TreeInfo[] trees = rc.senseNearbyTrees(distanceTarget);
    for (TreeInfo t : trees) {
      if (RobotPlayer.willCollideWithTargetLocation(outerEdge, targetDir, t.location,
          t.getRadius())) {
        return false;
      }
    }
    return true;
  }

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
      if (RobotPlayer.willCollideWithTargetLocation(bi.location, bi.dir, loc, myType.bodyRadius)) {
        return false;
      }
    }
    return true;
  }
  
  public static void engage(int target) throws GameActionException {
    RobotInfo targetRobot = rc.senseRobot(target);
    rc.broadcast(squad_channel + 1, targetRobot.ID);
    rc.broadcast(squad_channel + 2, (int) targetRobot.location.x);
    rc.broadcast(squad_channel + 3, (int) targetRobot.location.y);
    Direction direction = here.directionTo(targetRobot.location);
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets(EvasiveScout.BULLET_DETECT_RADIUS);
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(EvasiveScout.LUMBERJACK_DETECT_RADIUS, them);
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
            System.out.println("c");
            rc.move(newLoc);
          }
        }
      }
      else {
        Direction rotated20 = direction.opposite().rotateLeftDegrees(20);
        MapLocation newLoc = targetRobot.location.add(rotated20, KEEPAWAY_RADIUS);
        boolean locIsSafe = true;
        if (rc.canMove(newLoc)) {
          if (clearShot(newLoc, targetRobot.location) && isLocationSafe(nearbyBullets, newLoc)) {
            System.out.println("d");
            rc.move(newLoc);
          }
          else {
            shouldShoot = false;
          }
        }
        else {
          rotated20 = direction.opposite().rotateRightDegrees(20);
          newLoc = targetRobot.location.add(rotated20, KEEPAWAY_RADIUS);
          if (rc.canMove(newLoc)) {
            if (clearShot(newLoc, targetRobot.location) && isLocationSafe(nearbyBullets, newLoc)) {
              System.out.println("e");
              rc.move(newLoc);
            }
            else {
              shouldShoot = false;
            }
          }
        }
      }
    }
    Globals.update();
    direction = here.directionTo(targetRobot.location);
    if (shouldShoot && rc.canFireSingleShot() && clearShot(here, targetRobot.location)) {
      System.out.println("CLEARSHOT!");
      rc.fireSingleShot(direction);
      /*
      for (BulletInfo bi : rc.senseNearbyBullets()) {
        System.out.println(bi.location);
      }
      */
      rc.setIndicatorDot(targetRobot.location, 255, 0, 0);
    }
  }

  public static void loop() {
    try {
      Globals.update();
      EvasiveScout.init();
      // Early scouts should target the archon
      if (rc.getRoundNum() < 100) {
        squad_channel = 100;
        MapLocation[] enemies = rc.getInitialArchonLocations(them);
        MapLocation first = enemies[0];
        targetDirection = here.directionTo(first);
      }
      // Later scouts divide into squads
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
                Direction newDir = targetDirection.rotateRightDegrees(10);
                while (!rc.canMove(newDir)) {
                  newDir = newDir.rotateRightDegrees(10);
                }
                targetDirection = newDir;
                //System.out.println("h");
                rc.move(targetDirection);
                //System.out.println(direction.getAngleDegrees());
              }
            }
          }
        }
        if (current_mode == ATTACK) {
          // Currently on attack mode
          //rc.setIndicatorDot(here, 0, 255, 0);
          int target = rc.readBroadcast(squad_channel + 1);
          if (rc.canSenseRobot(target)) {
            // TODO fix disengagement
            while (rc.canSenseRobot(target)) {
              Globals.update();
              engage(target);
              Clock.yield();
            }
            rc.broadcast(squad_channel + 1, 0);
            current_mode = ROAM;
            targetDirection = new Direction((float) (Math.random() * 2 * Math.PI));
            if (!rc.hasMoved() && rc.canMove(targetDirection)) {
              //System.out.println("i");
              rc.move(targetDirection);
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