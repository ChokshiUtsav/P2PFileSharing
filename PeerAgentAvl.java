import java.net.*;
import java.io.*;
import java.util.*;

public class PeerAgentAvl{

	public static void main(String args[]){

		//Asking user for IP address.
		Scanner scan = new Scanner(System.in);
		System.out.println("Enter remote machines's IP Address : ");
		String ipAddress = scan.nextLine();
		System.out.println("Enter remote machines's Port Number : ");
		int portNumber = scan.nextInt();

		if(isRemotePortInUse(ipAddress,portNumber)){
			System.out.println("Remote Service is available");
		}
		else{
			System.out.println("Remote Service is not available");
		}
	}
	
	private static boolean isRemotePortInUse(String hostName, int portNumber) {
	    try {
	        // Socket to try to open a REMOTE port
	        new Socket(hostName, portNumber).close();
	        // this port is in use on the remote machine !
	        return true;
	    } catch(Exception e) {
	        // remote port is closed, nothing is running on
	        return false;
	    }
	}
}