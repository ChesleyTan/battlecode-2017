package v2_soldier;

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

  private static void dodge(BulletInfo[] bullets, RobotInfo[] robots) throws GameActionException {
    float sumX = 0;
    float sumY = 0;
    for (BulletInfo i : bullets) {
      MapLocation endLocation = i.location.add(i.getDir(), i.getSpeed());
      float x0 = i.location.x;
      float y0 = i.location.y;
      float x1 = endLocation.x;
      float y1 = endLocation.y;
      float a = x1 - x0;
      float b = y0 - y1;
      if (a == 0 && b == 0){
        a = 0.01f;
      }
      float c = x0 * y1 - y0 * x1;
      float distance = (float) (Math.abs(a * here.x + b * here.y + c)
          / Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2)));
      if (distance < 2) {
        float x2 = (float) ((b * (b * here.x - a * here.y) - a * c)
            / (Math.pow(a, 2) + Math.pow(b, 2)));
        float y2 = (float) ((a * (a * here.y - b * here.x) - b * c)
            / (Math.pow(a, 2) + Math.pow(b, 2)));
        Direction away = new MapLocation(x2, y2).directionTo(here);
        float weighted = (float) Math.pow((RobotType.SCOUT.bulletSightRadius - distance), 2)
            / RobotType.SCOUT.bulletSightRadius * RobotType.SCOUT.strideRadius;
        sumX += away.getDeltaX(weighted);
        sumY += away.getDeltaY(weighted);
      }
    }

    for (RobotInfo r : robots) {
      Direction their_direction = r.location.directionTo(here);
      float baseValue = (RobotType.GARDENER.sensorRadius - here.distanceTo(r.location) + r.getRadius()) * (RobotType.GARDENER.sensorRadius - here.distanceTo(r.location) + r.getRadius());
      float their_distance = baseValue / RobotType.GARDENER.sensorRadius * RobotType.GARDENER.strideRadius;
      rc.setIndicatorDot(here.add(their_direction,  their_distance), 255, 0, 0);
      //System.out.println(their_distance);
      if (r.getTeam() == us){
        their_distance = their_distance / 2;
      }
      sumX += their_direction.getDeltaX(their_distance);
      sumY += their_direction.getDeltaY(their_distance);
    }

    TreeInfo[] nearbyTrees = rc.senseNearbyTrees(GameConstants.NEUTRAL_TREE_MAX_RADIUS);
    if(nearbyTrees.length <= 10){
      for (TreeInfo t : nearbyTrees) {
        Direction their_direction = t.location.directionTo(here);
        float baseValue = (RobotType.GARDENER.sensorRadius - here.distanceTo(t.location) + t.getRadius()) * (RobotType.GARDENER.sensorRadius - here.distanceTo(t.location) + t.getRadius());
        float their_distance = baseValue / RobotType.GARDENER.sensorRadius * RobotType.GARDENER.strideRadius;
        sumX += their_direction.getDeltaX(their_distance);
        sumY += their_direction.getDeltaY(their_distance);
      }
    }
    
    if (!(Clock.getBytecodesLeft() < 2000)){
      float sightRadius = RobotType.GARDENER.sensorRadius - 1;
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
    float finaldist = (float) Math.sqrt(sumX * sumX + sumY * sumY);

    Direction finalDir = new Direction(sumX, sumY);
    RobotUtils.tryMoveDist(finalDir, finaldist, 10, 6);
  }
  
  private static void findSquad() throws GameActionException {
    int i = DEFENSE_START_CHANNEL;
    while (i < DEFENSE_END_CHANNEL) {
      int squad_count = rc.readBroadcast(i);
      // System.out.println(squad_count);
      if (squad_count == 0) {
        // Clear out target field
        rc.broadcast(i + 1, -1);
      }
      if (squad_count < 3) {
        squad_channel = i;
        rc.broadcast(i, squad_count + 1);
        return;
      }
      i = i + 4;
    }
    squad_channel = ATTACK_START_CHANNEL;
  }
  
  private static boolean blockedByTree(BulletInfo i, TreeInfo[] trees){
    Direction base = here.directionTo(i.location);
    float baseDistance = here.distanceTo(i.location);
    for (TreeInfo tree: trees){
      if (i.location.distanceTo(tree.location) > baseDistance){
        continue;
      }
      Direction t = here.directionTo(tree.location);
      float radians = Math.abs(t.radiansBetween(base));
      float dist = (float)Math.sin(radians);
      if (dist < tree.getRadius()){
        return true;
      }
    }
    return false;
  }
  
  private static void checkBulletsAndDodge() throws GameActionException{
    BulletInfo[] bullets = rc.senseNearbyBullets();
    boolean willGetHitByBullet = false;
    TreeInfo[] trees = rc.senseNearbyTrees();
    for (BulletInfo i: bullets){
      if (RobotUtils.willCollideWithMe(i) && !blockedByTree(i, trees)){
        System.out.println("in danger");
        willGetHitByBullet = true;
        break;
      }
    }
    if (willGetHitByBullet){
      RobotInfo[] robots = rc.senseNearbyRobots();
      System.out.println("dodging");               
      dodge(bullets, robots);
    }
  }
  /*
   * Assumes target has already been seen and is a local variable
   * Makes no assumption about movement
   */
  private static void attack() throws GameActionException{
    if(!rc.hasMoved()){
      BulletInfo[] bullets = rc.senseNearbyBullets();
      RobotInfo[] robots = rc.senseNearbyRobots(-1, them);
      dodge(bullets, robots);
    }
    if (rc.canFireTriadShot()){
      Direction towardsEnemy = here.directionTo(target.location);
      rc.fireTriadShot(towardsEnemy);
    }
  }
  
  /* Moves in the direction of attacking and fires if reasonable
   * Assumes soldier has neither moved nor attacked
   * Also assumes that there is a target
   */
  private static void moveToAttack(int targetID) throws GameActionException{
    checkBulletsAndDodge();
    RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
    if(rc.canSenseRobot(targetID)){
      target = rc.senseRobot(targetID);
      rc.broadcast(squad_channel + 2, (int)target.location.x);
      rc.broadcast(squad_channel + 3, (int)target.location.y);
      attack();
    }
    else if (enemies.length != 0){
      target = enemies[0];
      attack();
    }
    else{
      float xCor = rc.readBroadcast(squad_channel + 2);
      float yCor = rc.readBroadcast(squad_channel + 3);
      mydir = here.directionTo(new MapLocation(xCor, yCor));
      if (!rc.hasMoved() && !RobotUtils.tryMove(mydir, 10, 9)){
        while(!rc.canMove(mydir)){
          mydir = RobotUtils.randomDirection();
        }
        if (rc.canMove(mydir)){
          rc.move(mydir);
        }
      }
    }
  }
  
  private static void roam() throws GameActionException{
    int targetID = rc.readBroadcast(squad_channel + 1);
    if (targetID != 0){
      mode = ATTACK;
      moveToAttack(targetID);
    }
    else{
      checkBulletsAndDodge();
      RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
      int priority = priority(enemies);
      while (priority != -1){
        RobotInfo enemy = enemies[priority];
        MapLocation destination = enemy.location;
        boolean successfulMove = RobotUtils.tryMove(here.directionTo(destination), 10, 6);
        if (successfulMove){
          target = enemy;
          rc.broadcast(squad_channel + 1, target.ID);
          rc.broadcast(squad_channel + 2, (int)target.location.x);
          rc.broadcast(squad_channel + 3, (int)target.location.y);
          mode = ATTACK;
          break;
        }
        else{
          enemies[priority] = null;
          priority = priority(enemies);
        }
      }
      if (priority != -1){
        if (TargetingUtils.clearShot(here, target)){
          rc.fireTriadShot(here.directionTo(target.location));
        }
      }
      else{
        int attempts = 0;
        while(!rc.canMove(mydir) || attempts < 20){
          mydir = RobotUtils.randomDirection();
          attempts ++;
        }
        if (rc.canMove(mydir)){
          rc.move(mydir);
        }
      }
    }
  }
  
  private static int value(RobotType r){
    switch(r){
    case SCOUT:
      return 5;
    case GARDENER:
      return 4;
    case LUMBERJACK:
      return 3;
    case SOLDIER:
      return 2;
    case ARCHON:
      return 1;
    case TANK:
      return 0;
    default:
      return -1;
    }
  }

  public static int priority(RobotInfo[] enemies) {
    int result = -1;
    int currValue = 0;
    for (int i = 0; i < enemies.length; i++) {
      int value = 0;
      if (enemies[i] == null){
        continue;
      }
      switch (enemies[i].getType()) {
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
      // System.out.println("Value: " + value);
      if (value > currValue) {
        currValue = value;
        result = i;
      }
    }
    return result;
  }

  public static void loop() {
    try{
      findSquad();
      mydir = RobotUtils.randomDirection();
    }catch(Exception e){
      e.printStackTrace();
    }
    while(true){
      Globals.update();
      try{
        if (mode == ATTACK){
          if(target == null){
            //hasn't found him or he recently died
            int targetID = rc.readBroadcast(squad_channel + 1);
            if (targetID == 0){
              mode = ROAM;
              roam();
            }
            else{
              // if there is a target requested but not yet seen
              moveToAttack(targetID);
            }
          }
          else{
            // if target != null
            if (rc.canSenseRobot(target.ID)){
              target = rc.senseRobot(target.ID);
              rc.broadcast(squad_channel + 1, target.ID);
              rc.broadcast(squad_channel + 2, (int)target.location.x);
              rc.broadcast(squad_channel + 3, (int)target.location.y);
              attack();
            }
            else{
              int ogTarget = rc.readBroadcast(squad_channel + 1);
              if (target.ID == ogTarget){
                rc.broadcast(squad_channel + 1, 0);
                rc.broadcast(squad_channel + 2, 0);
                rc.broadcast(squad_channel + 3, 0);
                roam();
              }
              else{
                moveToAttack(ogTarget);
              }
              target = null;
              
            }
          }
        }
        else if (mode == DEFEND){
          // check defender's coords from defense channel, and stick near them
          // Dont know that this will actually be implemented yet, might just stick with attacking
          // gardener's troubles
        }
        else if (mode == ROAM){
          roam();
          // check defense every turn, if so then head to defend target
        }
      }catch(Exception e){
        e.printStackTrace();
      }
    }
  }
}