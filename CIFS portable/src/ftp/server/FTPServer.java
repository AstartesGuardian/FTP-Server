
package ftp.server;

import ftp.server.datamanager.FTPServerDataManager;
import ftp.server.datamanager.FTPServerData;
import ftp.server.authentificator.AuthentificatorAcceptAll;
import ftp.server.authentificator.Authentificator;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FTPServer implements Runnable
{
    public static void main(String[] args) throws IOException
    {
        FTPServer server = new FTPServer(
                // listen port
                8000,
                // local directory corresponding to ftp folder : "/"
                "D:\\Documents\\FTP_TEST",
                // authentificator
                AuthentificatorAcceptAll.class,
                // server data manager
                FTPServerData.class
        );
        new Thread(server).start();
        
        while(true);
    }
    
    public FTPServer(String workingDirectory, Class authentificator, Class serverDataManager)
    {
        if(workingDirectory.endsWith("/"))
            this.workingDirectory = workingDirectory.substring(0, workingDirectory.length() - 1);
        else
            this.workingDirectory = workingDirectory;
        
        this.port = null;
        this.authentificator = authentificator;
        this.serverDataManager = serverDataManager;
    }
    public FTPServer(int port, String workingDirectory, Class authentificator, Class serverDataManager)
    {
        if(workingDirectory.endsWith("/"))
            this.workingDirectory = workingDirectory.substring(0, workingDirectory.length() - 1);
        else
            this.workingDirectory = workingDirectory;
        
        this.port = port;
        this.authentificator = authentificator;
        this.serverDataManager = serverDataManager;
    }
    
    private final Integer port;
    private final Class authentificator;
    private final Class serverDataManager;
    
    private final String workingDirectory;
    public String getWorkingDirectory()
    {
        return workingDirectory;
    }
    
    private boolean exit;
    public void Exit()
    {
        exit = true;
    }

    @Override
    public void run()
    {
        exit = false;
        try
        {
            ServerSocket serverSocket;
            if(port == null)
                serverSocket = new ServerSocket();
            else
                serverSocket = new ServerSocket(port);
            System.out.println("Server '"
                    + serverSocket.getLocalSocketAddress()
                    + "' started on the port : " + port);
        
            while(!exit)
                new Thread(new FTPServerRuntime(
                        serverSocket.accept(),
                        this,
                        (Authentificator)authentificator.newInstance(),
                        (FTPServerDataManager)serverDataManager.newInstance()
                )).start();
        }
        catch (Exception ex)
        { }
    }
}
