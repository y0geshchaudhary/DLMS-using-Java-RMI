package server.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Book extends Remote {

	public String getName() throws RemoteException;
	public String getId() throws RemoteException;
	public int getNumberOfCopies() throws RemoteException;
	
	public void setName(String name) throws RemoteException;
	public void setId(String id) throws RemoteException;
	public void setNumberOfCopies(int numberOfCopies) throws RemoteException;
}
