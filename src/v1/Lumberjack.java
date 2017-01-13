package v1;

import battlecode.common.*;


public class Lumberjack extends Globals{
  
  private static Direction mydir;
  private static RobotInfo target;
  
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
    RobotInfo[] attackingLumbers = rc.senseNearbyRobots(target.location, 2f, us);
    Direction toMe90 = target.location.directionTo(here).rotateRightDegrees(90);
    boolean isInRangeOfFriendlies = false;
    int rotateAmt = 0;
    MapLocation closestPoint = target.location.add(toMe90, target.getRadius() + 1);
    for (RobotInfo r : attackingLumbers){
      if (r.location.isWithinDistance(closestPoint, 2)){
        isInRangeOfFriendlies = true;
        break;
      }
    }
    while(isInRangeOfFriendlies && rotateAmt < 18){
      toMe90 = toMe90.rotateLeftDegrees(10);
      rotateAmt ++;
      closestPoint = target.location.add(toMe90, target.getRadius() + 1);
      for (RobotInfo r : attackingLumbers){
        if (r.location.isWithinDistance(closestPoint, 2)){
          isInRangeOfFriendlies = true;
          continue;
        }
      }
      isInRangeOfFriendlies = false;
    }
    Direction principledirect = toMe90.opposite();
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
        
    }
    
  }
  public static void chase() throws GameActionException{
    checkNearbyLumbersAndMove();
    if (here.distanceTo(target.location) <= 2){
      rc.strike();
    }
  }
  public static RobotInfo priority(RobotInfo[] enemies){
    RobotInfo result = null;
    for (RobotInfo r : enemies){
      int currvalue = 0;
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
      case TANK:
        value = 0;
        break;
      }
      if( value > currvalue){
        result = r;
      }
    }
    return result;
  }
  
  public static void loop() throws GameActionException{
    try{
      mydir = new Direction((float)(rand.nextFloat() * 2 * Math.PI));
      while(true){
        Globals.update();
        if (target != null){
          chase();
        }
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
        if(enemies.length != 0){
          target = priority(enemies);
          chase();
        }
        roam();
      }
    }catch(GameActionException e){
      e.printStackTrace();
    }
  }
}