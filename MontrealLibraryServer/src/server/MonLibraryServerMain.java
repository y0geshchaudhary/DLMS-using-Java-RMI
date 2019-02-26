package server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import centralRepo.interfaces.Repository;
import server.model.LibraryOperationsImpl;
import server.model.UDPListener;

public class MonLibraryServerMain {
	private static final Logger log = LogManager.getLogger(MonLibraryServerMain.class);
	
	public static void main(String[] args) {
		log.debug("Inside main() method.");
		// RMI and UDP details.
		String library = "MON";
		String libraryUDP = "MONUDP";
		int port = 2005;
		int portUDP = 2006;
		String stubName = "MONTREALLibrary";
		Registry libRegistry;
		LibraryOperationsImpl libraryOperationsImpl = null;

		// binding remote objects to registry.
		try {
			libRegistry = LocateRegistry.createRegistry(port);
			libraryOperationsImpl = new LibraryOperationsImpl(library);
			libRegistry.rebind(stubName, libraryOperationsImpl);
			log.debug("Binded LibraryOperations stub to registry at port " + port);
		} catch (AccessException e) {
			log.error("Issue binding to registry.",e);
			e.printStackTrace();
		} catch (RemoteException e) {
			log.error("Issue either creating libRegistry or from creating libraryOperations instance.", e);
			e.printStackTrace();
		}

		// setting up UDP listener thread.
		Thread udpThread = new Thread(new UDPListener(portUDP));
		udpThread.start();
		log.debug("Starting UDP thread.");
		// fetching central repository details.
		String hostname = null;
		Registry centralRegistry;
		Repository repository = null;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
			centralRegistry = LocateRegistry.getRegistry(Repository.CENTRAL_REPOSITORY_HOSTNAME, Repository.CENTRAL_REPOSITORY_PORT);
			repository = (Repository) centralRegistry.lookup("Repository");
			libraryOperationsImpl.setCentralRepository(repository);
			log.debug("Fetching centralRepository stub.");
		} catch (UnknownHostException e) {
			log.error("Unable to identify localhost.", e);
			e.printStackTrace();
		} catch (RemoteException e) {
			log.error("Unable to fetch centralRegistry.", e);
			e.printStackTrace();
		} catch (NotBoundException e) {
			log.error("Unable to do lookup for Repository stub.", e);
			e.printStackTrace();
		}

		// registering Concordia server details and UDP socket details to central
		// repository.
		try {
			repository.registerLibraryServer(libraryUDP, hostname, portUDP, null);
			repository.registerLibraryServer(library, hostname, port, stubName);
			log.debug("Saved server details and UDP connection details with central repository.");
		} catch (RemoteException e) {
			log.error("Issue with central repository.", e);
			e.printStackTrace();
		}
		log.debug("Concordia server is up.");

	}

}
