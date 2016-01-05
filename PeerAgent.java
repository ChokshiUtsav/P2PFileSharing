import java.util.*;
import java.io.*;
import java.text.*;
import java.net.*;

class UserInputThread implements Runnable{
	private Thread t;
	private static String TorrentServerAddress = "127.0.0.1";
	private static int TorrentServerPort = 15000;
	private static int TorrentClientPort = 16000;

	public void start(){
		if(t == null){
			t = new Thread(this);
			t.start();
		}
	}

	public void run(){

		try{
				//Intializing screen
				System.out.println("****************************************************************");
				System.out.println("Following are list of functionalities provided by torrent : ");
				System.out.println("1.Search");
				System.out.println("2.Download");
				System.out.println("3.Share");	
				System.out.println("4.Exit");		
				System.out.println("****************************************************************");

				Scanner scan = new Scanner(System.in);
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
			           			String[] peerIPAddress = new String(receivePacket.getData(),0,receivePacket.getLength()).split(";");
			           			if(peerIPAddress[0].equals("Not Found")){
			           				System.out.println("Sorry, No peers have requested file");
			           			}
			           			else{
			           				System.out.println("Please use Download command to get file from peer.");
			           				System.out.println("Following peers have file : " + fileName);
			           				for(String s : peerIPAddress){
			           					System.out.println(s);
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
			 			System.out.println("Please enter file-name to be downloaded : ");
						String fileName = scan.nextLine();
						//System.out.println(fileName);

						System.out.println("Please enter peerIPAddress : ");
						String address = scan.nextLine();
						//System.out.println(address);

						Socket socket = new Socket(address,UserInputThread.TorrentClientPort);
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						oos.writeObject(fileName);
						
						int filesize=1022386; 
			            int bytesRead;
			            int currentTot = 0;
			            byte [] bytearray  = new byte [filesize];
			            InputStream is = socket.getInputStream();
			            FileOutputStream fos = new FileOutputStream(fileName+"_torrent"+".mp3");
			            BufferedOutputStream bos = new BufferedOutputStream(fos);
			            bytesRead = is.read(bytearray,0,bytearray.length);
			            currentTot = bytesRead;

			            do{
			               bytesRead =
			                  is.read(bytearray, currentTot, (bytearray.length-currentTot));
			               if(bytesRead >= 0) currentTot += bytesRead;
			            }
			            while(bytesRead > -1);

			            bos.write(bytearray, 0 , currentTot);
			            bos.flush();
			            bos.close();
			            socket.close();
			 		}
				}
			}
			catch(IOException e){
				System.out.println("IOException!!");
			}
			catch(Exception e){
				System.out.println("Exception!!");	
			}
	}
}


class FileTransferThread implements Runnable{
	private Thread t;
	private static String TorrentServerAddress = "127.0.0.1";
	private static int TorrentServerPort = 15000;
	private static int TorrentClientPort = 16000;

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
				//creating socket and waiting for client connection
				Socket socket = server.accept();

				File fout = new File("agentlog.txt");
          		FileOutputStream fos = new FileOutputStream(fout);
          		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
          		Date dateobj = new Date();
          		bw.write(df.format(dateobj));
          		bw.newLine();

				//Recieve filename from client
				ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
				String fileName = (String) ois.readObject();

				//String log = "Client " + clientIPAddress.toString() + " requested " + fileName;
				String log = "Peer requested " + fileName;
            	bw.write(log);
            	bw.newLine();
				
				//Creating file stream
				File transferFile = new File (fileName);
		        byte [] bytearray  = new byte [(int)transferFile.length()];
		        FileInputStream fin = new FileInputStream(transferFile);
		        BufferedInputStream bin = new BufferedInputStream(fin);
		        bin.read(bytearray,0,bytearray.length);
		        OutputStream os = socket.getOutputStream();

		        log = "File transfer intiated";
		        bw.write(log);
            	bw.newLine();

		        //Writing file to output stream
		        os.write(bytearray,0,bytearray.length);
		        os.flush();
		        bin.close();

		        log = "File transfer completed";
		        bw.write(log);
            	bw.newLine();

            	bw.close();
		        socket.close();
			}
		}
		catch(IOException e){
			System.out.println("IOException!!");
		}
		catch(Exception e){
			System.out.println("Exception!!");	
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