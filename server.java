import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.String;

public class server extends Thread {
   private static ServerSocket serverSocket;
   static private Socket server;
   
   static HashMap<String, Integer> users = new HashMap<String, Integer>();
   public static void serverr(int port) throws IOException {
      serverSocket = new ServerSocket(port);
      serverSocket.setSoTimeout(30000);
   }


   public static void main(String [] args) throws IOException {
      int port = Integer.parseInt(args[0]);
      try {
         serverr(port);
         System.out.println("Waiting for client on port " + 
            serverSocket.getLocalPort() + "...");
         
      }catch(IOException e) {
         e.printStackTrace();
      }
      while(true){
         try{
            server = serverSocket.accept();
            new serverThread(server).start();
         }
         catch(SocketTimeoutException e){;}
      }
   }
}

class serverThread extends Thread {
   private Socket server;
   private InputStream dataIs;
   private DataInputStream in;
   private DataOutputStream out;
   private OutputStream dataOs;
   public serverThread(Socket sserver) throws IOException{
      this.server = sserver;
   }

   public void run() {
      while(true) {
         try {
            HashMap<String, String> hm = new HashMap<String,String>();
            hm.put("admin","secret");
            hm.put("guest","pass");
            File dir;
            String command,response;
            Boolean fly=false;
            BufferedReader sc = new BufferedReader(new InputStreamReader(System.in));
            
            System.out.println("Just connected to " + server.getRemoteSocketAddress());
            in = new DataInputStream(server.getInputStream());
            
            System.out.println(in.readUTF());
            out = new DataOutputStream(server.getOutputStream());
            dataOs = server.getOutputStream();
            // out.writeUTF(">>> enter username & password: ");
            while(true){               
               String[] cred = in.readUTF().split(" ");
                String man=cred[0].toLowerCase();
               switch (man) {
                  case "user" : 
                     if (hm.containsKey(cred[1])) {
                        out.writeUTF("");
                        String[] cred1 = in.readUTF().split(" ");
                        if (cred1[0].equalsIgnoreCase("pass")) {
                           System.out.println(cred[1]+": "+cred1[1]);
                           if (hm.get(cred[1]).equals(cred1[1])) {
                              out.writeUTF("1");
                              fly=true;
                           }
                           else {
                              out.writeUTF("0");
                           }
                        }
                        else {
                           out.writeUTF("0");
                        }
                     }
                     else{
                        out.writeUTF("0");
                     }
                     break;
                  case "pass" :
                     out.writeUTF("0");
                     break;

               }
               if (fly) {
                  break;
               }
            }

            String workingDir=".";
            while(true){
               fly = false;
               File theDir;
               String[] cmd = in.readUTF().split(" ");
               switch (cmd[0].toLowerCase()) {
                  case "port" :
                     out.writeUTF("portc");
                     break;
                  case "retr" :
                     out.writeUTF("sending");
                     File sfile = new File(workingDir+"/"+cmd[1]);
                     byte[] buff = new byte[4096];
                     try{
                        InputStream fileStream = new FileInputStream(sfile);
                        out.writeUTF(sfile.length()+"");
                        int recv;            
                        while ((recv = fileStream.read(buff, 0, buff.length)) > 0) {
                           dataOs.write(buff,0,recv);
                        }
                     }
                     catch(NullPointerException n){
                        out.writeUTF("-1");
                     }
                     break;
                  case "stor" :
                     out.writeUTF("sendit");
                     DataInputStream dis = new DataInputStream(server.getInputStream());
                     FileOutputStream fos = new FileOutputStream(workingDir+cmd[1]);
                     byte[] buffer = new byte[4096];
                     dataIs = server.getInputStream();

                     File outFile = new File(workingDir+"/"+cmd[1]+".temp");       
                     OutputStream fileStream = new FileOutputStream(outFile);
                     
                     //read and write the file data
                     long len = 0L;
                     response = in.readUTF();
                     if (response.equals("-1")) {
                        break;
                     }
                     long size = Long.valueOf(response);
                     int recv = 0;
                     if (size > 0) {
                        while(len + recv < size) {
                           len += recv;
                           recv = dataIs.read(buffer, 0, buffer.length);
                           fileStream.write(buffer,0,recv);
                        }
                     }
                     File newfile =new File(workingDir+"/"+cmd[1]);
                     if(outFile.renameTo(newfile)){
                        System.out.println("File transfer succesful");
                     }else{
                        System.out.println("File transfer failed");
                     }

                     fileStream.close();
                     break;
                  case "noop" :
                     out.writeUTF("OK");
                     break;
                           
                  case "cwd" :
                     try{
                        theDir = new File(workingDir+"/"+cmd[1]);
                     }catch(ArrayIndexOutOfBoundsException a){
                        out.writeUTF("No name provided");
                        break;
                     }
                     if (cmd[1].equals(".")) {
                        out.writeUTF("Staying in same directory");
                        break;
                     }
                     if (cmd[1].equals("..")) {
                        try{
                           int i = workingDir.lastIndexOf('/');
                           workingDir = workingDir.substring(0,i);

                        }
                        catch (StringIndexOutOfBoundsException e){
                           out.writeUTF("In the root of server !");
                           break;
                        }
                        workingDir = workingDir;
                        out.writeUTF("Changed to "+workingDir);
                        break;
                        // workingDir = workingDir.substring(0,(workingDir.length() - workingDir.split('/')[(workingDir.split('/').length)-1].toString().length())) 
                     }
                     if (theDir.exists()) {
                        if (theDir.isFile()) {
                           out.writeUTF("Not a Directory");
                        }
                        else{
                           workingDir = workingDir +"/"+ cmd[1];
                           out.writeUTF("Changed to "+cmd[1]);
                        }
                        break;
                     }
                     else{
                           out.writeUTF("Directory not found !");
                           break;
                     }
                           
                  case "mkd" :
                     try{
                        theDir = new File(workingDir+"/"+cmd[1]);
                     }catch(ArrayIndexOutOfBoundsException a){
                        out.writeUTF("No name provided");
                        break;
                     }
                     
                     if (!theDir.exists()) {
                         try{
                             theDir.mkdir();
                             out.writeUTF("Directory Created !");
                         } 
                         catch(SecurityException se){
                             //handle it
                         }        
                     }
                     else {
                        out.writeUTF("Directory already exists !");
                     }
                     break;
                  case "rmd" :
                     try{
                        theDir = new File(workingDir+"/"+cmd[1]);
                     }catch(ArrayIndexOutOfBoundsException a){
                        out.writeUTF("No name provided");
                        break;
                     }
                     if (theDir.exists()) {
                         try{
                           if (theDir.isFile()) {
                              out.writeUTF("Not a directory !");
                           }
                           else{
                             theDir.delete();
                             out.writeUTF("Directory Deleted !");
                           }
                         } 
                         catch(SecurityException se){
                             //handle it
                         }        
                     }
                     else {
                        out.writeUTF("Directory doesnot exist !");
                     }
                     break;
                  case "dele" :
                     try{
                        theDir = new File(workingDir+"/"+cmd[1]);
                     }catch(ArrayIndexOutOfBoundsException a){
                        out.writeUTF("No name provided");
                        break;
                     }
                     if (theDir.exists()) {
                         try{
                           if (theDir.isFile()) {
                             theDir.delete();
                             out.writeUTF("File Deleted !");
                           }
                           else{
                              out.writeUTF("Not a file !");
                           }
                         } 
                         catch(SecurityException se){
                         }        
                     }
                     else {
                        out.writeUTF("File doesnot exist !");
                     }
                     break;
                  case "" :
                     out.writeUTF("inc");
                     break;
                  case "quit" :
                     out.writeUTF("");
                     fly=true;    
                     break;  
                  case "pwd":
                     dir = new File(workingDir);
                     out.writeUTF("\nAbsolute path: " + dir.getAbsolutePath());
                     break;
                  case "list":
                     dir=null;
                     try{
                        dir = new File(workingDir+"/"+cmd[1]);
                        StringBuilder sb = new StringBuilder();
                        sb.append("\nName: " + dir.getName());
                        sb.append("\nAbsolute path: " + dir.getAbsolutePath());
                        sb.append("\nSize: " + dir.length());
                        sb.append("\nLast modified: " + dir.lastModified());
                        out.writeUTF(sb.toString());
                        break;
                     }
                     catch(NullPointerException n){
                        out.writeUTF("File not found!");
                        break;
                     }
                     catch(ArrayIndexOutOfBoundsException a){
                        dir = new File(workingDir+"/");
                        StringBuilder sb = new StringBuilder();
                        File[] filesList = dir.listFiles();
                        try{
                           if (filesList.length == 0) {
                              out.writeUTF("No files or folder to show !");
                              break;
                           }
                        }
                        catch(NullPointerException n){
                           out.writeUTF("No files or folder to show !");
                           break;
                        }
                        for (File file : filesList) {
                            if (file.isFile()) {
                                sb.append("File: \t"+file.getName()+"\n");
                            }
                            else{
                                sb.append("Folder:\t"+file.getName()+"\n");
                            }
                        }
                        out.writeUTF(sb.toString());
                     }
                     break;
                  default :
                     out.writeUTF("incr");
               }    
               if (fly) {
                  break; 
               }           
            }

            server.close();
            break;
         }catch(SocketTimeoutException s) {
            System.out.println("Socket timed out!");
            break;
         }catch(IOException e) {
            e.printStackTrace();
            break;
         }
      }

   }

}