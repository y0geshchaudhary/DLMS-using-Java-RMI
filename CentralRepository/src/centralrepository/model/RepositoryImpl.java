package centralrepository.model;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedHashMap;

import centralRepo.interfaces.Repository;
import centralRepo.interfaces.ServerDetail;

public class RepositoryImpl extends UnicastRemoteObject implements Repository {

	private static final long serialVersionUID = 1L;
	private HashMap<String, ServerDetail> libServers;
	
	public RepositoryImpl() throws RemoteException {
		super();
		this.libServers = new LinkedHashMap<String, ServerDetail>();
	}

	@Override
	public boolean registerLibraryServer(String library, String hostname, int port, String stubName) throws RemoteException {
		ServerDetail server = new ServerDetailImpl(hostname, port, stubName);
		libServers.put(library, server);
		return true;
	}

	@Override
	public ServerDetail getServerDetails(String libId) throws RemoteException {
		return libServers.get(libId);
	}

}
