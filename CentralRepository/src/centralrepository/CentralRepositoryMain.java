package centralrepository;

import java.rmi.AccessException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import centralRepo.interfaces.Repository;
import centralrepository.model.RepositoryImpl;

public class CentralRepositoryMain {

	private static final Logger log = LogManager.getLogger(CentralRepositoryMain.class);

	public static void main(String[] args) {
		log.debug("Inside main() method.");
		try {
			Registry registry = LocateRegistry.createRegistry(2000);
			Repository repository = new RepositoryImpl();
			registry.rebind("Repository", repository);
			log.debug("Created repository and binded at port 2000.");
		} catch (AccessException e) {
			log.error("Issue binding to registry." + "\n" + e.getMessage(),e);
			e.printStackTrace();
		} catch (RemoteException e) {
			log.error("Issue either creating registry or from creating repository instance." + "\n" + e.getMessage(),e);
			e.printStackTrace();
		}
		log.debug("Central Repository is up.");
	}

}
