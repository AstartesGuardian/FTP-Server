
package ftp.server;

public class AuthentificatorAcceptAll extends Authentificator
{
    @Override
    protected boolean connect(String user, String password)
    {
        return true;
    }
}
