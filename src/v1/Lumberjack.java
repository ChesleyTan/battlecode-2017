package v1;

import battlecode.common.*;
import utils.Globals;


public class Lumberjack extends Globals {
  
  private static Direction mydir;
  private static RobotInfo target;
  private static TreeInfo targetTree;
  
  public static TreeInfo[] getAllTrees() {
    TreeInfo[] first = rc.senseNearbyTrees(-1, them);
    TreeInfo[] second = rc.senseNearbyTrees(-1, Team.NEUTRAL);
    TreeInfo[] both = new TreeInfo[first.length+second.length];
    for(int i=0;i<first.length;i++)
        both[i] = first[i];
    for(int i=0;i<second.length;i++)
        both[first.length + i] = second[i];
    return both;
}

  public static void roam() throws GameActionException{
    if(rc.canMove(mydir)){
      rc.move(mydir);
    }
    else{
      while(!rc.canMove(mydir)){
        mydir = mydir.rotateLeftDegrees(10);
      }
      rc.move(mydir);
    }
  }
  
  public static void checkNearbyLumbersAndMove() throws GameActionException{
    //Note: this assumes all nearby robots chasing the enemy are lumberjacks
    if (rc.canSenseRobot(target.ID)){
      target = rc.senseRobot(target.ID);
      RobotInfo[] attackingLumbers = rc.senseNearbyRobots(target.location, 2f, us);
      Direction toMe90 = target.location.directionTo(here).rotateRightDegrees(90);
      boolean isInRangeOfFriendlies = false;
      int rotateAmt = 0;
      MapLocation closestPoint = target.location.add(toMe90, target.getRadius() + 1.1f);
      for (RobotInfo r : attackingLumbers){
        if (r.location.isWithinDistance(closestPoint, 2)){
          isInRangeOfFriendlies = true;
          break;
        }
      }
      while(isInRangeOfFriendlies && rotateAmt < 18){
        toMe90 = toMe90.rotateLeftDegrees(10);
        rotateAmt ++;
        closestPoint = target.location.add(toMe90, target.getRadius() + 1.1f);
        isInRangeOfFriendlies = false;
        for (RobotInfo r : attackingLumbers){
          if (r.location.isWithinDistance(closestPoint, 2)){
            isInRangeOfFriendlies = true;
            break;
          }
        }
      }
      Direction principledirect = toMe90.opposite();
      rc.setIndicatorDot(here.add(principledirect), 0, 255, 0);
      if (!isInRangeOfFriendlies){
        if (here.distanceTo(target.location) - RobotType.LUMBERJACK.bodyRadius - target.getRadius() > 1){
          boolean canMove = RobotPlayer.tryMove(principledirect, 15, 3);
          if (!canMove){
            target = null;
          }
        }
      }
    }
    else{
      target = null;
    }
    /*
    if (rc.canMove(principledirect) && !isInRangeOfFriendlies){
      rc.move(principledirect);
    }
    else{
      rotateAmt = 1;
      while(rotateAmt < 9){
        int offset = rotateAmt * 10;
        if (rc.canMove(principledirect.rotateLeftDegrees(offset))){
          rc.move(principledirect.rotateLeftDegrees(offset));
          break;
        }
        else if (rc.canMove(principledirect.rotateRightDegrees(offset))){
          rc.move(principledirect.rotateRightDegrees(offset));
          break;
        }
        rotateAmt ++;
      }
      if (!rc.hasMoved()){
        target = null;
      }
        
    }*/
    
  }
  public static void chase() throws GameActionException{
    checkNearbyLumbersAndMove();
    if (target != null){
      if (here.distanceTo(target.location) <= 2 + target.getRadius()){
        rc.strike();
      }
      if (target.getHealth() <= 0){
        target = null;
      }
    }
  }
  
  public static TreeInfo reachable(RobotInfo target){
    Direction toThere = here.directionTo(target.location);
    TreeInfo[] nearbyTrees = getAllTrees();
    for (TreeInfo t : nearbyTrees){
      Direction toTree = here.directionTo(t.getLocation());
      if (Math.abs(toThere.degreesBetween(toTree)) < 90){
        float distToTree = here.distanceTo(t.location) - t.getRadius();
        float distToTarget = here.distanceTo(target.location) - target.getRadius();
        if (distToTree < distToTarget){
          return t;
        }
      }
    }
    return null;
  }
  
  public static RobotInfo reachable(TreeInfo target){
    Direction toThere = here.directionTo(target.location);
    RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, them);
    for (RobotInfo r : nearbyRobots){
      Direction toTree = here.directionTo(r.getLocation());
      if (Math.abs(toThere.degreesBetween(toTree)) < 90){
        float distToTree = here.distanceTo(r.location) - r.getRadius();
        float distToTarget = here.distanceTo(target.location) - target.getRadius();
        if (distToTree < distToTarget){
          return r;
        }
      }
    }
    return null;
  }
  
  public static void switchTarget(TreeInfo tree){
    target = null;
    targetTree = tree;
  }
  
  public static void switchTarget(RobotInfo robot){
    target = robot;
    targetTree = null;
  }
  
  public static void tryChop() throws GameActionException{
    if (rc.canSenseTree(targetTree.ID)){
      targetTree = rc.senseTree(targetTree.ID);
      float distanceBetween = here.distanceTo(targetTree.location);
      if(distanceBetween - RobotType.LUMBERJACK.bodyRadius - targetTree.getRadius() > RobotType.LUMBERJACK.strideRadius){
        Direction towardsTree = here.directionTo(targetTree.location);
        boolean moved = RobotPlayer.tryMove(towardsTree, 15, 3);
        if (!moved){
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
      if (targetTree != null){
        if(rc.canChop(targetTree.location)){
          rc.chop(targetTree.location);
        }
        System.out.println("Health " + targetTree.getHealth());
      }
    }
    else{
      targetTree = null;
    }
  }
  
  public static RobotInfo priority(RobotInfo[] enemies){
    RobotInfo result = null;
    int currvalue = 0;
    for (RobotInfo r : enemies){
      int value = 0;
      switch(r.getType()){
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
        value = 0;
        break;
      }
      System.out.println("Value: " + value);
      if( value > currvalue){
        currvalue = value;
        result = r;
      }
    }
    return result;
  }
  
  public static void loop() throws GameActionException{
    try{
      mydir = new Direction((float)(rand.nextFloat() * 2 * Math.PI));
      while(true){
        if (target != null)
          System.out.println(target.ID);
        if (targetTree != null)
          System.out.println(targetTree.ID);
        Globals.update();
        // Consider targets that are behind trees
        if (target != null || targetTree != null){
          // reachable should not automatically switch targets
          if (target != null){
            TreeInfo closerTree = reachable(target);
            if (closerTree == null){
              chase();
            }
            else{
              switchTarget(closerTree);
              tryChop();
            }
          }
          else if (targetTree != null){
            RobotInfo closerRobot = reachable(targetTree);
            if (closerRobot != null)
              System.out.println(closerRobot.ID);
            if (closerRobot == null){
              tryChop();
            }
            else{
              switchTarget(closerRobot);
              chase();
            }
          }
        }
        else{
          RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
          if(enemies.length != 0){
            target = priority(enemies);
            TreeInfo closerTree = reachable(target);
            if (closerTree == null){
              chase();
            }
            else{
              switchTarget(closerTree);
              tryChop();
            }
          }
          else{
            TreeInfo[] trees = getAllTrees();
            if(trees.length != 0){
              targetTree = trees[0];
              RobotInfo closerRobot = reachable(targetTree);
              if (closerRobot == null){
                tryChop();
              }
              else{
                switchTarget(closerRobot);
                chase();
              }
            }
            else{
              roam();
            }
          }
        }
        Clock.yield();
      }
    }catch(GameActionException e){
      e.printStackTrace();
    }
  }
}