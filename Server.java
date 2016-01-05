import java.io.*; 
import java.net.*; 
import java.util.*;
import java.text.*;

  
class TorrentServer_v1{

  public static void main(String args[]) throws Exception { 
    
    //Hashmap that keeps track of peers and files they shared i.e. maps file-names to IPAddresses. 
    Map<String,String> tracker = new HashMap<String,String>();
   
    //For log file
    DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");


     try{ 
      int serverPort = 15000;
      DatagramSocket serverSocket = new DatagramSocket(serverPort); 
      
      //Server continuosuly listens to incoming requests.
      while(true){ 
          byte[] receiveData = new byte[4096]; 
          DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
           
          //Obtaining data from client/receiver  
          serverSocket.receive(receivePacket); 
          String[] rdata = new String(receivePacket.getData(),0,receivePacket.getLength()).split(":");
          String command = rdata[0];
          String fileName = rdata[1];
          InetAddress clientIPAddress = receivePacket.getAddress(); 
          int clientPort = receivePacket.getPort(); 

          //Opening a log file.
          File fout = new File("serverlog.txt");
          FileOutputStream fos = new FileOutputStream(fout);
          BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
          Date dateobj = new Date();
          bw.write(df.format(dateobj));
          bw.newLine();
      
          //Preparing data for receiver
          String sdata = null;
          if(command.equals("Search")){
            
            String log = "Client " + clientIPAddress.toString() + " requested " + fileName;
            bw.write(log);
            bw.newLine();

            if(tracker.containsKey(fileName)){
              sdata = tracker.get(fileName);
              bw.write("Requested file found");
              bw.newLine();
            }
            else{
              sdata = "Not Found";
              bw.write("Requested file not found");
              bw.newLine();
            }
          }

          //Updating tracker using data received from receiver
          if(command.equals("Share")){

            String log = "Client " + clientIPAddress.toString() + " shared " + fileName;
            bw.write(log);
            bw.newLine();

            sdata = "Shared";

            if(tracker.containsKey(fileName)){
              String temp = tracker.get(fileName).concat(";" + clientIPAddress.toString());
              tracker.put(fileName,temp);
            }
            else{
              String temp = clientIPAddress.toString();
              tracker.put(fileName,temp);
            } 
          }

          //Sending data to client/receiver
          byte[] sendData = sdata.getBytes();
          DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIPAddress, clientPort); 
          serverSocket.send(sendPacket);

          //Closing log file.
          bw.close();
        } 
     }
     catch (SocketException ex) {
        System.out.println("UDP Port 15000 is occupied.");
        System.exit(1);
     }
  } 
}