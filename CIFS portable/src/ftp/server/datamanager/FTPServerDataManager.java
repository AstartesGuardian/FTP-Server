
package ftp.server.datamanager;

import ftp.server.FTPServerRuntime;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public abstract class FTPServerDataManager
{
    private FTPServerRuntime serverRuntime;
    private InetAddress dataIP;
    private int dataPort;
    
    private PrintWriter out;
    private OutputStream outByte;
    private InputStream in;
    
    public void setServerRuntime(FTPServerRuntime serverRuntime)
    {
        this.serverRuntime = serverRuntime;
    }
    public void setAddress(InetAddress ip, int port)
    {
        this.dataIP = ip;
        this.dataPort = port;
    }
    
    protected void sendCommand(int code, String text)
    {
        serverRuntime.send(code, text);
    }
    protected void error(int code, String text)
    {
        serverRuntime.error(code, text);
    }
    protected void sendData(byte buffer[], int len) throws IOException
    {
        outByte.write(buffer, 0, len);
    }
    protected void sendData(char data)
    {
        out.print(data);
    }
    protected void sendData(String data)
    {
        out.print(data);
    }
    protected void flushData()
    {
        out.flush();
    }
    protected void flushByteData() throws IOException
    {
        outByte.flush();
    }
    
    private Socket openSocket() throws IOException
    {
        Socket socket = new Socket(dataIP, dataPort);
        out = new PrintWriter(socket.getOutputStream());
        outByte = socket.getOutputStream();
        in = socket.getInputStream();
        return socket;
    }
    private void closeSocket(Socket socket)
    {
        try
        {
            if(socket != null)
                if(!socket.isClosed())
                    socket.close();
        }
        catch(Exception ex)
        {}
    }
    
    private static String createPad(int length)
    {
        StringBuilder pad = new StringBuilder();
        
        for (int i = 0; i < length; i++)
            pad.append(' ');
        
        return pad.toString();
    }
    
    
    protected class FileInfo
    {
        public FileInfo(boolean isDirectory, Date lastModified, long size, String name)
        {
            this.isDirectory = isDirectory;
            this.lastModified = lastModified;
            this.size = size;
            this.name = name;
        }
        
        final boolean isDirectory;
        final Date lastModified;
        final long size;
        final String name;
    }
    
    
    protected void sendFileInfo(File file)
    {
        FileInfo fi = getFileInfo(file);
        
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd hh:mm");
        String dateStr = dateFormat.format(fi.lastModified);

        String sizeStr = Long.toString(fi.size);
        int sizePadLength = Math.max(8 - sizeStr.length(), 0);
        String sizeField = createPad(sizePadLength) + sizeStr;
        
        sendData(fi.isDirectory ? 'd' : '-');
        sendData("rwxrwxrwx");
        sendData(" ");
        sendData("  1");
        sendData(" ");
        sendData("ftp     ");
        sendData(" ");
        sendData("ftp     ");
        sendData(" ");
        sendData(sizeField);
        sendData(" ");
        sendData(dateStr);
        sendData(" ");
        sendData(fi.name);

        sendData('\n');
    }
    
    
    public void receiveFile(String localPath)
    {
        Socket socket = null;
        
        try
        {
            File file = new File(localPath);
            
            if(fileExists(file))
            {
                error(550, "File exists in that location.");
                return;
            }

            socket = openSocket();

            sendCommand(150, "Opening the file.");
            
            saveFile(file, in);
            
            sendCommand(226, "Transfer completed.");
        }
        catch (IOException ex)
        {
            error(425, "Can't open data connection.");
        }
        catch(Exception ex)
        {
            error(425, "Can't upload file.");
        }
        
        closeSocket(socket);
    }
    
    
    
    public void sendFile(String localPath)
    {
        Socket socket = null;
        
        try
        {
            File file = new File(localPath);
            
            if(!fileExists(file))
            {
                error(550, "No such file.");
                return;
            }
            else if(!isFile(file))
            {
		sendCommand(550, "Not a plain file.");
                return;
            }

            socket = openSocket();

            sendCommand(150, "Opening the file.");
            
            sendFile(file);
            
            flushByteData();
            
            sendCommand(226, "Transfer completed.");
        }
        catch (IOException ex)
        {
            error(425, "Can't open data connection.");
        }
        catch(Exception ex)
        {
            error(550, "No such file.");
        }
        
        closeSocket(socket);
    }
    
    public void list(String localPath)
    {
        Socket socket = null;
        
        try
        {
            File directory = new File(localPath);
            String[] files = getDirectoryFiles(directory);
            int nbFile = (files == null ? 0 : files.length);

            socket = openSocket();

            sendCommand(150, "Opening the file.");

            sendData("total " + nbFile + "\n");
            
            if(nbFile > 0)
                for(String s : files)
                    sendFileInfo(new File(localPath, s));
            
            flushData();
            
            sendCommand(226, "Transfer completed.");
        }
        catch (IOException ex)
        {
            error(425, "Can't open data connection.");
        }
        catch(Exception ex)
        {
            error(550, "No such directory.");
        }
        
        closeSocket(socket);
    }
    
    protected abstract void sendFile(File file) throws Exception;
    protected abstract void saveFile(File destination, InputStream in) throws Exception;
    protected abstract String[] getDirectoryFiles(File directory);
    protected abstract FileInfo getFileInfo(File file);
    
    public abstract long getFileSize(File file);
    public abstract boolean fileExists(File file);
    public abstract boolean isFile(File file);
    public abstract boolean delete(File file);
}
