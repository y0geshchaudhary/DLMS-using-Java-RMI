package centralRepo.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Repository extends Remote {
	
	static final String CENTRAL_REPOSITORY_HOSTNAME = "";
	static final int CENTRAL_REPOSITORY_PORT = 2000;
	public boolean registerLibraryServer(String library, String hostname, int port, String stubName) throws RemoteException;
	public ServerDetail getServerDetails(String libId) throws RemoteException;

}
