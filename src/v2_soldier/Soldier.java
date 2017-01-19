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
  private static boolean isMovingAroundTree=false;
  private static TreeInfo tree = null;
  private static MapLocation targetAroundTreeDestination = null;

  private static void dodge(BulletInfo[] bullets, RobotInfo[] robots, MapLocation targetLocation) throws GameActionException {
    float sumX = 0;
    float sumY = 0;
    if (targetLocation != null){
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
      if (a == 0 && b == 0){
        a = 0.01f;
      }
      float c = x0 * y1 - y0 * x1;
      float distance = (float) (Math.abs(a * here.x + b * here.y + c)
          / Math.sqrt(a * a + b * b));
      if (distance <= 2.5){
        float x2 = (float) ((b * (b * here.x - a * here.y) - a * c)
            / (a * a + b * b));
        float y2 = (float) ((a * (a * here.y - b * here.x) - b * c)
            / (Math.pow(a, 2) + Math.pow(b, 2)));
        MapLocation destLocation = new MapLocation(x2,y2);
        Direction away = new MapLocation(x2, y2).directionTo(here);
        if (away == null){
          away = here.directionTo(i.getLocation()).rotateLeftDegrees(90);
        }
        System.out.println("distance: " + distance);
        float weighted = (float) Math.pow((RobotType.SOLDIER.bulletSightRadius - distance / myType.bulletSightRadius), 2);
        //float weighted = RobotType.SOLDIER.bulletSightRadius / distance;
        System.out.println("weighted: " + weighted); 
        rc.setIndicatorDot(here.add(away,  1), 255, 0, 0);
        sumX += away.getDeltaX(weighted);
        sumY += away.getDeltaY(weighted);
      }
    }

    for (RobotInfo r : robots) {
      Direction their_direction = r.location.directionTo(here);
      float their_distance = ((RobotType.SOLDIER.sensorRadius - here.distanceTo(r.location) + r.getRadius())) * ((RobotType.SOLDIER.sensorRadius - here.distanceTo(r.location) + r.getRadius()));
      System.out.println(their_distance);
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
        float baseValue = ((myType.sensorRadius - here.distanceTo(t.location) + t.getRadius())) * ((myType.sensorRadius - here.distanceTo(t.location) + t.getRadius()));
        float their_distance = baseValue * myType.strideRadius;
        sumX += their_direction.getDeltaX(their_distance);
        sumY += their_direction.getDeltaY(their_distance);
      }
    }
    
    if (!(Clock.getBytecodesLeft() < 2000)){
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
  
  private static void findSquad() throws GameActionException {
    int i = DEFENSE_START_CHANNEL;
    while (i < DEFENSE_END_CHANNEL) {
      int squad_count = rc.readBroadcast(i);
      System.out.println(squad_count);
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
    BulletInfo[] bullets = rc.senseNearbyBullets(EvasiveSoldier.BULLET_DETECT_RADIUS);
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
      RobotInfo[] robots = rc.senseNearbyRobots(EvasiveSoldier.ENEMY_DETECT_RADIUS);
      System.out.println("dodging");               
      EvasiveSoldier.move(bullets, robots, null);
    }
  }
  /*
   * Assumes target has already been seen and is a local variable
   * Makes no assumption about movement
   */
  private static void attack(RobotInfo target) throws GameActionException{
    System.out.println("attacking");
    if(!rc.hasMoved()){
      BulletInfo[] bullets = rc.senseNearbyBullets(EvasiveSoldier.BULLET_DETECT_RADIUS);
      RobotInfo[] robots = rc.senseNearbyRobots(EvasiveSoldier.ENEMY_DETECT_RADIUS);
      MapLocation targetLocation = target.location;
      EvasiveSoldier.move(bullets, robots, here.directionTo(targetLocation));
    }
    if (rc.canFireTriadShot() && TargetingUtils.clearShot(here, target)){
      Direction towardsEnemy = here.directionTo(target.location);
      if (here.distanceTo(target.location) <= 3.5){
        rc.fireTriadShot(towardsEnemy);
      }
      else{
        rc.fireSingleShot(towardsEnemy);
      }
    }
  }
  
  /* Moves in the direction of attacking and fires if reasonable
   * Assumes soldier has neither moved nor attacked
   * Also assumes that there is a target
   */
  private static void moveToDefend(int targetID) throws GameActionException{
    checkBulletsAndDodge();
    RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
    if(rc.canSenseRobot(targetID)){
      target = rc.senseRobot(targetID);
      rc.broadcast(squad_channel + 2, (int)target.location.x);
      rc.broadcast(squad_channel + 3, (int)target.location.y);
      mode = DEFEND;
      defend();
    }
    else if (enemies.length != 0){
      int index = priority(enemies);
      if (index != -1){
        RobotInfo newTarget = enemies[priority(enemies)];
        attack(newTarget);
      }
    }
    else{
      float xCor = rc.readBroadcast(squad_channel + 2);
      float yCor = rc.readBroadcast(squad_channel + 3);
      MapLocation targetLocation = new MapLocation(xCor, yCor);
      mydir = here.directionTo(targetLocation);
      if (!rc.hasMoved() && !RobotUtils.tryMove(mydir, 10, 9)){
        int attempts = 0;
        while(!rc.canMove(mydir) && attempts < 20){
          mydir = RobotUtils.randomDirection();
          attempts ++;
        }
        if (rc.canMove(mydir)){
          rc.move(mydir);
        }
      }
      if (here.distanceTo(targetLocation) <= 5){
        rc.broadcast(squad_channel + 1, -1);
        rc.broadcast(squad_channel + 2, 0);
        rc.broadcast(squad_channel + 3, 0);
      }
    }
  }
  
  private static void roam() throws GameActionException{
    int targetID = rc.readBroadcast(squad_channel + 1);
    if (targetID != -1){
      mode = DEFEND;
      moveToDefend(targetID);
    }
    else{
      checkBulletsAndDodge();
      RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
      int priority = priority(enemies);
      while (priority != -1){
        RobotInfo enemy = enemies[priority];
        MapLocation destination = enemy.location;
        target = enemy;
        //rc.broadcast(squad_channel + 1, target.ID);
        //rc.broadcast(squad_channel + 2, (int)target.location.x);
        //rc.broadcast(squad_channel + 3, (int)target.location.y);
        mode = ATTACK;
        if (!rc.hasMoved()){
          boolean successfulMove = RobotUtils.tryMove(here.directionTo(destination), 10, 6);
          if (successfulMove){
            break;
          }
          else{
            enemies[priority] = null;
            priority = priority(enemies);
          }
        }
      }
      if (priority != -1){
        if (rc.canFireTriadShot() && TargetingUtils.clearShot(here, target)){
          Direction towardsEnemy = here.directionTo(target.location);
          if (here.distanceTo(target.location) <= 3.5){
            rc.fireTriadShot(towardsEnemy);
          }
          else{
            rc.fireSingleShot(towardsEnemy);
          }
        }
      }
      else{
        if (!rc.hasMoved()){
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
  }
  
 
  private static void survey(RobotInfo target) throws GameActionException{
    System.out.println("defending target");
    RobotInfo[] enemies = rc.senseNearbyRobots(-1, them);
    if (enemies.length != 0){
      int priority = priority(enemies);
      attack(enemies[priority]);
    }
    else{
      RobotUtils.tryMove(here.directionTo(target.location), 15, 8);
    }
  }
  /*
   * Defend the person who's considered "target"
   */
  private static void defend() throws GameActionException{
    // We're assuming the person hasn't found the target yet
    if (target == null){
      int targetID = rc.readBroadcast(squad_channel + 1);
      if(targetID == -1){
        mode = ROAM;
        roam();
      }
      else{
        // If he can see the person he's supposed to defend
        if (rc.canSenseRobot(targetID)){
          System.out.println("acquired defense target");
          target = rc.senseRobot(targetID);
          rc.broadcast(squad_channel + 2, (int)target.location.x);
          rc.broadcast(squad_channel + 3, (int)target.location.y);
          if (here.distanceTo(target.location) <= 5){
            survey(target);
          }
          else{
            int xCor = rc.readBroadcast(squad_channel + 2);
            int yCor = rc.readBroadcast(squad_channel + 3);
            MapLocation destination = new MapLocation(xCor, yCor);
            BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, them);
            EvasiveSoldier.move(nearbyBullets, nearbyRobots, here.directionTo(destination));
          }
        }
        else{
          //Either he's dead or I have to move to him
          int xCor = rc.readBroadcast(squad_channel + 2);
          int yCor = rc.readBroadcast(squad_channel + 3);
          MapLocation targetLocation = new MapLocation(xCor, yCor);
          if(here.distanceTo(targetLocation) <= 5){
            // he's dead
            rc.broadcast(squad_channel + 1, -1);
            rc.broadcast(squad_channel + 2, 0);
            rc.broadcast(squad_channel + 3, 0);
            target = null;
            mode = ROAM;
            roam();
          }
          else{
            // I have to find him
            BulletInfo[] nearbyBullets = rc.senseNearbyBullets();
            RobotInfo[] nearbyRobots = rc.senseNearbyRobots(-1, them);
            EvasiveSoldier.move(nearbyBullets, nearbyRobots, here.directionTo(targetLocation));
          }
        }
      }
    }
    else{
      System.out.println("defending target: " + target.ID);
      int broadcasted = rc.readBroadcast(squad_channel + 1);
      if (broadcasted != target.ID){
        target = null;
      }
      else{
        if (here.distanceTo(target.location) <= 5){
          survey(target);
        }
        else{
          RobotUtils.tryMove(here.directionTo(target.location), 15, 8);
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
      return -1;
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
        value = -1;
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
      if (value > currValue) {
        currValue = value;
        result = i;
      }
    }
    return result;
  }

  public static void loop() {
    try{
      //Start out and join a squad
      EvasiveSoldier.init();
      findSquad();
      MapLocation[] enemies = rc.getInitialArchonLocations(them);
      mydir = here.directionTo(enemies[0]);
      mode = ROAM;
    }catch(Exception e){
      e.printStackTrace();
    }
    while(true){
      Globals.update();
      try{
        System.out.println(mode);
        if (mode == ATTACK){
          if(target == null){
            //hasn't found him or he recently died
            int targetID = rc.readBroadcast(squad_channel + 1);
            if (targetID == -1){
              System.out.println("null target, roaming");
              mode = ROAM;
              roam();
            }
            else{
              // if there is a target requested but not yet seen
              System.out.println("null target, moving to attack");
              moveToDefend(targetID);
            }
          }
          else{
            System.out.println("target ID: " + target.ID);
            // if target != null
            if (rc.canSenseRobot(target.ID)){
              target = rc.senseRobot(target.ID);
              rc.broadcast(squad_channel + 1, target.ID);
              rc.broadcast(squad_channel + 2, (int)target.location.x);
              rc.broadcast(squad_channel + 3, (int)target.location.y);
              attack(target);
            }
            else{
              int ogTarget = rc.readBroadcast(squad_channel + 1);
              if (target.ID == ogTarget){
                target = null;
                mode = ROAM;
                rc.broadcast(squad_channel + 1, -1);
                rc.broadcast(squad_channel + 2, 0);
                rc.broadcast(squad_channel + 3, 0);
                roam();
              }
              else{
                moveToDefend(ogTarget);
              }
              target = null;
              
            }
          }
        }
        else if (mode == DEFEND){
          defend();
        }
        else if (mode == ROAM){
          roam();
          // check defense every turn, if so then head to defend target
        }
        RobotUtils.donateEverythingAtTheEnd();
        RobotUtils.shakeNearbyTrees();
        Clock.yield();
      }catch(Exception e){
        e.printStackTrace();
      }
    }
  }
}