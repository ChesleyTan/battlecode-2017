package finalVersion;

import battlecode.common.*;
import utils.Globals;
import utils.RobotUtils;
import utils.TargetingUtils;

public class Tank extends Globals {

  private static final int ATTACK = 1;
  private static final int DEFEND = 2;
  private static final int ROAM = 3;
  private static int squad_channel;
  private static int mode;
  private static Direction myDir;
  private static RobotInfo target;
  private static MapLocation enemyArchonLocation;
  private static final boolean TANK_DEBUG = true;
  private static boolean hasReportedDeath = false;
  private static boolean visitedEnemyArchon = true;

  private static void findSquad() throws GameActionException {
    int i = DEFENSE_START_CHANNEL;
    while (i < DEFENSE_END_CHANNEL) {
      int squad_count = rc.readBroadcast(i);
      if (squad_count == 0) {
        // Clear out target field
        rc.broadcast(i + 1, -1);
      }
      if (squad_count < 3) {
        squad_channel = i;
        if (TANK_DEBUG) {
          System.out.println("Squad channel: " + i);
        }
        rc.broadcast(i, squad_count + 1);
        return;
      }
      i += DEFENSE_BLOCK_WIDTH;
    }
    squad_channel = DEFENSE_START_CHANNEL;
  }
  
  private static boolean pentadShotGardener(TreeInfo treeBetween, MapLocation gardenerLocation){
    return (treeBetween != null && treeBetween.getTeam() == them);
  }

  /*
   * Assumes target has already been seen and is a local variable
   * make sure whenever this is called, person has not moved yet
   */
  private static void attack(RobotInfo target) throws GameActionException {
    if (TANK_DEBUG) {
      System.out.println("attacking");
    }
    MapLocation targetLocation = target.getLocation();
    Direction towardsEnemy = here.directionTo(targetLocation);
    TreeInfo treeBetween = rc
        .senseTreeAtLocation(here.add(towardsEnemy, RobotType.TANK.bodyRadius + 1));
    boolean pentadShotGardener = pentadShotGardener(treeBetween, targetLocation);
    if (TANK_DEBUG) {
      System.out.println("pentadshotgardener: " + pentadShotGardener);
    }
    RobotInfo[] robots = rc.senseNearbyRobots(EvasiveTank.ENEMY_DETECT_RADIUS, them);
    if (target.getType() != RobotType.GARDENER || !pentadShotGardener) {
      BulletInfo[] bullets = rc.senseNearbyBullets(EvasiveTank.BULLET_DETECT_RADIUS);
      EvasiveTank.move(bullets, robots, target, targetLocation);
    }
    //RobotUtils.tryMoveDestination(targetLocation);
    if (pentadShotGardener || TargetingUtils.clearShot(here, target)) {
      if (TANK_DEBUG) {
        System.out.println("clearShot to target");
      }
      float distanceEnemy = here.distanceTo(targetLocation);
      if ((distanceEnemy <= 6 || robots.length > 2) && rc.canFirePentadShot() && rc.getTreeCount() > 1) {
        rc.firePentadShot(towardsEnemy);
      }
      else {
        if (rc.canFireTriadShot()) {
          rc.fireTriadShot(towardsEnemy);
        }
        else if (rc.canFireSingleShot()) {
          rc.fireSingleShot(towardsEnemy);
        }
      }
    }
  }

  /* Moves in the direction of attacking and fires if reasonable
   * Assumes tank has neither moved nor attacked
   * Also assumes that there is a target
   */
  private static void moveToAttack(MapLocation destination) throws GameActionException {
    BulletInfo[] bullets = rc.senseNearbyBullets();
    RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
    if (enemies.length != 0) {
      int index = priority(enemies);
      if (index != -1) {
        target = enemies[index];
        attack(target);
      }
    }
    else {
      EvasiveTank.move(bullets, enemies, target, destination);
    }
  }

  private static void roam() throws GameActionException {
    int targetID = rc.readBroadcast(squad_channel + 1);
    if (targetID != -1) {
      if (TANK_DEBUG) {
        System.out.println("target id = -1");
      }
      mode = ATTACK;
      int xCor = rc.readBroadcast(squad_channel + 2);
      int yCor = rc.readBroadcast(squad_channel + 3);
      MapLocation destination = new MapLocation(xCor, yCor);
      moveToAttack(destination);
    }
    else {
      if (TANK_DEBUG) {
        System.out.println("sensing nearby enemies");
      }
      RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
      //BulletInfo[] bullets = rc.senseNearbyBullets();

      int priority = priority(enemies);
      if (priority != -1) {
        target = enemies[priority];
        if (TANK_DEBUG) {
          System.out.println(target.getID());
        }
        mode = ATTACK;
        attack(target);
      }
      else {
        int cacheTarget = rc.readBroadcast(GARDENER_TARGET_CACHE_CHANNEL);
        if (cacheTarget != 0) {
          if (TANK_DEBUG) {
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
            while (!RobotUtils.tankCanMove(myDir) && attempts < 20) {
              if (Clock.getBytecodesLeft() < 2000) {
                break;
              }
              myDir = RobotUtils.randomDirection();
              attempts++;
            }
            if (RobotUtils.tankCanMove(myDir)) {
              rc.move(myDir);
            }
          }
        }
      }
    }
  }

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
          if (TANK_DEBUG) {
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
          if (TANK_DEBUG) {
            System.out.println("Moving to target");
          }
          int xCor = rc.readBroadcast(squad_channel + 2);
          int yCor = rc.readBroadcast(squad_channel + 3);
          MapLocation targetLocation = new MapLocation(xCor, yCor);
          if (here.distanceTo(targetLocation) <= RobotType.TANK.sensorRadius
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
            //EvasiveTank.move(nearbyBullets, nearbyRobots, here.directionTo(targetLocation), targetLocation);
            RobotUtils.tryMoveDestination(targetLocation);
          }
        }
      }
    }
    else {
      if (TANK_DEBUG) {
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
          value = 3;
          break;
        case SCOUT:
          value = 1;
          break;
        case ARCHON:
          value = 0;
          break;
        case SOLDIER:
          value = 4;
          break;
        case LUMBERJACK:
          value = 2;
          break;
        case TANK:
          value = 5;
          break;
      }
      if (TANK_DEBUG) {
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
      MapLocation[] locations = rc.getInitialArchonLocations(them);
      findSquad();
      enemyArchonLocation = locations[0];
      mode = ATTACK;
      if (rc.getRoundNum() < 100) {
        rc.broadcast(squad_channel + 1, 0);
        rc.broadcast(squad_channel + 2, (int) enemyArchonLocation.x);
        rc.broadcast(squad_channel + 3, (int) enemyArchonLocation.y);
        visitedEnemyArchon = false;
        myDir = here.directionTo(enemyArchonLocation);
      }
      else {
        if (rc.readBroadcast(squad_channel + 1) == -1) {
          enemyArchonLocation = locations[myID % locations.length];
          rc.broadcast(squad_channel + 1, 0);
          rc.broadcast(squad_channel + 2, (int) enemyArchonLocation.x);
          rc.broadcast(squad_channel + 3, (int) enemyArchonLocation.y);
          visitedEnemyArchon = false;
          myDir = here.directionTo(enemyArchonLocation);
        }
        else {
          int x = rc.readBroadcast(squad_channel + 2);
          int y = rc.readBroadcast(squad_channel + 3);
          myDir = here.directionTo(new MapLocation(x, y));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    while (true) {
      Globals.update();
      try {
        if (TANK_DEBUG) {
          System.out.println(mode);
        }
        if (mode == ATTACK) {
          if (target == null) {
            //hasn't found him or he recently died
            int targetID = rc.readBroadcast(squad_channel + 1);
            if (targetID == -1) {
              if (TANK_DEBUG) {
                System.out.println("null target, roaming");
              }
              mode = ROAM;
              roam();
            }
            else {
              // if there is a target requested but not yet seen
              if (TANK_DEBUG) {
                System.out.println("null target, moving to attack");
              }
              int xCor = rc.readBroadcast(squad_channel + 2);
              int yCor = rc.readBroadcast(squad_channel + 3);
              MapLocation destination = new MapLocation(xCor, yCor);
              if (TANK_DEBUG) {
                System.out.println(destination);
              }
              if (here.distanceTo(destination) <= RobotType.TANK.sensorRadius
                  + MIN_ROBOT_RADIUS) {
                //System.out.println(visitedEnemyArchon);
                if (!visitedEnemyArchon && destination.distanceTo(enemyArchonLocation) < 1.5f) {
                  visitedEnemyArchon = true;
                }
                // Keep moving towards archon location
                if (!visitedEnemyArchon) {
                  rc.broadcast(squad_channel + 1, 0);
                  destination = enemyArchonLocation;
                  rc.broadcast(squad_channel + 2, (int) enemyArchonLocation.x);
                  rc.broadcast(squad_channel + 3, (int) enemyArchonLocation.y);
                  moveToAttack(destination);
                }
                // Disengage because target not found in last location
                else {
                  mode = ROAM;
                  rc.broadcast(squad_channel + 1, -1);
                  roam();
                }
              }
              else {
                moveToAttack(destination);
              }
            }
          }
          else {
            if (TANK_DEBUG) {
              System.out.println("target ID: " + target.getID());
            }
            // if target != null
            if (rc.canSenseRobot(target.getID())) {
              RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
              target = enemies[priority(enemies)];
              rc.broadcast(squad_channel + 1, target.getID());
              rc.broadcast(squad_channel + 2, (int) target.getLocation().x);
              rc.broadcast(squad_channel + 3, (int) target.getLocation().y);
              attack(target);
            }
            else {
              int ogTarget = rc.readBroadcast(squad_channel + 1);
              if (target.getID() == ogTarget) {
                int xCor = rc.readBroadcast(squad_channel + 2);
                int yCor = rc.readBroadcast(squad_channel + 3);
                MapLocation targetLocation = new MapLocation(xCor, yCor);
                if (here.distanceTo(targetLocation) <= RobotType.TANK.sensorRadius) {
                  // Disengage, because target could not be found at last known location
                  if (targetLocation.equals(enemyArchonLocation)) {
                    visitedEnemyArchon = true;
                  }
                  target = null;
                  if (!visitedEnemyArchon) {
                    mode = ATTACK;
                    rc.broadcast(squad_channel + 1, 0);
                    MapLocation destination = enemyArchonLocation;
                    rc.broadcast(squad_channel + 2, (int) enemyArchonLocation.x);
                    rc.broadcast(squad_channel + 3, (int) enemyArchonLocation.y);
                    moveToAttack(destination);
                  }
                  else {
                    mode = ROAM;
                    rc.broadcast(squad_channel + 1, -1);
                    roam();
                  }
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
        if (!hasReportedDeath && rc.getHealth() < 6) {
          int tanks = rc.readBroadcast(TANK_PRODUCTION_CHANNEL);
          hasReportedDeath = true;
          rc.broadcast(TANK_PRODUCTION_CHANNEL, tanks - 1);
        }
        if (currentRoundNum % 10 == 0) {
          report();
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