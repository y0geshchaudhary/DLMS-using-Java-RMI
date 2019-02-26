package centralRepo.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerDetail extends Remote {

	public String getHostname() throws RemoteException;
	public int getPortNumber() throws RemoteException;
	public String getStubName() throws RemoteException;
	/*public void setHostname(String hostname) throws RemoteException;
	public void setPortNumber(int portNumber) throws RemoteException;*/
	
}
