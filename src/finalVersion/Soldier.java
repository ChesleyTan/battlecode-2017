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
  private static Direction myDir;
  private static RobotInfo target;
  private static MapLocation enemyArchonLocation;
  private static final boolean SOLDIER_DEBUG = true;
  private static boolean hasReportedDeath = false;
  private static boolean visitedEnemyArchon = true;
  private static int roamCount = 0;
  private static int roundStarted = rc.getRoundNum();

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
        if (SOLDIER_DEBUG) {
          System.out.println("Squad channel: " + i);
        }
        rc.broadcast(i, squad_count + 1);
        return;
      }
      i += DEFENSE_BLOCK_WIDTH;
    }
    squad_channel = DEFENSE_START_CHANNEL;
  }

  private static boolean pentadShotGardener(TreeInfo treeBetween, MapLocation gardenerLocation) {
    RobotInfo[] friendlies = rc.senseNearbyRobots(gardenerLocation, 5, us);
    int soldierCount = 0;
    for (RobotInfo r : friendlies) {
      if (r.getType() == RobotType.SOLDIER) {
        soldierCount++;
        if (soldierCount == 3) {
          break;
        }
      }
    }
    return ((RobotUtils.getBugCount() > 10 || soldierCount >= 2)
        && (treeBetween != null && treeBetween.getTeam() == them));
  }

  /*
   * Assumes target has already been seen and is a local variable
   * make sure whenever this is called, person has not moved yet
   */
  private static void attack(RobotInfo target) throws GameActionException {
    if (SOLDIER_DEBUG) {
      System.out.println("attacking");
    }
    MapLocation targetLocation = target.getLocation();
    Direction towardsEnemy = here.directionTo(targetLocation);
    TreeInfo treeBetween = rc
        .senseTreeAtLocation(here.add(towardsEnemy, RobotType.SOLDIER.bodyRadius + 1));
    boolean pentadShotGardener = pentadShotGardener(treeBetween, targetLocation);
    if (SOLDIER_DEBUG) {
      System.out.println("pentadshotgardener: " + pentadShotGardener);
    }
    RobotInfo[] robots = rc.senseNearbyRobots(EvasiveSoldier.ENEMY_DETECT_RADIUS, them);
    if (target.getType() != RobotType.GARDENER || !pentadShotGardener) {
      BulletInfo[] bullets = rc.senseNearbyBullets(EvasiveSoldier.BULLET_DETECT_RADIUS);
      EvasiveSoldier.move(bullets, robots, target, targetLocation);
    }
    //RobotUtils.tryMoveDestination(targetLocation);
    if (pentadShotGardener || TargetingUtils.clearShot(here, target)) {
      if (SOLDIER_DEBUG) {
        System.out.println("clearShot to target");
      }
      float distanceEnemy = here.distanceTo(targetLocation);
      if ((distanceEnemy <= 6 || robots.length > 2) && rc.canFirePentadShot()
          && rc.getTreeCount() > 1) {
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
   * Assumes soldier has neither moved nor attacked
   * Also assumes that there is a target
   */
  private static void moveToAttack(MapLocation destination) throws GameActionException {
    BulletInfo[] bullets = rc.senseNearbyBullets();
    RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
    if (enemies.length != 0) {
      RobotInfo priorityEnemy = null;
      int nullifiedCount = enemies.length;
      while (priorityEnemy == null && nullifiedCount > 0) {
        int index = priority(enemies);
        if (index != -1) {
          priorityEnemy = enemies[index];
          if (priorityEnemy.getType() != RobotType.SCOUT
              || rc.senseTreeAtLocation(here.add(here.directionTo(priorityEnemy.location),
                  Math.min(RobotType.SOLDIER.sensorRadius - 0.1f,
                      here.distanceTo(priorityEnemy.location)))) == null) {
            target = priorityEnemy;
            attack(target);
          }
          else {
            priorityEnemy = null;
            nullifiedCount--;
          }
        }
      }
      if (priorityEnemy == null) {
        EvasiveSoldier.move(bullets, enemies, target, destination);
      }
    }
    else {
      EvasiveSoldier.move(bullets, enemies, target, destination);
    }
  }

  private static void roam() throws GameActionException {
    int targetID = rc.readBroadcast(squad_channel + 1);
    if (targetID != -1) {
      if (SOLDIER_DEBUG) {
        System.out.println("target id = -1");
      }
      mode = ATTACK;
      roamCount = 0;
      int xCor = rc.readBroadcast(squad_channel + 2);
      int yCor = rc.readBroadcast(squad_channel + 3);
      MapLocation destination = new MapLocation(xCor, yCor);
      moveToAttack(destination);
    }
    else {
      if (SOLDIER_DEBUG) {
        System.out.println("sensing nearby enemies");
      }
      RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
      //BulletInfo[] bullets = rc.senseNearbyBullets();
      if (enemies.length > 0) {
        RobotInfo priorityEnemy = null;
        int nullifiedCount = enemies.length;
        while (priorityEnemy == null && nullifiedCount > 0) {
          int priority = priority(enemies);
          if (priority != -1) {
            priorityEnemy = enemies[priority];
            if (priorityEnemy.getType() != RobotType.SCOUT
                || rc.senseTreeAtLocation(here.add(here.directionTo(priorityEnemy.location),
                    Math.min(RobotType.SOLDIER.sensorRadius - 0.1f,
                        here.distanceTo(priorityEnemy.location)))) == null) {
              if (SOLDIER_DEBUG) {
                System.out.println(priorityEnemy.getID());
              }
              target = priorityEnemy;
              mode = ATTACK;
              roamCount = 0;
              attack(target);
            }
            else {
              priorityEnemy = null;
              nullifiedCount--;
            }
          }
        }
      }
      if (target == null) {
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
          roamCount = 0;
          mode = ATTACK;
          moveToAttack(new MapLocation(cacheTargetX, cacheTargetY));
        }
        else {
          if (SOLDIER_DEBUG) {
            System.out.println("Roam count: " + roamCount);
          }
          if (!rc.hasMoved()) {
            if (roamCount >= 30) {
              int i = DEFENSE_START_CHANNEL;
              while (i < DEFENSE_END_CHANNEL) {
                int target = rc.readBroadcast(i + 1);
                if (target > 0) {
                  int xCor = rc.readBroadcast(i + 2);
                  int yCor = rc.readBroadcast(i + 3);
                  rc.broadcast(squad_channel + 1, target);
                  rc.broadcast(squad_channel + 2, xCor);
                  rc.broadcast(squad_channel + 3, yCor);
                  MapLocation destination = new MapLocation(xCor, yCor);
                  roamCount = 0;
                  mode = ATTACK;
                  moveToAttack(destination);
                  return;
                }
                i += DEFENSE_BLOCK_WIDTH;
              }
            }
            int attempts = 0;
            while (!rc.canMove(myDir) && attempts < 20) {
              if (Clock.getBytecodesLeft() < 2000) {
                break;
              }
              myDir = RobotUtils.randomDirection();
              attempts++;
            }
            if (rc.canMove(myDir)) {
              rc.move(myDir);
              ++roamCount;
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
      MapLocation[] locations = rc.getInitialArchonLocations(them);
      findSquad();
      enemyArchonLocation = locations[0];
      mode = ATTACK;
      if (rc.getRoundNum() < 100) {
        rc.broadcast(squad_channel + 1, 0);
        rc.broadcast(squad_channel + 2, (int) enemyArchonLocation.x);
        rc.broadcast(squad_channel + 3, (int) enemyArchonLocation.y);
        visitedEnemyArchon = currentRoundNum > 1000;
        myDir = here.directionTo(enemyArchonLocation);
      }
      else {
        if (rc.readBroadcast(squad_channel + 1) == -1) {
          enemyArchonLocation = locations[myID % locations.length];
          rc.broadcast(squad_channel + 1, 0);
          rc.broadcast(squad_channel + 2, (int) enemyArchonLocation.x);
          rc.broadcast(squad_channel + 3, (int) enemyArchonLocation.y);
          visitedEnemyArchon = rc.getRoundNum() > 1200;
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
        if (SOLDIER_DEBUG) {
          System.out.println(mode);
        }
        if (!visitedEnemyArchon && currentRoundNum - roundStarted > 300) {
          visitedEnemyArchon = true;
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
            if (SOLDIER_DEBUG) {
              System.out.println("target ID: " + target.getID());
            }
            // if target != null
            if (rc.canSenseRobot(target.getID())) {
              RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
              int priorityIndex = priority(enemies);
              int nullifiedCount = enemies.length;
              target = null;
              while (target == null && nullifiedCount > 0) {
                RobotInfo priority = enemies[priorityIndex];
                MapLocation location = priority.getLocation();
                if (priority.getType() != RobotType.SCOUT
                    || rc.senseTreeAtLocation(here.add(here.directionTo(priority.location),
                        Math.min(RobotType.SOLDIER.sensorRadius - 0.1f,
                            here.distanceTo(priority.location)))) == null) {
                  target = priority;
                  rc.broadcast(squad_channel + 1, target.getID());
                  rc.broadcast(squad_channel + 2, (int) location.x);
                  rc.broadcast(squad_channel + 3, (int) location.y);
                  attack(target);
                }
                else {
                  enemies[priorityIndex] = null;
                  nullifiedCount--;
                  priorityIndex = priority(enemies);
                }
              }
              if (target == null) {
                mode = ROAM;
                rc.broadcast(squad_channel + 1, -1);
                roam();
              }
            }
            else {
              int ogTarget = rc.readBroadcast(squad_channel + 1);
              if (target.getID() == ogTarget) {
                int xCor = rc.readBroadcast(squad_channel + 2);
                int yCor = rc.readBroadcast(squad_channel + 3);
                MapLocation targetLocation = new MapLocation(xCor, yCor);
                if (here.distanceTo(targetLocation) <= RobotType.SOLDIER.sensorRadius) {
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
          int soldiers = rc.readBroadcast(SOLDIER_PRODUCTION_CHANNEL);
          hasReportedDeath = true;
          rc.broadcast(SOLDIER_PRODUCTION_CHANNEL, soldiers - 1);
          int squad_count = rc.readBroadcast(squad_channel);
          rc.broadcast(squad_channel, squad_count - 1);
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