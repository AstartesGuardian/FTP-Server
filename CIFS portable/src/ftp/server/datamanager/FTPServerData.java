
package ftp.server.datamanager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Date;

public class FTPServerData extends FTPServerDataManager
{
    @Override
    public long getFileSize(File file)
    {
        return file.length();
    }
    
    @Override
    public boolean delete(File file)
    {
        if(file.exists() && file.isFile())
            return file.delete();
        else
            return true;
    }
    
    @Override
    protected FileInfo getFileInfo(File file)
    {
        return new FileInfo(file.isDirectory(),
                new Date(file.lastModified()),
                file.length(),
                file.getName()
        );
    }
    
    @Override
    protected String[] getDirectoryFiles(File directory)
    {
        return directory.list();
    }
    
    @Override
    public boolean fileExists(File file)
    {
        return file.exists();
    }
    
    @Override
    public boolean isFile(File file)
    {
        return file.isFile();
    }
    
    @Override
    protected void sendFile(File file) throws Exception
    {
        try (FileInputStream fstream = new FileInputStream(file))
        {
            byte[] buffer = new byte[1024];
            int nread;
            while ((nread = fstream.read(buffer)) > 0)
                sendData(buffer, nread);
        }
    }

    @Override
    protected void saveFile(File destination, InputStream in) throws Exception
    {
        try (FileOutputStream fstream = new FileOutputStream(destination))
        {
            byte[] buffer = new byte[1024];
            int nread;
            while ((nread = in.read(buffer)) > 0)
                fstream.write(buffer, 0, nread);
        }
    }
}
