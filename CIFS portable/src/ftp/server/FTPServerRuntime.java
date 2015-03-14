
package ftp.server;

import ftp.server.datamanager.FTPServerDataManager;
import ftp.server.authentificator.Authentificator;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FTPServerRuntime implements Runnable
{
    public FTPServerRuntime(Socket clientSocket, FTPServer server, Authentificator authentificator, FTPServerDataManager serverDataManager) throws IOException
    {
        this.authentificator = authentificator;
        this.clientSocket = clientSocket;
        this.server = server;
        this.serverDataManager = serverDataManager;
        System.out.println("[USER] : " + clientSocket.getInetAddress() + " : " + clientSocket.getPort());
        
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()), true);
    }
    
    private final FTPServerDataManager serverDataManager;
    private final Socket clientSocket;
    private final FTPServer server;
    private final Authentificator authentificator;
    
    private PrintWriter out;
    private BufferedReader in;
    
    public void send(int code, String text)
    {
        out.println(code + " " + text);
        System.out.println("[SENT] : " + code + " : " + text);
        System.out.flush();
    }
    public void error(int code, String text)
    {
        out.println(code + " " + text);
        System.out.println("[ERROR] : " + code + " : " + text);
        System.out.flush();
    }
    protected String read()
    {
        try
        {
            String str = in.readLine();
            System.out.println("[READ] : " + str);
            return str;
        }
        catch (IOException ex)
        {
            System.out.println("[ERROR-READ] Nothing read.");
            return "";
        }
    }
    
    protected String getLocalPath(String directory, String file)
    {
        String dir = getLocalPath(directory);
        if(dir.endsWith("/"))
            dir = dir + file;
        else
            dir = dir + "/" + file;
        return dir;
    }
    protected String getLocalPath(String path)
    {
        if(path.charAt(0) == '/')
            path = server.getWorkingDirectory() + path;
        else
            path = server.getWorkingDirectory() + "/" + path;
        return path;
    }
    
    private void close()
    {
        try
        {
            if(!clientSocket.isClosed())
                clientSocket.close();
        }
        catch (Exception ex)
        { }
    }
    
    protected String getFullName(StringTokenizer token)
    {
        String fileName = "";
        while(token.hasMoreTokens())
            fileName += token.nextToken() + " ";
        return fileName.trim();
    }

    @Override
    public void run()
    {
        send(220, "localhost FTP server (" + 1.0 + ") ready.");
        
        FTPEnvironnement environnement = new FTPEnvironnement();
        environnement.serverData = this.serverDataManager;
        environnement.serverData.setServerRuntime(this);
        
        try
        {
            String line;
            while((line = read()) != null)
            {
                StringTokenizer token = new StringTokenizer(line);

                String command = token.nextToken().toUpperCase();
                
                if(!authentificator.isConnected())
                {
                    switch(command)
                    {
                        case "USER":
                            if(token.hasMoreTokens())
                                authentificator.setUser(token.nextToken());
                            send(331, "Password required for " + authentificator.getUser() + ".");
                            break;

                        case "PASS":
                            if(authentificator.getUser() == null)
                            {
                                error(503, "Login with USER first.");
                                break;
                            }

                            if(token.hasMoreTokens())
                            {
                                if(authentificator.connect(token.nextToken()))
                                    send(230, "User " + authentificator.getUser() + " logged in.");
                                else
                                    send(430, "Invalid username or password.");
                            }
                            else
                                error(500, "Not enough parameters");
                            break;

                        default:
                            error(500, "Command " + command + " not recognized.");
                            break;
                    }
                }
                else
                {
                    switch(command)
                    {
                        case "PWD":
                            send(257, "\"" + environnement.currentDir + "\"");
                            break;

                        case "TYPE":
                            send(200, "Type set to ...");
                            break;

                        case "CWD":
                        {
                            String newCurrentDir = token.nextToken();
                            environnement.currentDir = newCurrentDir;
                            send(200, "CWD command successful.");
                            break;
                        }

                        case "STOR":
                        {
                            String fileName = getFullName(token);

                            environnement.serverData.receiveFile(getLocalPath(environnement.currentDir, fileName));
                            break;
                        }

                        case "DELE":
                        {
                            String fileName = getFullName(token);

                            File file = new File(getLocalPath(environnement.currentDir, fileName));
                            
                            if(!environnement.serverData.delete(file))
                                error(550, fileName + " : could not delete file.");
                            else
                                send(250, "File " + fileName + " deleted.");

                            break;
                        }

                        case "SIZE":
                        {
                            String fileName = getFullName(token);

                            File file = new File(getLocalPath(environnement.currentDir, fileName));
                            if(!environnement.serverData.fileExists(file))
                                error(550, fileName + " : no such file.");
                            else if(!environnement.serverData.isFile(file))
                                error(550, fileName + " : not a plain file.");
                            else
                                send(200, Long.toString(environnement.serverData.getFileSize(file)));
                            break;
                        }

                        case "NOOP":
                            send(200, "NOOP command successful.");
                            break;

                        case "RETR":
                        {
                            String fileName = getFullName(token);

                            environnement.serverData.sendFile(getLocalPath(environnement.currentDir, fileName));
                            break;
                        }

                        case "LIST":
                        {
                            String path = null;
                            if (token.hasMoreTokens())
                                path = token.nextToken();
                            else
                                path = environnement.currentDir;

                            environnement.serverData.list(getLocalPath(path));
                            break;
                        }

                        case "LPRT":
                        {
                            StringTokenizer lprt_token = new StringTokenizer(token.nextToken(), ",");

                            String af = lprt_token.nextToken();
                            byte hal = Byte.parseByte(lprt_token.nextToken());
                            byte[] hx = new byte[hal];
                            for(int i = 0; i < hal; i++)
                                hx[i] = Byte.parseByte(lprt_token.nextToken());
                            byte pal = Byte.parseByte(lprt_token.nextToken());

                            int dataPort = 0;
                            for(int i = pal - 1; i >= 0; i--)
                                dataPort += Integer.parseInt(lprt_token.nextToken()) * Math.pow(2, i * 8);

                            InetAddress dataIP = InetAddress.getByAddress(hx);
                            environnement.serverData.setAddress(dataIP, dataPort);

                            send(200, "[IP] = " + dataIP + " [PORT] = " + dataPort);
                            break;
                        }

                        case "EPSV":
                            send(500, "OK.");
                            break;
                        case "LPSV":
                            send(500, "OK.");
                            break;

                        case "EPRT":
                        {
                            StringTokenizer eprt_token = new StringTokenizer(token.nextToken(), "|");

                            String type = eprt_token.nextToken();
                            String ip = eprt_token.nextToken();
                            int dataPort = Integer.parseInt(eprt_token.nextToken());

                            InetAddress dataIP = InetAddress.getByName(ip);
                            environnement.serverData.setAddress(dataIP, dataPort);

                            send(200, "[IP] = " + dataIP + " [PORT] = " + dataPort);
                            break;
                        }

                        case "PORT":
                        {
                            StringTokenizer port_token = new StringTokenizer(token.nextToken(), ",");

                            String ip = port_token.nextToken();
                            ip += "." + port_token.nextToken();
                            ip += "." + port_token.nextToken();
                            ip += "." + port_token.nextToken();

                            int dataPort = 0;
                            for(int i = 2 - 1; i >= 0; i--)
                                dataPort += Integer.parseInt(port_token.nextToken()) * Math.pow(2, i * 8);

                            InetAddress dataIP = InetAddress.getByName(ip);
                            environnement.serverData.setAddress(dataIP, dataPort);

                            send(200, "[IP] = " + dataIP + " [PORT] = " + dataPort);
                            break;
                        }

                        default:
                            error(500, "Command " + command + " not recognized.");
                            break;
                    }
                }
            }
        } catch(Exception ex)
        {
            System.err.println(ex.getMessage());
        }
    }
}
