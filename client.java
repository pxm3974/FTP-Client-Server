import java.net.*;
import java.io.*;

public class client {
   private static DataOutputStream out ;
   private static DataInputStream in ;
   private static OutputStream dataOs;
   private static InputStream dataIs;

   private static byte[] buff = new byte[1024];

   public static void main(String [] args) {
      BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));
      try {
         System.out.print("PORT <SP> <host-port> <CRLF>\n>>> ");
         String[] command;
         Socket client;
         while(true){
            command = sc.readLine().split(" ");
            if (!command[0].equalsIgnoreCase("port")) {
               System.out.print("Invalid command!\n>>> ");
            }
            else{
               System.out.println(command[0]+" "+command[1]);
               String[] server = command[1].split(":");
               System.out.println("Connecting to " + server[0] + " on port " + server[1]);
               try{
                  client = new Socket(server[0], Integer.parseInt(server[1]));
                  break;
               }
               catch(ConnectException c){
                  System.out.print("Server not found\n>>> ");
               }
            }
         }
         dataOs = client.getOutputStream();
         dataIs = client.getInputStream();

         System.out.println("Just connected to " + client.getRemoteSocketAddress());
         OutputStream outToServer = client.getOutputStream();
         out = new DataOutputStream(outToServer);
         
         out.writeUTF("Hello from " + client.getLocalSocketAddress());
         InputStream inFromServer = client.getInputStream();
         in = new DataInputStream(inFromServer);


         String username,response,reply,cmd;
         System.out.println("Login required\n\nUSER <SP> <username> <CRLF>");
         while(true){
            response = sc.readLine();
            if (response.split(" ")[0].equalsIgnoreCase("user") || response.split(" ")[0].equalsIgnoreCase("pass")) {
               out.writeUTF(response);
            }else {
               System.out.println("No permission. Login Needed.");
               continue;
            }
            response = in.readUTF();
            if (response.equals("1")) {
               break;
            }
            else if (response.equals("0")) {
               System.out.println("Bad Command/Credentials !");
            }
            else {
               System.out.println("\n\nPASS <SP> <password> <CRLF>");
            }
         }
         System.out.print("Login success\n\nList of commands for this server:\nRETR <SP> <pathname> <CRLF>\nSTOR <SP> <pathname> <CRLF>\nCWD  <SP> <pathname> <CRLF>\nMKD  <SP> <pathname> <CRLF>\nRMD  <SP> <pathname> <CRLF>\nDELE <SP> <pathname> <CRLF>\nLIST [<SP> <pathname>] <CRLF>\nPWD  <CRLF>\nNOOP <CRLF>\nHELP <CRLF>\nQUIT <CRLF>\n\n>>> ");
         try{
            while(true){
               cmd = sc.readLine();
               if (cmd.equalsIgnoreCase("help")) {
                  System.out.print("Login success\n\nList of commands for this server:\nRETR <SP> <pathname> <CRLF>\nSTOR <SP> <pathname> <CRLF>\nCWD  <SP> <pathname> <CRLF>\nMKD  <SP> <pathname> <CRLF>\nRMD  <SP> <pathname> <CRLF>\nDELE <SP> <pathname> <CRLF>\nLIST [<SP> <pathname>] <CRLF>\nPWD  <CRLF>\nNOOP <CRLF>\nHELP <CRLF>\nQUIT <CRLF>\n\n>>> ");
                  continue;
               }
               out.writeUTF(cmd);
               reply = in.readUTF();
               if (reply.equals("")) {
                  System.out.println("Goodbye !");
                  break;
               }
               else if (reply.equalsIgnoreCase("portc")) {
                  System.out.print("Already connected to server !\n\n>>> ");
               }
               else if (reply.equalsIgnoreCase("sendit")) {
                  try{
                     stor(cmd.split(" ")[1]);
                  }
                  catch(NullPointerException n){
                     out.writeUTF("0");
                     System.out.println("File not found");
                  }
               }
               else if (reply.equalsIgnoreCase("sending")) {
                  retr(cmd.split(" ")[1]);
               }
               else if (reply.equalsIgnoreCase("inc")) {
                  System.out.print("No Command provided !\n\n>>> ");
               }
               else if (reply.equalsIgnoreCase("incr")) {
                  System.out.print("Not a valid command !\n\n>>> ");
               }
               else{
                  System.out.println(reply+"\n");
                  System.out.print(">>> ");
               }
            }
         }
         catch(SocketException s){
            System.out.println("Connection Broken !");
         }
         
         client.close();
      }catch(IOException e) {
         e.printStackTrace();
      }
   }
   private static boolean stor(String fileName) throws IOException {
      boolean result = false;
      
      File inFile = new File(fileName);
      try {
         FileInputStream fileInputStream = new FileInputStream(inFile);
         Long length = inFile.length();
         out.writeUTF(inFile.length()+"");
         int recv = 0;int flag=0;
         Long datasent=0L;
         while ((recv = fileInputStream.read(buff, 0, buff.length)) > 0) {
            dataOs.write(buff,0,recv);
            datasent += 1024L;
            int percent = (int)(datasent*100/length);
            if (percent%10 == 0) {
               if (flag !=percent) {
                  System.out.println("Transmitted: "+percent+"%"); 
               }
               flag = percent;
            }
         }
         dataOs.flush();
         fileInputStream.close();
         System.out.print("File Transfered successfully !\n\n>>> ");
         
      } catch (FileNotFoundException e) {
         System.out.print("File " + fileName + " was not found!\n\n>>> ");
         out.writeUTF("-1");
      } catch (IOException e) {
         System.out.print("Problem transfering file : \n\n>>> ");
      }

      
      return result;
   }
   private static boolean retr(String fileName) throws IOException {
      boolean result = false;
      
      File outFile = new File(fileName);
      try {
         FileOutputStream fileOutputStream = new FileOutputStream(outFile);
         String response = in.readUTF();
         if (response.equals("-1")) {
             System.out.print("File Not Found!\n\n>>> ");
            return false;
         }
         long size = Long.valueOf(response);
         long len = 0;
         int recv = 0;int flag=0;
         Long datasent=0L;
         if (size > 0) {
            while (len + recv < size) {
               len += recv;
               recv = dataIs.read(buff,0,buff.length);               
               fileOutputStream.write(buff,0,recv);

               datasent += 1024L;
               int percent = (int)(datasent*100/size);
               if (percent%10 == 0) {
                  if (flag !=percent) {
                     System.out.println("Transmitted: "+percent+"%"); 
                  }
                  flag = percent;
               }
            }
         }
         fileOutputStream.close();
         System.out.print("File Received successfully !\n\n>>> ");
         
      } catch (FileNotFoundException e) {
         System.out.print("File " + fileName + " was not found!\n\n>>> ");
         out.writeUTF("-1");
      } catch (IOException e) {
         System.out.print("Problem transfering file for put: \n\n>>> ");
      }

      
      return result;
   }
}