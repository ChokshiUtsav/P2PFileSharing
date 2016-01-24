import java.util.*;
import java.io.*;
import java.text.*;
import java.net.*;
import java.nio.file.Paths;
import java.nio.file.Path;


class UserInputThread implements Runnable{
	private Thread t;
	private static String TorrentServerAddress = "127.0.0.1";
	private static int TorrentServerPort = 15000;
	private static int TorrentClientPort = 16200;

	public void start(){
		if(t == null){
			t = new Thread(this);
			t.start();
		}
	}

	public void run(){

		try{
				Scanner scan = new Scanner(System.in);
				System.out.println("Please enter torrent server IP address to connect with :");
				UserInputThread.TorrentServerAddress = scan.nextLine();

				//Intializing screen
				System.out.println("****************************************************************");
				System.out.println("Following are list of functionalities provided by torrent : ");
				System.out.println("1.Search");
				System.out.println("2.Download");
				System.out.println("3.Share");	
				System.out.println("4.Exit");		
				System.out.println("****************************************************************");

				
				while(true){

					//Scanning for user input
					System.out.println("Please enter command (Search/Share/Download/Exit) : ");
					String commandName = scan.nextLine();
					
					if(commandName.equals("Exit")){
						PeerAgent.exitFlag = 1;
						break;
					}

					if(commandName.equals("Search") || commandName.equals("Share")){
						System.out.println("Please enter file-name to be shared/searched : ");
						String fileName = scan.nextLine();

						//Connecting to server
						String serverHostname = new String (UserInputThread.TorrentServerAddress);
						InetAddress ServerIPAddress = InetAddress.getByName(serverHostname); 
						DatagramSocket clientSocket = new DatagramSocket();

						//Preparing and Sending packet to server
						String sdata = commandName + ":" + fileName;
						byte[] sendData = sdata.getBytes();         
						DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ServerIPAddress, UserInputThread.TorrentServerPort); 
						clientSocket.send(sendPacket);

						//Receiving packet from server
						byte[] receiveData = new byte[4096]; 
						DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
						clientSocket.setSoTimeout(10000);
						try {
			           		clientSocket.receive(receivePacket);
			           		if(commandName.equals("Search")){
			           			String[] peerAddress = new String(receivePacket.getData(),0,receivePacket.getLength()).split(";");
			           			if(peerAddress[0].equals("Not Found")){
			           				System.out.println("Sorry, No peers have the requested file");
			           			}
			           			else{
			           				System.out.println("Please use Download command to get file from peer.");
			           				System.out.println("Following peers have file : " + fileName);
			           				for(String s : peerAddress){
			           					String[] temp = s.split("#");
			           					System.out.println("IPAddress : "+ temp[0]);
			           					System.out.println("Location : "+ temp[1]);	
			           				}
			           			}
			           		}
			           		else{
			           			System.out.println("File : " + fileName + " is shared.");
			           		} 
			           	}
			           	 catch (SocketTimeoutException ste){
			           		System.out.println ("Unable to connect to server. Please try again latter.");
			      		}
			 			clientSocket.close();
			 		}

			 		if(commandName.equals("Download")){
			 			System.out.println("Please enter complete file-path of file to be downloaded : ");
						String fileName = scan.nextLine();
						//System.out.println(fileName);

						System.out.println("Please enter peerIPAddress : ");
						String address = scan.nextLine();
						//System.out.println(address);

						Socket socket = new Socket(address,UserInputThread.TorrentClientPort);
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						oos.writeObject(fileName);


						Path p = Paths.get(fileName);
						String newFileName = p.getFileName().toString();
						byte [] bytearray  = new byte [4096];
						DataInputStream dis = new DataInputStream(socket.getInputStream());
						FileOutputStream fos = new FileOutputStream(newFileName);
						DataOutputStream dos = new DataOutputStream(fos);
						int bytesRead = 0;

						while((bytesRead=dis.read(bytearray))>0){
		        			dos.write(bytearray,0,bytesRead);
		        		}

		        		dos.flush();
		        		dis.close();
		        		dos.close();
			            socket.close();
			 		}
				}
			}
			catch(IOException e){
				System.out.println("IOException1!!");
				e.printStackTrace();
			}
			catch(Exception e){
				System.out.println("Exception!!");	
			}
	}
}


class FileTransferThread implements Runnable{
	private Thread t;
	//private static String TorrentServerAddress = "10.2.133.142";
	private static int TorrentServerPort = 15000;
	private static int TorrentClientPort = 16200;

	public void start(){
		if(t == null){
			t = new Thread(this);
			t.start();
		}
	}

	public void run(){

		//For log file
	    DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");

		try{

			//Creating socket for handling other peer's request
			ServerSocket server = new ServerSocket(FileTransferThread.TorrentClientPort);

			while(true){

				if(PeerAgent.exitFlag == 1){
             		break;
             	 }

				//creating socket and waiting for client connection
				Socket socket = server.accept();

				File fout = new File("agentlog.txt");
          		FileOutputStream fos = new FileOutputStream(fout,true);
          		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
          		Date dateobj = new Date();
          		bw.write(df.format(dateobj));
          		bw.newLine();

				//Recieve filename from client
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				String fileName = (String) ois.readObject();

				String log = "Peer requested " + fileName;
            	bw.write(log);
            	bw.newLine();
				
				//Creating file stream
				File transferFile = new File (fileName);
		        byte [] bytearray  = new byte [4096];
		        FileInputStream fin = new FileInputStream(transferFile);
		        DataInputStream dis = new DataInputStream(fin);
		        DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		        int bytesRead = 0;
		       		        
		        log = "File transfer intiated";
		        bw.write(log);
            	bw.newLine();
	
		        while((bytesRead=dis.read(bytearray))>0){
		        	dos.write(bytearray,0,bytesRead);
		        }

		        log = "File transfer completed";
		        bw.write(log);
            	bw.newLine();

		        //Writing file to output stream	        
		        dos.flush();
		        dis.close();
		        dos.close();
            	bw.close();
		        socket.close();
			}
		}
		catch(IOException e){
			System.out.println("IOException2!!");
			e.printStackTrace();
		}
		catch(Exception e){
			System.out.println("Exception2!!");	
		}
	}
}

public class PeerAgent{

	//Flag becomes '1' when user fires 'Exit' Command.
	public static int exitFlag = 0;
	
	public static void main(String args[]){

		System.out.println("Torrent Agent is started....");

		//Creating thread to handle user's inputs
		UserInputThread t1 = new UserInputThread();
		t1.start();

		//Creating thread to handle other peer's request to access data of this user
		FileTransferThread t2 = new FileTransferThread();
		t2.start();
	}
}