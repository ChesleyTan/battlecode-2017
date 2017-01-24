package finalVersion;

import battlecode.common.*;
import utils.Globals;
import utils.RobotUtils;
import utils.TargetingUtils;

public class Soldier extends Globals {

  private static final int ATTACK = 1;
  private static final int DEFEND = 2;
  private static final int ROAM = 3;
  private static int squad_channel;
  private static int mode;
  private static Direction mydir;
  private static RobotInfo target;
  private static MapLocation enemyArchonLocation;
  private static final boolean SOLDIER_DEBUG = true;

  /*
  private static void dodge(BulletInfo[] bullets, RobotInfo[] robots, MapLocation targetLocation)
      throws GameActionException {
    float sumX = 0;
    float sumY = 0;
    if (targetLocation != null) {
      Direction toTarget = here.directionTo(targetLocation);
      float distTarget = here.distanceTo(targetLocation);
      sumX = toTarget.getDeltaX(distTarget / myType.sensorRadius * myType.strideRadius);
      sumY = toTarget.getDeltaY(distTarget / myType.sensorRadius * myType.strideRadius);
    }
    for (BulletInfo i : bullets) {
      MapLocation endLocation = i.location.add(i.getDir(), i.getSpeed());
      float x0 = i.location.x;
      float y0 = i.location.y;
      float x1 = endLocation.x;
      float y1 = endLocation.y;
      float a = y0 - y1;
      float b = x1 - x0;
      if (a == 0 && b == 0) {
        a = 0.01f;
      }
      float c = x0 * y1 - y0 * x1;
      float distance = (float) (Math.abs(a * here.x + b * here.y + c) / Math.sqrt(a * a + b * b));
      if (distance <= 2.5) {
        float x2 = (float) ((b * (b * here.x - a * here.y) - a * c) / (a * a + b * b));
        float y2 = (float) ((a * (a * here.y - b * here.x) - b * c)
            / (Math.pow(a, 2) + Math.pow(b, 2)));
        MapLocation destLocation = new MapLocation(x2, y2);
        Direction away = destLocation.directionTo(here);
        if (away == null) {
          away = here.directionTo(i.getLocation()).rotateLeftDegrees(90);
        }
        System.out.println("distance: " + distance);
        float weighted = (float) Math
            .pow((RobotType.SOLDIER.bulletSightRadius - distance / myType.bulletSightRadius), 2);
        //float weighted = RobotType.SOLDIER.bulletSightRadius / distance;
        System.out.println("weighted: " + weighted);
        rc.setIndicatorDot(here.add(away, 1), 255, 0, 0);
        sumX += away.getDeltaX(weighted);
        sumY += away.getDeltaY(weighted);
      }
    }
  
    for (RobotInfo r : robots) {
      Direction their_direction = r.location.directionTo(here);
      float their_distance = ((RobotType.SOLDIER.sensorRadius - here.distanceTo(r.location)
          + r.getRadius()))
          * ((RobotType.SOLDIER.sensorRadius - here.distanceTo(r.location) + r.getRadius()));
      System.out.println(their_distance);
      if (r.getTeam() == us) {
        their_distance = their_distance / 2;
      }
      sumX += their_direction.getDeltaX(their_distance);
      sumY += their_direction.getDeltaY(their_distance);
    }
  
    TreeInfo[] nearbyTrees = rc.senseNearbyTrees(GameConstants.NEUTRAL_TREE_MAX_RADIUS);
    if (nearbyTrees.length <= 10) {
      for (TreeInfo t : nearbyTrees) {
        Direction their_direction = t.location.directionTo(here);
        float baseValue = ((myType.sensorRadius - here.distanceTo(t.location) + t.getRadius()))
            * ((myType.sensorRadius - here.distanceTo(t.location) + t.getRadius()));
        float their_distance = baseValue * myType.strideRadius;
        sumX += their_direction.getDeltaX(their_distance);
        sumY += their_direction.getDeltaY(their_distance);
      }
    }
  
    if (Clock.getBytecodesLeft() >= 2000) {
      float sightRadius = RobotType.SOLDIER.sensorRadius - 1;
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
    //float finaldist = (float) Math.sqrt(sumX * sumX + sumY * sumY);
  
    Direction finalDir = new Direction(sumX, sumY);
    RobotUtils.tryMove(finalDir, 10, 6);
  }
  */

  private static void findSquad() throws GameActionException {
    int i = DEFENSE_START_CHANNEL;
    while (i < DEFENSE_END_CHANNEL) {
      int squad_count = rc.readBroadcast(i);
      if (SOLDIER_DEBUG) {
        System.out.println(squad_count);
      }
      if (squad_count == 0) {
        // Clear out target field
        rc.broadcast(i + 1, -1);
      }
      if (squad_count < 3) {
        squad_channel = i;
        rc.broadcast(i, squad_count + 1);
        return;
      }
      i += DEFENSE_BLOCK_WIDTH;
    }
    squad_channel = DEFENSE_START_CHANNEL;
  }

  /*private static void moveAroundTree(RobotInfo target) throws GameActionException{
    Direction toEnemy = here.directionTo(target.location);
    if (rc.canMove(toEnemy)){
      if (!RobotUtils.tryMove(toEnemy, 15, 6)){
        TreeInfo[] nearbyTrees = rc.senseNearbyTrees(1);
        if (nearbyTrees.length != 0){
          MapLocation targetLocation = here.add(toEnemy);
          for(TreeInfo t: nearbyTrees){
            if (targetLocation.distanceTo(tree.location) < t.getRadius() + myType.bodyRadius){
              while()
            }
          }
        }
      }
    }
  }*/
  private static boolean blockedByTree(BulletInfo i, TreeInfo[] trees) {
    Direction base = here.directionTo(i.location);
    float baseDistance = here.distanceTo(i.location);
    for (TreeInfo tree : trees) {
      if (i.location.distanceTo(tree.location) > baseDistance) {
        continue;
      }
      Direction t = here.directionTo(tree.location);
      float radians = Math.abs(t.radiansBetween(base));
      float dist = (float) Math.sin(radians) * here.distanceTo(tree.location);
      if (dist < tree.getRadius()) {
        return true;
      }
    }
    return false;
  }

  private static void move(BulletInfo[] bullets, RobotInfo[] robots, MapLocation destination)
      throws GameActionException {
    EvasiveSoldier.move(bullets, robots, target, destination);
  }

  /*
   * Assumes target has already been seen and is a local variable
   * make sure whenever this is called, person has not moved yet
   */
  private static void attack(RobotInfo target) throws GameActionException {
    if (SOLDIER_DEBUG) {
      System.out.println("attacking");
    }
    BulletInfo[] bullets = rc.senseNearbyBullets(EvasiveSoldier.BULLET_DETECT_RADIUS);
    RobotInfo[] robots = rc.senseNearbyRobots(EvasiveSoldier.ENEMY_DETECT_RADIUS);
    MapLocation targetLocation = target.getLocation();
    move(bullets, robots, targetLocation);
    //RobotUtils.tryMoveDestination(targetLocation);
    if (TargetingUtils.clearShot(here, target) || (rc.getType() == RobotType.GARDENER && rc.getOpponentVictoryPoints() > 10)) {
      if (SOLDIER_DEBUG) {
        System.out.println("clearShot to target");
      }
      Direction towardsEnemy = here.directionTo(targetLocation);
      float distanceEnemy = here.distanceTo(targetLocation);
      if (distanceEnemy <= 3.5 && rc.canFirePentadShot() && rc.getTeamBullets() > 200) {
        rc.firePentadShot(towardsEnemy);
      }
      else {
        if (rc.canFireTriadShot() && rc.getTeamBullets() > 50) {
          rc.fireTriadShot(towardsEnemy);
        }
        else if (rc.canFireSingleShot()) {
          rc.fireSingleShot(towardsEnemy);
        }
      }
    }
  }

  /* Moves in the direction of attacking and fires if reasonable
   * Assumes soldier has neither moved nor attacked
   * Also assumes that there is a target
   */
  private static void moveToAttack(MapLocation destination) throws GameActionException {
    BulletInfo[] bullets = rc.senseNearbyBullets();
    RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);

    if (enemies.length != 0) {
      int index = priority(enemies);
      if (index != -1) {
        target = enemies[priority(enemies)];
        attack(target);
      }
    }
    else {
      move(bullets, enemies, destination);
    }
  }

  private static void roam() throws GameActionException {
    int targetID = rc.readBroadcast(squad_channel + 1);
    if (targetID != -1) {
      mode = ATTACK;
      int xCor = rc.readBroadcast(squad_channel + 2);
      int yCor = rc.readBroadcast(squad_channel + 3);
      MapLocation destination = new MapLocation(xCor, yCor);
      moveToAttack(destination);
    }
    else {
      RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
      //BulletInfo[] bullets = rc.senseNearbyBullets();

      int priority = priority(enemies);
      if (priority != -1) {
        target = enemies[priority];
        if (SOLDIER_DEBUG) {
          System.out.println(target.ID);
        }
        mode = ATTACK;
        attack(target);
      }
      else {
        int cacheTarget = rc.readBroadcast(GARDENER_TARGET_CACHE_CHANNEL);
        if (cacheTarget != 0) {
          if (SOLDIER_DEBUG) {
            System.out.println("Using target from gardener cache: " + cacheTarget);
          }
          int data = rc.readBroadcast(GARDENER_TARGET_CACHE_CHANNEL + 1);
          int cacheTargetX = readGardenerCacheX(data);
          int cacheTargetY = readGardenerCacheY(data);
          rc.broadcast(squad_channel + 1, cacheTarget);
          rc.broadcast(squad_channel + 2, cacheTargetX);
          rc.broadcast(squad_channel + 3, cacheTargetY);
          rc.broadcast(GARDENER_TARGET_CACHE_CHANNEL, 0);
          moveToAttack(new MapLocation(cacheTargetX, cacheTargetY));
        }
        else {
          if (!rc.hasMoved()) {
            int attempts = 0;
            while (!rc.canMove(mydir) && attempts < 20) {
              if (Clock.getBytecodesLeft() < 2000) {
                break;
              }
              mydir = RobotUtils.randomDirection();
              attempts++;
            }
            if (rc.canMove(mydir)) {
              rc.move(mydir);
            }
          }
        }
      }
    }
  }

  /*private static void survey(RobotInfo target) throws GameActionException{
    System.out.println("defending target");
    RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
    if (enemies.length != 0){
      int priority = priority(enemies);
      attack(enemies[priority]);
    }
    else{
      Direction towardsEnemy = target.getLocation().directionTo(enemyLocation);
      MapLocation destination = target.location.add(towardsEnemy, 5);
      float distance = here.distanceTo(destination);
      if (distance < 6){
        destination = null;
      }
      RobotUtils.tryMoveDestination(destination);
    }
  }*/
  /*
   * Defend the person who's considered "target"
   */
  private static void defend() throws GameActionException {
    // We're assuming the person hasn't found the target yet
    if (target == null) {
      int targetID = rc.readBroadcast(squad_channel + 1);
      if (targetID == -1) {
        mode = ROAM;
        roam();
      }
      else {
        // If he can see the person he's supposed to attack
        if (rc.canSenseRobot(targetID)) {
          if (SOLDIER_DEBUG) {
            System.out.println("acquired target");
          }
          target = rc.senseRobot(targetID);
          mode = ATTACK;
          rc.broadcast(squad_channel + 2, (int) target.location.x);
          rc.broadcast(squad_channel + 3, (int) target.location.y);
          attack(target);
        }
        else {
          //Either he's not in the vicinity or he died
          if (SOLDIER_DEBUG) {
            System.out.println("Moving to target");
          }
          int xCor = rc.readBroadcast(squad_channel + 2);
          int yCor = rc.readBroadcast(squad_channel + 3);
          MapLocation targetLocation = new MapLocation(xCor, yCor);
          if (here.distanceTo(targetLocation) <= RobotType.SOLDIER.sensorRadius
              + MIN_ROBOT_RADIUS) {
            // he's dead
            rc.broadcast(squad_channel + 1, -1);
            target = null;
            mode = ROAM;
            roam();
          }
          else {
            // I have to find him
            //BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
            //RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, them);
            //EvasiveSoldier.move(nearbyBullets, nearbyRobots, here.directionTo(targetLocation), targetLocation);
            RobotUtils.tryMoveDestination(targetLocation);
          }
        }
      }
    }
    else {
      if (SOLDIER_DEBUG) {
        System.out.println("attacking target: " + target.ID);
      }
      int broadcasted = rc.readBroadcast(squad_channel + 1);
      if (broadcasted != target.ID) {
        target = null;
      }
      else {
        if (rc.canSenseRobot(target.ID)) {
          target = rc.senseRobot(target.ID);
          attack(target);
        }
        else {
          if (here.distanceTo(target.getLocation()) <= 3) {
            // he's dead
            rc.broadcast(squad_channel + 1, -1);
            target = null;
            mode = ROAM;
            roam();
          }
          else {
            RobotUtils.tryMove(here.directionTo(target.location), 15, 8);
          }
        }
      }
    }
  }

  private static int value(RobotType r) {
    switch (r) {
      case SCOUT:
        return 5;
      case GARDENER:
        return 4;
      case LUMBERJACK:
        return 3;
      case SOLDIER:
        return 2;
      case ARCHON:
        return -1;
      case TANK:
        return 0;
      default:
        return -1;
    }
  }

  public static int priority(RobotInfo[] enemies) {
    int result = -1;
    int currValue = -1;
    for (int i = 0; i < enemies.length; i++) {
      int value = 0;
      if (enemies[i] == null) {
        continue;
      }
      switch (enemies[i].getType()) {
        case GARDENER:
          value = 2;
          break;
        case SCOUT:
          value = 3;
          break;
        case ARCHON:
          value = 0;
          break;
        case SOLDIER:
          value = 4;
          break;
        case LUMBERJACK:
          value = 4;
          break;
        case TANK:
          value = 0;
          break;
      }
      if (SOLDIER_DEBUG) {
        System.out.println("Value: " + value);
      }
      if (value > currValue) {
        currValue = value;
        result = i;
      }
    }
    return result;
  }

  public static void loop() {
    try {
      //Start out and join a squad
      enemyArchonLocation = rc.getInitialArchonLocations(them)[0];
      findSquad();
      if (rc.getRoundNum() < 100) {
        rc.broadcast(squad_channel + 1, 0);
        rc.broadcast(squad_channel + 2, (int) enemyArchonLocation.x);
        rc.broadcast(squad_channel + 3, (int) enemyArchonLocation.y);
        mydir = here.directionTo(enemyArchonLocation);
        mode = ATTACK;
      }
      else {
        mydir = RobotUtils.randomDirection();
        mode = ROAM;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    while (true) {
      Globals.update();
      try {
        if (SOLDIER_DEBUG) {
          System.out.println(mode);
        }
        if (mode == ATTACK) {
          if (target == null) {
            //hasn't found him or he recently died
            int targetID = rc.readBroadcast(squad_channel + 1);
            if (targetID == -1) {
              if (SOLDIER_DEBUG) {
                System.out.println("null target, roaming");
              }
              mode = ROAM;
              roam();
            }
            else {
              // if there is a target requested but not yet seen
              if (SOLDIER_DEBUG) {
                System.out.println("null target, moving to attack");
              }
              int xCor = rc.readBroadcast(squad_channel + 2);
              int yCor = rc.readBroadcast(squad_channel + 3);
              MapLocation destination = new MapLocation(xCor, yCor);
              if (SOLDIER_DEBUG) {
                System.out.println(destination);
              }
              if (here.distanceTo(destination) <= RobotType.SOLDIER.sensorRadius
                  + MIN_ROBOT_RADIUS) {
                // Disengage, because target could not be found at last known location
                mode = ROAM;
                rc.broadcast(squad_channel + 1, -1);
                roam();
              }
              else {
                moveToAttack(destination);
              }
            }
          }
          else {
            if (SOLDIER_DEBUG) {
              System.out.println("target ID: " + target.ID);
            }
            // if target != null
            if (rc.canSenseRobot(target.ID)) {
              RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
              target = enemies[priority(enemies)];
              rc.broadcast(squad_channel + 1, target.ID);
              rc.broadcast(squad_channel + 2, (int) target.location.x);
              rc.broadcast(squad_channel + 3, (int) target.location.y);
              attack(target);
            }
            else {
              int ogTarget = rc.readBroadcast(squad_channel + 1);
              if (target.ID == ogTarget) {
                int xCor = rc.readBroadcast(squad_channel + 2);
                int yCor = rc.readBroadcast(squad_channel + 3);
                MapLocation targetLocation = new MapLocation(xCor, yCor);
                if (here.distanceTo(targetLocation) <= RobotType.SOLDIER.sensorRadius
                    + MIN_ROBOT_RADIUS) {
                  // Disengage, because target could not be found at last known location
                  target = null;
                  mode = ROAM;
                  rc.broadcast(squad_channel + 1, -1);
                  roam();
                }
                else {
                  moveToAttack(targetLocation);
                }
              }
              else {
                //moveToDefend(ogTarget);
                target = null;
              }
            }
          }
        }
        else if (mode == DEFEND) {
          defend();
        }
        else if (mode == ROAM) {
          roam();
          // check defense every turn, if so then head to defend target
        }
        RobotUtils.donateEverythingAtTheEnd();
        RobotUtils.shakeNearbyTrees();
        trackEnemyGardeners();
        RobotUtils.notifyBytecodeLimitBreach();
        Clock.yield();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}