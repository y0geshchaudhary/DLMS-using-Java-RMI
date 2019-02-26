package server.model;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import server.interfaces.Book;

public class BookImpl extends UnicastRemoteObject implements Book {

	private static final long serialVersionUID = 1L;
	private String name;
	private String id;
	private int numberOfCopies;
	
	public BookImpl(String id, String name, int numberOfCopies) throws RemoteException {
		super();
		this.name = name;
		this.id = id;
		this.numberOfCopies = numberOfCopies;
	}

	@Override
	public String getName() throws RemoteException {
		return name;
	}

	@Override
	public void setName(String name) throws RemoteException {
		this.name = name;
	}

	@Override
	public String getId() throws RemoteException {
		return id;
	}

	@Override
	public void setId(String id) throws RemoteException {
		this.id = id;
	}

	@Override
	public int getNumberOfCopies() throws RemoteException {
		return numberOfCopies;
	}

	@Override
	public void setNumberOfCopies(int numberOfCopies) throws RemoteException {
		this.numberOfCopies = numberOfCopies;
	}

}
