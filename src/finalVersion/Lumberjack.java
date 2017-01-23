package finalVersion;

import battlecode.common.*;
import utils.RobotUtils;
import utils.BroadcastUtils;
import utils.BroadcastUtils.Directive;
import utils.Globals;

public class Lumberjack extends Globals {

  private static Direction targetDirection;
  private static RobotInfo target;
  private static TreeInfo targetTree;
  private static final int NEUTRAL = 0;
  private static final int DEFENSE = 1;
  private static int mode;
  private static TreeInfo[] allNearbyTrees;
  private static int targetBlacklist;
  private static int targetBlacklistRound;
  private static int queuedTree = 0;
  private static MapLocation destination;
  
  

  
  public static TreeInfo[] getAllTrees() {
    TreeInfo[] union;
    TreeInfo[] nearbyTrees = rc.senseNearbyTrees(-1);
    int count = 0;
    for (TreeInfo ti : nearbyTrees) {
      if (ti.team != us) {
        ++count;
      }
    }
    union = new TreeInfo[count];
    for (TreeInfo ti : nearbyTrees) {
      if (ti.team != us) {
        union[--count] = ti;
      }
    }
    return union;
  }

  public static RobotInfo[] siftForLumbers(RobotInfo[] sample) {
    RobotInfo[] result;
    int count = 0;
    for (RobotInfo ri : sample) {
      if (ri.type == RobotType.LUMBERJACK) {
        ++count;
      }
    }
    result = new RobotInfo[count];
    for (RobotInfo ri : sample) {
      if (ri.type == RobotType.LUMBERJACK) {
        result[--count] = ri;
      }
    }
    return result;
  }

  public static void roam() throws GameActionException {
    System.out.println("Roaming");
    System.out.println(targetDirection);
    if (!rc.onTheMap(here.add(targetDirection,
        1 + RobotType.LUMBERJACK.strideRadius + RobotType.LUMBERJACK.bodyRadius))) {
      // Change direction when hitting border
      targetDirection = targetDirection.rotateRightRads((float) (rand.nextFloat() * Math.PI));
    }
    BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
    if (!RobotUtils.tryMoveIfSafe(targetDirection, nearbyBullets, (rand.nextFloat() * 10 + 20), 3)) {
      targetDirection = targetDirection.rotateRightRads((float) (rand.nextFloat() * Math.PI));
    }
  }

  public static void checkNearbyLumbersAndMove() throws GameActionException {
    //Note: this assumes all nearby robots chasing the enemy are lumberjacks
    if (rc.canSenseRobot(target.getID())) {
      target = rc.senseRobot(target.getID());
      MapLocation targetLoc = target.getLocation();
      RobotInfo[] attackingLumbers = siftForLumbers(rc.senseNearbyRobots(targetLoc,
          GameConstants.LUMBERJACK_STRIKE_RADIUS + 2 * RobotType.LUMBERJACK.bodyRadius, us));
      BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
      Direction toMe = targetLoc.directionTo(here);
      boolean isInRangeOfFriendlies = false;
      //int rotateAmt = 0;
      MapLocation closestPoint = targetLoc.add(toMe,
          target.getRadius() + RobotType.LUMBERJACK.bodyRadius + 0.1f);
      for (RobotInfo r : attackingLumbers) {
        //System.out.println(r.ID);
        //System.out.println(r.location.distanceTo(closestPoint));
        if (r.getLocation().isWithinDistance(closestPoint,
            2 * RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS)) {
          isInRangeOfFriendlies = true;
          break;
        }
      }
      // Code to make them more aggressive but also towards each other
      /*while(isInRangeOfFriendlies && rotateAmt < 9){
        Direction newDir = toMe.rotateLeftDegrees(10 * (rotateAmt + 1));
        closestPoint = target.location.add(newDir, target.getRadius() + 1.1f);
        isInRangeOfFriendlies = false;
        for (RobotInfo r : attackingLumbers){
          System.out.println(r.ID);
          System.out.println(r.location.distanceTo(closestPoint));
          if (r.location.isWithinDistance(closestPoint, 3)){
            isInRangeOfFriendlies = true;
            break;
          }
        }
        if (!isInRangeOfFriendlies)
          break;
        else{
          newDir = toMe.rotateRightDegrees(10* (rotateAmt + 1));
          closestPoint = target.location.add(newDir, target.getRadius() + 1.1f);
          isInRangeOfFriendlies = false;
          for (RobotInfo r : attackingLumbers){
            if (r.location.isWithinDistance(closestPoint, 3)){
              isInRangeOfFriendlies = true;
              break;
            }
          }
        }
        rotateAmt ++;
      }*/
      Direction principleDirection = toMe.opposite();
      rc.setIndicatorLine(here, here.add(principleDirection, 1f), 255, 0, 0);
      rc.setIndicatorLine(here.add(principleDirection, 1f), here.add(principleDirection, 1.25f), 0,
          0, 0);
      //System.out.println(target.ID);
      if (!isInRangeOfFriendlies) {
        boolean canMove = RobotUtils.tryMoveIfSafe(principleDirection, nearbyBullets, 15, 6);
        if (!canMove && !RobotUtils.tryMove(principleDirection, 15, 6)) {
          targetBlacklist = target.getID();
          targetBlacklistRound = currentRoundNum;
          target = null;
        }
      }
      else {
        RobotUtils.tryMoveIfSafe(toMe, nearbyBullets, 15, 6);
        target = null;
      }
    }
    else {
      target = null;
    }

  }

  public static void chase() throws GameActionException {
    checkNearbyLumbersAndMove();
    if (target != null && rc.canStrike()) {
      if (here.distanceTo(target.getLocation()) <= GameConstants.LUMBERJACK_STRIKE_RADIUS
          + RobotType.LUMBERJACK.bodyRadius + target.getRadius()) {
        rc.strike();
      }
      if (target.getHealth() <= 0) {
        target = null;
      }
    }
  }

  public static TreeInfo reachable(RobotInfo target) {
    MapLocation targetLoc = target.location;
    Direction toThere = here.directionTo(targetLoc);
    float distToTarget = here.distanceTo(targetLoc) - target.getRadius();
    float minDistTreeDist = 9999f;
    TreeInfo minDistTree = null;
    for (TreeInfo t : allNearbyTrees) {
      MapLocation treeLoc = t.getLocation();
      Direction toTree = here.directionTo(treeLoc);
      if (Math.abs(toThere.degreesBetween(toTree)) < 90) {
        float distToTree = here.distanceTo(treeLoc) - t.getRadius();
        if (distToTree < distToTarget && distToTree < minDistTreeDist) {
          minDistTreeDist = distToTree;
          minDistTree = t;
        }
      }
    }
    return minDistTree;
  }

  public static RobotInfo reachable(TreeInfo target) {
    MapLocation targetLoc = target.getLocation();
    Direction toThere = here.directionTo(targetLoc);
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, them);
    float distToTarget = here.distanceTo(targetLoc) - target.getRadius();
    float minDistRobotDist = 9999f;
    RobotInfo minDistRobot = null;
    for (RobotInfo r : nearbyRobots) {
      MapLocation robotLoc = r.getLocation();
      Direction toRobot = here.directionTo(robotLoc);
      if (Math.abs(toThere.degreesBetween(toRobot)) < 90) {
        float distToRobot = here.distanceTo(robotLoc) - r.getRadius();
        if (distToRobot < distToTarget && distToRobot < minDistRobotDist) {
          minDistRobotDist = distToRobot;
          minDistRobot = r;
        }
      }
    }
    return minDistRobot;
  }

  public static void switchTarget(TreeInfo tree) {
    target = null;
    targetTree = tree;
  }

  public static void switchTarget(RobotInfo robot) {
    target = robot;
    targetTree = null;
  }

  public static void tryChop() throws GameActionException {
    System.out.println("Trying to chop: " + targetTree.getID());
    if (rc.canSenseTree(targetTree.ID)) {
      targetTree = rc.senseTree(targetTree.ID);
      MapLocation treeLoc = targetTree.getLocation();
      float distanceBetween = here.distanceTo(treeLoc);
      if (distanceBetween > RobotType.LUMBERJACK.bodyRadius + targetTree.getRadius()
          + GameConstants.INTERACTION_DIST_FROM_EDGE) {
        Direction towardsTree = here.directionTo(treeLoc);
        BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
        boolean moved = RobotUtils.tryMoveIfSafe(towardsTree, nearbyBullets, 15, 6);
        if (!moved) {
          System.out.println("Could not move towards tree. Disengaging");
          targetTree = null;
        }
        /*if (rc.canMove(towardsTree)){
          rc.move(towardsTree);
        }
        else{
          towardsTree = towardsTree.rotateLeftDegrees(45);
          int attempts = 0;
          while (attempts < 9){
            if (rc.canMove(towardsTree)){
              rc.move(towardsTree);
              break;
            }
            towardsTree.rotateRightDegrees(10);
            attempts ++;
          }
        }*/
      }
      if (targetTree != null) {
        if (rc.canChop(treeLoc)) {
          rc.chop(treeLoc);
        }
        //System.out.println("Health " + targetTree.getHealth());
      }
    }
    else {
      targetTree = null;
    }
  }

  public static RobotInfo priority(RobotInfo[] enemies) {
    RobotInfo result = null;
    int currvalue = 0;
    for (RobotInfo r : enemies) {
      int value = 0;
      if (r.getID() == targetBlacklist && currentRoundNum - targetBlacklistRound < 30) {
        continue;
      }
      switch (r.getType()) {
        case GARDENER:
          value = 4;
          break;
        case SCOUT:
          value = 3;
          break;
        case ARCHON:
          value = 2;
          break;
        case SOLDIER:
          value = 1;
          break;
        case LUMBERJACK:
          value = 1;
          break;
        case TANK:
          value = 1;
          break;
      }
      //System.out.println("Value: " + value);
      if (value > currvalue) {
        currvalue = value;
        result = r;
      }
    }
    return result;
  }

  public static void loop() throws GameActionException {
    try {
      if (currentRoundNum < 500) {
        mode = DEFENSE;
      }
      else {
        mode = NEUTRAL;
      }
      targetDirection = RobotUtils.randomDirection();
    } catch (Exception e) {
      e.printStackTrace();
      Clock.yield();
    }
    while (true) {
      try {
        Globals.update();
        allNearbyTrees = getAllTrees();
        /*
        RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(-1, them);
        RobotInfo nearestEnemy = priority(nearbyEnemies);
        float minEnemyDist = nearestEnemy == null ? 9999f : nearestEnemy.getLocation().distanceTo(here);
        TreeInfo nearestTree = null;
        float minTreeDist = 9999f;
        for (TreeInfo ti : allNearbyTrees) {
          float treeDist = ti.getLocation().distanceTo(here);
          if (treeDist < minTreeDist) {
            nearestTree = ti;
            minTreeDist = treeDist;
          }
        }
        if (nearestEnemy != null && (nearestTree == null || minEnemyDist <= minTreeDist)) {
          target = nearestEnemy;
          chase();
        }
        else if (nearestTree != null && (nearestEnemy == null || minEnemyDist > minTreeDist)) {
          targetTree = nearestTree;
          tryChop();
        }
        else {
          roam();
        }
        */
        // Consider targets that are behind trees
        if (target != null) {
          System.out.println("Target: " + target);
          TreeInfo closerTree = reachable(target);
          if (closerTree == null) {
            chase();
          }
          else {
            switchTarget(closerTree);
            tryChop();
          }
        }
        else if (targetTree != null) {
          System.out.println("Target tree: " + targetTree);
          RobotInfo closerRobot = reachable(targetTree);
          if (closerRobot == null) {
            tryChop();
          }
          else {
            switchTarget(closerRobot);
            chase();
          }
        }
        else {
          System.out.println("sensing nearby robots");
          RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
          target = priority(enemies);
          if (target != null) {
            TreeInfo closerTree = reachable(target);
            if (closerTree == null) {
              chase();
            }
            else {
              switchTarget(closerTree);
              tryChop();
            }
          }
          else {
            System.out.println("looking for region directives");
            Directive[] directives = BroadcastUtils.getRegionDirectives((int)here.x, (int)here.y);
            for(Directive d: directives){
              // CUT
              if (d != null){
                if (d.type == 7){
                  MapLocation center = new MapLocation(d.x, d.y);
                  TreeInfo[] trees = rc.senseNearbyTrees(center, d.radius, Team.NEUTRAL);
                  System.out.println("Center of region directive: " + center);
                  if (trees.length > 0){
                    targetTree = trees[0];
                    break;
                  }
                  else{
                    BroadcastUtils.addRegionDirective(0, 0, 0, 0, 0);
                  }
                }
              }
            }
            if (targetTree == null){
              System.out.println("sensing nearby trees");
              if (allNearbyTrees.length != 0) {
                TreeInfo closestTree = null;
                float minDist = 9999f;
                for (TreeInfo ti : allNearbyTrees) {
                  float treeDist = ti.getLocation().distanceTo(here);
                  if (treeDist < minDist) {
                    closestTree = ti;
                    minDist = treeDist;
                  }
                }
                targetTree = closestTree;
                //System.out.println(closestTree);
                RobotInfo closerRobot = reachable(targetTree);
                if (closerRobot == null) {
                  tryChop();
                }
                else {
                  switchTarget(closerRobot);
                  chase();
                }
              }
              else {
                roam();
              }
            }
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