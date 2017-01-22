package finalVersion;

import battlecode.common.*;
import utils.RobotUtils;
import utils.Globals;

public class Lumberjack extends Globals {

  private static Direction mydir;
  private static RobotInfo target;
  private static TreeInfo targetTree;
  private static final int NEUTRAL = 0;
  private static final int DEFENSE = 1;
  private static int mode;

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
    if (rc.canMove(mydir)) {
      rc.move(mydir);
    }
    else {
      int attempts = 0;
      while (attempts < 10 && !rc.canMove(mydir)) {
        mydir = RobotUtils.randomDirection();
        ++attempts;
      }
      if (rc.canMove(mydir)) {
        rc.move(mydir);
      }
    }
  }

  public static void checkNearbyLumbersAndMove() throws GameActionException {
    //Note: this assumes all nearby robots chasing the enemy are lumberjacks
    if (rc.canSenseRobot(target.ID)) {
      target = rc.senseRobot(target.ID);
      RobotInfo[] attackingLumbers = siftForLumbers(rc.senseNearbyRobots(target.location, 4f, us));
      Direction toMe = target.location.directionTo(here);
      boolean isInRangeOfFriendlies = false;
      //int rotateAmt = 0;
      MapLocation closestPoint = target.location.add(toMe, target.getRadius() + 1.1f);
      for (RobotInfo r : attackingLumbers) {
        //System.out.println(r.ID);
        //System.out.println(r.location.distanceTo(closestPoint));
        if (r.location.isWithinDistance(closestPoint, RobotType.LUMBERJACK.bodyRadius
            + RobotType.LUMBERJACK.strideRadius + r.type.bodyRadius)) {
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
      Direction principledirect = toMe.opposite();
      rc.setIndicatorDot(here.add(principledirect), 0, 255, 0);
      //System.out.println(target.ID);
      if (!isInRangeOfFriendlies) {
        if (here.distanceTo(target.location) - RobotType.LUMBERJACK.bodyRadius
            - target.getRadius() > 3) {
          boolean canMove = RobotUtils.tryMove(principledirect, 15, 6);
          if (!canMove) {
            target = null;
          }
        }
        else {
          if (rc.canMove(principledirect)) {
            rc.move(principledirect);
          }
        }
      }
      else {
        RobotUtils.tryMove(toMe, 15, 6);
        target = null;
      }
    }
    else {
      target = null;
    }

  }

  public static void chase() throws GameActionException {
    checkNearbyLumbersAndMove();
    if (target != null) {
      if (here.distanceTo(target.location) <= 2 + target.getRadius()) {
        rc.strike();
      }
      if (target.getHealth() <= 0) {
        target = null;
      }
    }
  }

  public static TreeInfo reachable(RobotInfo target) {
    Direction toThere = here.directionTo(target.location);
    TreeInfo[] nearbyTrees = getAllTrees();
    float distToTarget = here.distanceTo(target.location) - target.getRadius();
    for (TreeInfo t : nearbyTrees) {
      Direction toTree = here.directionTo(t.getLocation());
      if (Math.abs(toThere.degreesBetween(toTree)) < 90) {
        float distToTree = here.distanceTo(t.location) - t.getRadius();
        if (distToTree < distToTarget) {
          return t;
        }
      }
    }
    return null;
  }

  public static RobotInfo reachable(TreeInfo target) {
    Direction toThere = here.directionTo(target.location);
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, them);
    float distToTarget = here.distanceTo(target.location) - target.getRadius();
    for (RobotInfo r : nearbyRobots) {
      Direction toRobot = here.directionTo(r.getLocation());
      if (Math.abs(toThere.degreesBetween(toRobot)) < 90) {
        float distToRobot = here.distanceTo(r.location) - r.getRadius();
        if (distToRobot < distToTarget) {
          return r;
        }
      }
    }
    return null;
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
    if (rc.canSenseTree(targetTree.ID)) {
      targetTree = rc.senseTree(targetTree.ID);
      float distanceBetween = here.distanceTo(targetTree.location);
      if (distanceBetween - RobotType.LUMBERJACK.bodyRadius
          - targetTree.getRadius() > RobotType.LUMBERJACK.strideRadius) {
        Direction towardsTree = here.directionTo(targetTree.location);
        boolean moved = RobotUtils.tryMove(towardsTree, 15, 6);
        if (!moved) {
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
        if (rc.canChop(targetTree.location)) {
          rc.chop(targetTree.location);
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
      mydir = new Direction((float) (rand.nextFloat() * 2 * Math.PI));
    } catch (Exception e) {
      e.printStackTrace();
      Clock.yield();
    }
    while (true) {
      try {
        Globals.update();
        // Consider targets that are behind trees
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
        else if (targetTree != null) {
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
          RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
          if (enemies.length != 0) {
            target = priority(enemies);
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
            TreeInfo[] trees = getAllTrees();
            if (trees.length != 0) {
              TreeInfo closestTree = null;
              float minDist = 9999f;
              for (TreeInfo ti : trees) {
                float treeDist = ti.location.distanceTo(here);
                if (treeDist < minDist) {
                  closestTree = ti;
                  minDist = treeDist;
                }
              }
              targetTree = closestTree;
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