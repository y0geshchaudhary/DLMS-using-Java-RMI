package server.model;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.ConnectIOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import centralRepo.interfaces.Repository;
import centralRepo.interfaces.ServerDetail;
import server.database.Database;
import server.interfaces.Book;
import server.interfaces.LibraryOperations;
import server.interfaces.OperationsEnum;

public class LibraryOperationsImpl extends UnicastRemoteObject implements LibraryOperations {
	private static final Logger log = LogManager.getLogger(LibraryOperationsImpl.class);
	
	private static final long serialVersionUID = 1L;
	private Database database;
	private Repository centralRepository;
	private String serverId;
	String[] otherServerIds = { "CON", "MCG" };

	public LibraryOperationsImpl(String serverId) throws RemoteException {
		super();
		database = Database.getDatabase();
		this.serverId = serverId;
	}

	public void setCentralRepository(Repository centralRepository) {
		this.centralRepository = centralRepository;
	}

	@Override
	public boolean userExists(String userId) throws RemoteException {
		log.debug("Inside userExists(String userId) method.");
		log.debug("call parameters: userId-"+userId);
		boolean result = database.userExists(userId);
		log.debug("method call result: "+result);
		return result;
	}

	/*
	 * Manager roles
	 */
	@Override
	public boolean addItem(String managerID, String itemID, String itemName, int quantity) throws RemoteException {
		log.debug("Inside addItem(String managerID, String itemID, String itemName, int quantity) method.");
		log.debug("call parameters: managerID-"+managerID+" ,itemID-"+itemID+" ,itemName-"+itemName+" ,quantity-"+quantity);
		boolean result;
		if (operationIsAllowed(managerID.toUpperCase(), true)) {
			result = database.addBookToLibrary(itemID, new BookImpl(itemID, itemName, quantity));
			log.debug("method call result: "+result);
			return result;
		} else {
			AccessException e = new AccessException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing AccessException.",e);
			throw e;
		}
	}

	@Override
	public int removeItem(String managerID, String itemID, int quantity) throws RemoteException {
		log.debug("Inside removeItem(String managerID, String itemID, int quantity) method.");
		log.debug("call parameters: managerID-"+managerID+" ,itemID-"+itemID+" ,quantity-"+quantity);
		int result;
		if (operationIsAllowed(managerID.toUpperCase(), true)) {
			result = database.removeBooksFromLibrary(itemID, quantity);
			log.debug("method call result: "+result);
			return result;
		} else {
			AccessException e = new AccessException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing AccessException.",e);
			throw e;
		}
	}

	@Override
	public List<Book> listAvailableItems(String managerID) throws RemoteException {
		log.debug("Inside listAvailableItems(String managerID) method.");
		log.debug("call parameters: managerID-"+managerID);
		List<Book> bookList;
		if (operationIsAllowed(managerID.toUpperCase(), true)) {
			bookList = database.getAllBooks();
			log.debug("method returning "+bookList.size()+" books.");
			return bookList;
		} else {
			AccessException e = new AccessException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing AccessException.",e);
			throw e;
		}
	}

	/*
	 * User roles
	 */
	@Override
	public int borrowItem(String userID, String itemID, String numberOfDays) throws RemoteException {
		log.debug("Inside borrowItem(String userID, String itemID, String numberOfDays) method.");
		log.debug("call parameters: userID-"+userID+" ,itemID-"+itemID+" ,numberOfDays-"+numberOfDays);
		userID = userID.toUpperCase();
		itemID = itemID.toUpperCase();
		if (operationIsAllowed(userID, false)) {
			if (itemID.startsWith(serverId)) {
				int result = database.borrowBook(userID, itemID, 1);
				log.debug("request belong to this library. method call returns: "+result);
				return result;
			} else {
				log.debug("request can't be served by this library. Making call to related library over UDP.");
				String data = OperationsEnum.BORROW_ITEM.name().concat("#").concat(userID).concat("#").concat(itemID);
				log.debug("Data to send over UDP socket: "+data);
				byte[] dataBytes = data.getBytes();
				String server = itemID.substring(0, 3);
				ServerDetail udpServerDetails = centralRepository.getServerDetails(server + "UDP");
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getByName(udpServerDetails.getHostname()), udpServerDetails.getPortNumber());
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					// if(data.length()!=0) {
					log.debug("response from remote library: "+data);
					return Integer.parseInt(data);
					// }
				} catch (SocketException e) {
					log.error("Unable to open socket connection.");
					e.printStackTrace();
					throw new ConnectIOException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails");
					e.printStackTrace();
					throw new ConnectIOException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.");
					e.printStackTrace();
					throw new ConnectIOException("Issue with sending or receiving data packet.");
				}
			}

		} else {
			AccessException e = new AccessException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing AccessException.",e);
			throw e;
		}
	}

	@Override
	public List<Book> findItem(String userID, String itemName) throws RemoteException {
		log.debug("Inside findItem(String userID, String itemName) method.");
		log.debug("call parameters: userID-"+userID+" ,itemName-"+itemName);
		userID = userID.toUpperCase();
		itemName = itemName.toUpperCase();
		List<Book> bookList = new ArrayList<>();
		if (operationIsAllowed(userID.toUpperCase(), false)) {
			// query local DB
			List<BookImpl> localDBBooksList =database.findItem(itemName);
			log.debug("no. of related books in local library are "+localDBBooksList.size());
			bookList.addAll(localDBBooksList);

			// query other server DB
			String data = OperationsEnum.FIND_ITEM.name().concat("#").concat(itemName);
			byte[] dataBytes = data.getBytes();
			log.debug("request data to be send to other libraries is "+data);
			try (DatagramSocket socket = new DatagramSocket();) {
				// get details from Montreal university/
				ServerDetail udpServerDetails = centralRepository.getServerDetails(otherServerIds[0] + "UDP");
				DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
						InetAddress.getByName(udpServerDetails.getHostname()), udpServerDetails.getPortNumber());
				socket.send(packet);
				dataBytes = new byte[5000];
				packet = new DatagramPacket(dataBytes, dataBytes.length);
				socket.receive(packet);
				data = new String(packet.getData()).trim();
				String[] bookDetails = data.split("#");
				log.debug("no. of related books received from "+otherServerIds[0]+" are "+bookDetails.length/2);
				
				for (int i = 0; i < bookDetails.length; i+=2) {
					bookList.add(new BookImpl(bookDetails[i], itemName, Integer.parseInt(bookDetails[i+1])));	
				}

				data = OperationsEnum.FIND_ITEM.name().concat("#").concat(itemName);
				dataBytes = data.getBytes();
				// get details from MCGill university
				udpServerDetails = centralRepository.getServerDetails(otherServerIds[1] + "UDP");
				packet = new DatagramPacket(dataBytes, dataBytes.length,
						InetAddress.getByName(udpServerDetails.getHostname()), udpServerDetails.getPortNumber());
				socket.send(packet);
				dataBytes = new byte[5000];
				packet = new DatagramPacket(dataBytes, dataBytes.length);
				socket.receive(packet);
				data = new String(packet.getData()).trim();
				bookDetails = data.split("#");
				log.debug("no. of related books received from "+otherServerIds[1]+" are "+bookDetails.length/2);
				
				for (int i = 0; i < bookDetails.length; i+=2) {
					bookList.add(new BookImpl(bookDetails[i], itemName, Integer.parseInt(bookDetails[i+1])));	
				}
				
				log.debug("Total books to be returned are "+bookList.size());
				return bookList;
			} catch (SocketException e) {
				log.error("Unable to open socket connection.",e);
				e.printStackTrace();
				throw new ConnectIOException("Unable to open socket connection.");
			} catch (UnknownHostException e) {
				log.error("Unable to identify host given by udpServerDetails",e);
				e.printStackTrace();
				throw new ConnectIOException("Unable to identify host given by udpServerDetails.");
			} catch (IOException e) {
				log.error("Issue with sending or receiving data packet.",e);
				e.printStackTrace();
				throw new ConnectIOException("Issue with sending or receiving data packet.");
			}
		} else {
			AccessException e = new AccessException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing AccessException.",e);
			throw e;
		}
	}

	@Override
	public String returnItem(String userID, String itemID) throws RemoteException {
		log.debug("Inside returnItem(String userID, String itemID) method.");
		log.debug("call parameters: userID-"+userID+" ,itemID-"+itemID);
		userID = userID.toUpperCase();
		itemID = itemID.toUpperCase();
		if (operationIsAllowed(userID.toUpperCase(), false)) {
			boolean result;
			if (itemID.startsWith(serverId)) {
				result = database.returnBook(userID, itemID);
				log.debug("request belong to this library. method call returns: "+result);
			} else {
				log.debug("request can't be served by this library. Making call to related library over UDP.");
				String data = OperationsEnum.RETURN_ITEM.name().concat("#").concat(userID).concat("#").concat(itemID);
				log.debug("Data to send over UDP socket: "+data);
				byte[] dataBytes = data.getBytes();
				String server = itemID.substring(0, 3);
				ServerDetail udpServerDetails = centralRepository.getServerDetails(server + "UDP");
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getByName(udpServerDetails.getHostname()), udpServerDetails.getPortNumber());
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					result = data.equals("TRUE") ? true : false;
					log.debug("response from remote library: "+data);
				} catch (SocketException e) {
					log.error("Unable to open socket connection.",e);
					e.printStackTrace();
					throw new ConnectIOException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails",e);
					e.printStackTrace();
					throw new ConnectIOException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.",e);
					e.printStackTrace();
					throw new ConnectIOException("Issue with sending or receiving data packet.");
				}
			}
			return result ? "Book returned to library successfully." : "Unable to return book to library";

		} else {
			AccessException e = new AccessException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing AccessException.",e);
			throw e;
		}
	}

	private boolean operationIsAllowed(String userId, boolean managerOperation) {
		log.debug("Inside operationIsAllowed(String userId, boolean managerOperation) method.");
		log.debug("call parameters: userId-"+userId+" ,managerOperation-"+managerOperation);
		boolean result;
		if (managerOperation)
			result = userId.charAt(3) == 'M' ? true : false;
		else
			result = userId.charAt(3) == 'U' ? true : false;
		
		log.debug("method call returns: "+result);
		return result;
	}

	@Override
	public boolean addToWaitingList(String userID, String itemID) throws RemoteException {
		log.debug("Inside addToWaitingList(String userID, String itemID) method.");
		log.debug("call parameters: userID-"+userID+" ,itemID-"+itemID);
		userID = userID.trim();
		itemID = itemID.trim();
		if (operationIsAllowed(userID, false)) {
			boolean result;
			if (itemID.startsWith(serverId)) {
				result = database.addUserToWaitingList(userID, itemID);
				log.debug("request belong to this library. method call returns: "+result);
			} else {
				log.debug("request can't be served by this library. Making call to related library over UDP.");
				String data = OperationsEnum.ADD_TO_WAITING_LIST.name().concat("#").concat(userID).concat("#")
						.concat(itemID);
				log.debug("Data to send over UDP socket: "+data);
				byte[] dataBytes = data.getBytes();
				String server = itemID.substring(0, 3);
				ServerDetail udpServerDetails = centralRepository.getServerDetails(server + "UDP");
				try (DatagramSocket socket = new DatagramSocket();) {
					DatagramPacket packet = new DatagramPacket(dataBytes, dataBytes.length,
							InetAddress.getByName(udpServerDetails.getHostname()), udpServerDetails.getPortNumber());
					socket.send(packet);
					dataBytes = new byte[5000];
					packet = new DatagramPacket(dataBytes, dataBytes.length);
					socket.receive(packet);
					data = new String(packet.getData()).trim();
					result = data.equals("TRUE") ? true : false;
					log.debug("response from remote library: "+data);
				} catch (SocketException e) {
					log.error("Unable to open socket connection.",e);
					e.printStackTrace();
					throw new ConnectIOException("Unable to open socket connection.");
				} catch (UnknownHostException e) {
					log.error("Unable to identify host given by udpServerDetails",e);
					e.printStackTrace();
					throw new ConnectIOException("Unable to identify host given by udpServerDetails.");
				} catch (IOException e) {
					log.error("Issue with sending or receiving data packet.",e);
					e.printStackTrace();
					throw new ConnectIOException("Issue with sending or receiving data packet.");
				}
			}
			return result;
		} else {
			AccessException e = new AccessException("Operation is not allowed for this USER.");
			log.error("Operation is not allowed for this USER. Throwing AccessException.",e);
			throw e;
		}

	}

}
