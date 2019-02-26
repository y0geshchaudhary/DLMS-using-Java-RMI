package server.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface LibraryOperations extends Remote {
	
	public boolean userExists(String userId) throws RemoteException;
	/*
	 * Manager operations 
	 */
	public boolean addItem(String managerID, String itemID, String itemName,int quantity) throws RemoteException;
	public int removeItem (String managerID, String itemID,int quantity) throws RemoteException;
	public List<Book> listAvailableItems (String managerID) throws RemoteException;
	
	/*
	 * Student operations
	 */
	public int borrowItem(String userID, String itemID, String numberOfDays) throws RemoteException;
	public List<Book> findItem (String userID, String itemName) throws RemoteException;
	public String returnItem (String userID, String itemID) throws RemoteException;
	public boolean addToWaitingList(String userID, String itemID) throws RemoteException;
}
