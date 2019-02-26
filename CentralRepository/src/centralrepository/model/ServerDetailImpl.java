package centralrepository.model;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

import centralRepo.interfaces.ServerDetail;

public class ServerDetailImpl extends UnicastRemoteObject implements ServerDetail {

	private static final long serialVersionUID = 1L;
	private String hostname;
	private int port;
	private String stubName;

	protected ServerDetailImpl(String hostname, int port, String stubName) throws RemoteException {
		super();
		this.hostname = hostname;
		this.port = port;
		this.stubName = stubName;
	}

	@Override
	public String getHostname() throws RemoteException {
		return this.hostname;
	}

	@Override
	public int getPortNumber() throws RemoteException {
		return this.port;
	}

	@Override
	public String getStubName() throws RemoteException {
		return this.stubName;
	}

	/*
	 * @Override public void setHostname(String hostname) throws RemoteException {
	 * // TODO Auto-generated method stub
	 * 
	 * }
	 * 
	 * @Override public void setPortNumber(int portNumber) throws RemoteException {
	 * // TODO Auto-generated method stub
	 * 
	 * }
	 */

}
