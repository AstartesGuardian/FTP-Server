
package ftp.server.authentificator;

public abstract class Authentificator
{
    public Authentificator()
    {
        this.connected = false;
        this.user = null;
    }
    
    private String user;
    private boolean connected;
    
    public void setUser(String user)
    {
        this.user = user;
    }
    public String getUser()
    {
        return user;
    }
    
    public boolean isConnected()
    {
        return connected;
    }
    
    public boolean connect(String password)
    {
        if(user == null)
            return false;
        
        connected = connect(user, password);
        return connected;
    }
    
    protected abstract boolean connect(String user, String password);
}
