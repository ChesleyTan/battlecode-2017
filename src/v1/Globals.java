package v1;

import battlecode.common.*;

import java.util.Random;

public class Globals {

	public static final boolean DEBUG = false;

	public static RobotController rc;
	public static MapLocation here;
	public static Team us;
	public static Team them;
	public static int myID;
	public static RobotType myType;
	public static Random rand;

	public static void init(RobotController theRC){
		rc = theRC;
		us = rc.getTeam();
		them = us.opponent();
		myID = rc.getID();
		myType = rc.getType();
		here = rc.getLocation();
		rand = new Random(theRC.getID());
	}
	
	public static void update(){
		here = rc.getLocation();
	}
}