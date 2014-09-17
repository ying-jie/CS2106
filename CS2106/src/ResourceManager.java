
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;


public class ResourceManager {
	
	private static final int RUNNING = 0;
	private static final int READY= 1;
	private static final int BLOCKED = 2;
	private static final int MAX_RESOURCE = 4;
	private static final String fileName = "A0105709H.txt";

    enum COMMAND_TYPE {
	init, cr, de, req, rel, to, invalid
	};

	
	static class PCB{
		String PID;
		LinkedList<RCBvalue> otherResources = null;
		int statusName;
		Object statusPointer;
		PCB parent;
		LinkedList<PCB> children = null;
		int priority;
	
		public PCB(String name, int p ){
			this.PID = name;
			this.otherResources = new LinkedList<RCBvalue>();
			this.statusName = READY;
			this.statusPointer = readyList;
		    this.parent = self;
		    this.children = new LinkedList<PCB>(); 
		    this.priority = p; 
		    searchProcessesList.put(PID, this);
		}

		public boolean equals(PCB pcb) {
			if (pcb==null) return false;
			if (pcb==this) return true;
			if (this.PID.compareTo(pcb.PID)!=0)
				return false;
			return true;
		}

	}

	static class RCB{
		String RID;
		int statusInitial;
		int statusAvailable;
		LinkedList<PCB> waitingList = null;

		public RCB(String name, int i){
			this.RID = name;
			this.statusInitial = i;
			this.statusAvailable = i;
			this.waitingList = new LinkedList<PCB>();
			searchResourcesList.put(RID, this);
		}
	}

	static class RCBvalue{
		RCB rcb;
		int value;
		public RCBvalue(RCB rcb, int i){
			this.rcb = rcb;
			this.value = i;
		}
	}
	
	private static LinkedList< LinkedList<PCB>> readyList = new LinkedList< LinkedList<PCB>>();
	private static LinkedList<PCB> blockedList = new LinkedList<PCB>();
	private static HashMap<String, PCB> searchProcessesList = new HashMap<String, PCB>();
	private static LinkedList<RCB> resourcesList = new LinkedList<RCB>();
	private static HashMap<String, RCB> searchResourcesList = new HashMap<String, RCB>();
	static PCB self = null;
	
	public static void main(String[] args) {
		deleteFile(fileName);
		initial();	
		try {
			FileReader fr = new FileReader(args[0]);
			BufferedReader br = new BufferedReader(fr);
			String command;
			while((command=br.readLine()) != null){
				executeCommand(command);
			}
			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void deleteFile(String fname) {
		try{ 
    		File file = new File(fname);
    		if(!file.delete()){
    			System.out.println("Existing Output File cannot be deleted.");
    		}
    	}
		catch(Exception e){
    		e.printStackTrace();
    	}
	}


	private static void executeCommand(String userCommand) {
		if (userCommand.trim().equals("")) {
			writeToFile("\r\n\r\n");
			return ;
		}

		String commandTypeString = getFirstWord(userCommand);
		COMMAND_TYPE commandType = determineCommandType(commandTypeString);

		switch (commandType) {
		case init:
			initial();
			break;
		case cr:
			create(userCommand);
			break;
		case de:
			destroy(userCommand);
			break;
		case req:
			request(userCommand);
			break;
		case rel:
			release(userCommand);
			break;
		case to:
			time_out(userCommand);
			break;
		case invalid:
		default:
			writeToFile("error");
		}
	}
	

	private static COMMAND_TYPE determineCommandType(String commandTypeString) {

		if (commandTypeString.equalsIgnoreCase("init")) {
			return COMMAND_TYPE.init;
		} else if (commandTypeString.equalsIgnoreCase("cr")) {
			return COMMAND_TYPE.cr;
		} else if (commandTypeString.equalsIgnoreCase("de")) {
			return COMMAND_TYPE.de;
	    } else if (commandTypeString.equalsIgnoreCase("req")) {
			return COMMAND_TYPE.req;
	    } else if (commandTypeString.equalsIgnoreCase("rel")) {
			return COMMAND_TYPE.rel;
	    } else if (commandTypeString.equalsIgnoreCase("to")) {
			return COMMAND_TYPE.to;
		}  else {
			return COMMAND_TYPE.invalid;
		}
	}


	private static void initial() {
		readyList.clear();
		for (int i = 0; i<=2; i++ ){
		    LinkedList<PCB> e = new LinkedList<PCB>();
		    readyList.add(e);
		}
		resetResources();
		self = null;
		PCB init = new PCB("init", 0);
		LinkedList<PCB> e = readyList.get(0);
		e.add(init);
        scheduler();
	}

	private static void resetResources() {
		for (int i=1; i<=MAX_RESOURCE; i++){
			RCB rcb= new RCB(String.format("R%1$s", i),i);
			resourcesList.add(rcb);
		}
	}

	private static void create(String userCommand) {
		String commandContent = removeFirstWord(userCommand);
		String[] parameters = splitParameters(commandContent);
		if (parameters[0].length()!=1) {
			System.out.println("error");
            return;
		}
		String name = parameters[0];
		int p = Integer.parseInt(parameters[1]);
		if ((p!=1)&&(p!=2)){
			System.out.println("error");
            return;
		}
		PCB pcb = new PCB(name, p);
		self.children.add(pcb);
		insertReadyList(pcb);
		scheduler();
	}

	private static void insertReadyList(PCB pcb) {
        readyList.get(pcb.priority).add(pcb);
        
	}

	private static void destroy(String userCommand) {
		String commandContent = removeFirstWord(userCommand);
		if (commandContent.length()!=1) {
			writeToFile("error");
            return;
		}
		PCB current = searchProcessesList.get(commandContent);
		killTree(current);
		self=null;
		scheduler();
	}

	private static void killTree(PCB current) {

		while (!current.children.isEmpty()){
		      killTree(current.children.pollFirst());
		}

		if (current.statusPointer == readyList) {
			for (int i=2; i>0; i--)
			for (PCB pcb: readyList.get(i)){
				if (pcb.equals(current)){
					readyList.get(i).remove(pcb);
					break;
				}
			}
		} 
		else {
			for (PCB pcb: blockedList){
				if (pcb.equals(current)){
				blockedList.remove(pcb);
				break;
				}
			}
			if (!current.otherResources.isEmpty()){
		        RCB rcb = current.otherResources.getLast().rcb;
	    		for (PCB pcb: rcb.waitingList){
                    if (pcb.equals(current)){
                    	rcb.waitingList.remove(pcb);
                    	break;
                    }      					
			    }
		        rcb.waitingList.remove(current);
			}
		}
		freeResources(current);
	}
	
	private static void freeResources(PCB current) {
        
		while(!current.otherResources.isEmpty()){
			RCB rcb =current.otherResources.getFirst().rcb;
			rcb.statusAvailable += current.otherResources.getFirst().value;
			while ((!(rcb.waitingList.isEmpty()))&&
					(rcb.statusAvailable>=rcb.waitingList.getFirst().otherResources.getLast().value)){
				rcb.statusAvailable -= rcb.waitingList.getFirst().otherResources.getLast().value;
				readyList.get(rcb.waitingList.getFirst().priority).add(rcb.waitingList.getFirst());
				rcb.waitingList.getFirst().statusName = READY;
				rcb.waitingList.getFirst().statusPointer = readyList;
				rcb.waitingList.removeFirst();
			}
			current.otherResources.removeFirst();
		}		
	}


	private static void request(String userCommand) {
		String commandContent = removeFirstWord(userCommand);
		String[] parameters = splitParameters(commandContent);
		String rid = parameters[0];
		int unitsNeeded = Integer.parseInt(parameters[1]);
		if ((rid.toLowerCase().charAt(0)!='r') || (Integer.parseInt(rid.substring(1)) > MAX_RESOURCE)){
			writeToFile("error");
            return ;
		}
		if ((unitsNeeded<0) || (unitsNeeded > Integer.parseInt(parameters[0].substring(1)))){
			writeToFile("error");
            return ;
		}
		RCB rcb = getRCB(rid);
		RCBvalue rcbvalue= new RCBvalue(rcb, unitsNeeded);

		if (rcb.statusAvailable>=unitsNeeded) {
			rcb.statusAvailable-=unitsNeeded;
			self.otherResources.add(rcbvalue);
		}
		else {
			self.statusName = BLOCKED;
			self.statusPointer = blockedList;
			self.otherResources.add(rcbvalue);
			readyList.get(self.priority).removeFirst();
			blockedList.add(self);
			rcb.waitingList.add(self);
		}
		scheduler();
	}


	private static void release(String userCommand) {
		String commandContent = removeFirstWord(userCommand);
		String[] parameters = splitParameters(commandContent);
		String rid = parameters[0];
		int unitsReleased = Integer.parseInt(parameters[1]);
		if ((rid.toLowerCase().charAt(0)!='r')||(Integer.parseInt(rid.substring(1))>MAX_RESOURCE)){
			writeToFile("error");
            return ;
		}
		if (unitsReleased<0 ){
			writeToFile("error");
            return ;
		}
		RCB rcb = getRCB(rid);
		if (rcb.statusInitial-rcb.statusAvailable<unitsReleased){
			writeToFile("error");
            return ;
		}
		rcb.statusAvailable += unitsReleased;
		while ((!(rcb.waitingList.isEmpty()))&&
				(rcb.statusAvailable>=rcb.waitingList.getFirst().otherResources.getLast().value)){
			rcb.statusAvailable -= rcb.waitingList.getFirst().otherResources.getLast().value;
			readyList.get(rcb.waitingList.getFirst().priority).add(rcb.waitingList.getFirst());
			rcb.waitingList.getFirst().statusName = READY;
			rcb.waitingList.getFirst().statusPointer = readyList;
			rcb.waitingList.removeFirst();
		}
		scheduler();
	}
	
	private static RCB getRCB(String rid) {
	    return searchResourcesList.get(rid.toUpperCase());
	}

	
	private static void time_out(String userCommand) {
		String commandContent = removeFirstWord(userCommand);
		if (!commandContent.equals("")) {
			writeToFile("error");
            return ;
		}
		self.statusName = READY;
		readyList.get(self.priority).removeFirst();
		readyList.get(self.priority).add(self);
		scheduler();
	}
	
	private static void scheduler() {
		int i=2;

		while (readyList.get(i).peekFirst()==null){
			i--;
		}
		PCB p = readyList.get(i).peekFirst();

	    if (self==null){
    	    self = p;
        	self.statusName = RUNNING;
        
        }
	    else if ((self.priority<p.priority)||(self.statusName!=RUNNING)){
			
	    	if (self.statusName!=BLOCKED)
			    self.statusName = READY;
			self = p;
        	self.statusName = RUNNING;
        
		}
		writeToFile(self.PID);
		
	}
	private static void writeToFile(String pID) {
		try {
			FileWriter fw = new FileWriter(fileName,true);
			BufferedWriter bw = new BufferedWriter(fw);
			if (pID.equals("\r\n\r\n") || pID.equals("error")) {
				bw.write(pID);
			} else  {
				bw.write(pID+" ");
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static String removeFirstWord(String userCommand) {
		return userCommand.replace(getFirstWord(userCommand), "").trim();
	}

	private static String getFirstWord(String userCommand) {
		String commandTypeString = userCommand.trim().split("\\s+")[0];
		return commandTypeString;
	}

	private static String[] splitParameters(String commandParametersString) {
		String[] parameters = commandParametersString.trim().split("\\s+");
		return parameters;
	}
}
