import java.io.*; 
import java.net.*; 
import java.util.*;
import java.text.*;
import java.nio.file.Paths;
import java.nio.file.Path;

class Server{

  public static void main(String args[]) throws Exception { 
    
    //Hashmap that keeps track of peers and files they have shared i.e. maps file-names to IPAddresses. 
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
          String completeFileName = rdata[1];
          InetAddress clientIPAddress = receivePacket.getAddress(); 
          int clientPort = receivePacket.getPort(); 

          //Opening a log file.
          File fout = new File("serverlog.txt");
          FileOutputStream fos = new FileOutputStream(fout,true);
          BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
          Date dateobj = new Date();
          bw.write(df.format(dateobj));
          bw.newLine();
      
          //Preparing data for receiver
          String sdata = null;
          if(command.equals("Search")){
            
            //Writing to Log file
            String log = "Client " + clientIPAddress.toString() + " requested " + completeFileName;
            bw.write(log);
            bw.newLine();

            //Searching for requested file
            if(tracker.containsKey(completeFileName)){
              sdata = tracker.get(completeFileName);
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

            Path p = Paths.get(completeFileName);
            String fileName = p.getFileName().toString();

            //Writing to Log file
            String log = "Client " + clientIPAddress.toString() + " shared " + fileName;
            bw.write(log);
            bw.newLine();

            sdata = "Shared";

            //Adding shared file
            if(tracker.containsKey(fileName)){
              String temp = tracker.get(fileName).concat(";" + clientIPAddress.toString()+"#"+completeFileName);
              tracker.put(fileName,temp);
            }
            else{
              String temp = clientIPAddress.toString()+"#"+completeFileName;
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