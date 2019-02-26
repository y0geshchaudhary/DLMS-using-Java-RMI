package server.database;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import server.interfaces.Book;
import server.model.BookImpl;

public class Database {
	private static final Logger log = LogManager.getLogger(Database.class);
	private static final Map<String, BookImpl> bookDB = Collections.synchronizedMap(new LinkedHashMap<>());
	private static final Set<String> users = Collections.synchronizedSet(new LinkedHashSet<>());
	private static final Map<String, List<String>> waitingList = Collections.synchronizedMap(new LinkedHashMap<>());
	private static final Map<String, List<String>> borrowedBooks = Collections.synchronizedMap(new LinkedHashMap<>());
	private static Database db;

	private Database() {
		createDatabaseEntries();
	}

	public static Database getDatabase() {
		if (db == null)
			db = new Database();

		return db;
	}

	private void createDatabaseEntries() {
		log.debug("Inside createDatabaseEntries() method.");
		synchronized (users) {
			Collections.addAll(users, "CONM1111", "CONM1112", "CONM1113", "CONM1114", "CONM1115", "CONU1111",
					"CONU1112", "CONU1113", "CONU1114", "CONU1115", "CONU1116", "CONU1117", "CONU1118", "CONU1119",
					"CONU1120");
		}
		log.debug("No. of users added to DB are "+users.size());
		List<String> bookIds = new LinkedList<>();
		List<String> bookNames = new LinkedList<>();
		int numberOfCopies = 5;
		Collections.addAll(bookIds, "CON6231", "CON6641", "CON6491", "CON6651", "CON6481", "CON6501", "CON6411",
				"CON6180", "CON6461", "CON6521");
		Collections.addAll(bookNames, "Distributed Systems", "Advanced Programming", "Systems Software",
				"Algorithm Design", "System Requirements Spec", "Programming Competency", "Comparative Studies",
				"Data Mining", "Software Design", "Advance Database");
		try {
			synchronized (bookDB) {
				BookImpl book;
				for (int i = 0; i < bookIds.size(); i++) {
					book = new BookImpl(bookIds.get(i), bookNames.get(i), numberOfCopies);
					bookDB.put(book.getId(), book);
				}
				log.debug("No. of books added to DB are "+bookDB.size());
			}
		} catch (RemoteException e) {
			log.error("Unable to create book instance.",e);
			e.printStackTrace();
		}

	}

	public boolean userExists(String userID) {
		log.debug("Inside userExists(String userID) method.");
		log.debug("call parameters: userID-"+userID);
		synchronized (users) {
			log.debug("return value: "+users.contains(userID));
			return users.contains(userID);
		}
	}

	public boolean addBookToLibrary(String itemID, BookImpl book) throws RemoteException {
		log.debug("Inside addBookToLibrary(String itemID, BookImpl book) method.");
		log.debug("call parameters: itemID-"+itemID+" , book-"+book);
		BookImpl b;
		synchronized (bookDB) {
			synchronized (waitingList) {
				synchronized (borrowedBooks) {
					try {
						if (bookDB.containsKey(itemID)) {
							b = bookDB.get(itemID);
							b.setNumberOfCopies(b.getNumberOfCopies() + book.getNumberOfCopies());
							log.debug("Book is already in DB, incremented it's quantity.");
						} else {
							bookDB.put(itemID, book);
							log.debug("Book is not in DB, adding book to DB.");
						}
						List<String> waitingUsers = waitingList.get(itemID);
						if (waitingUsers != null && waitingUsers.size() > 0) {
							log.debug("No. of user's waiting for this book are: "+waitingUsers.size());
							b = bookDB.get(itemID);
							int iterator = waitingUsers.size() < bookDB.get(itemID).getNumberOfCopies()
									? waitingUsers.size()
									: bookDB.get(itemID).getNumberOfCopies();
							for (int i = 0; i < iterator; i++) {
								String waitingUser = waitingUsers.get(i);
								waitingUsers.remove(waitingUser);
								b.setNumberOfCopies(b.getNumberOfCopies()-1);
								if(borrowedBooks.containsKey(itemID)) {
									borrowedBooks.get(itemID).add(waitingUser);
								}else {
									List<String> temp = new LinkedList<>();
									temp.add(waitingUser);
									borrowedBooks.put(itemID,temp);
								}
								log.debug("Book issues to "+waitingUser);
							}
						}
						return true;
					} catch (RemoteException e) {
						log.error("Unable to perform addBooksToLibrary() in Database.",e);
						e.printStackTrace();
						throw e;
					}	
				}
			}
		}
	}

	// return 0 if operation is failed, 1 if operation is successful, 2 when
	// quantity is greater than the number of copies which library have and 3 if there is no item to delete.
	public int removeBooksFromLibrary(String itemID, int quantity) throws RemoteException {
		log.debug("Inside removeBooksFromLibrary(String itemID, int quantity) method.");
		log.debug("call parameters: itemID-"+itemID+" , quantity-"+quantity);
		BookImpl b;
		synchronized (bookDB) {
			synchronized (borrowedBooks) {
				try {
					if (bookDB.containsKey(itemID)) {
						b = bookDB.get(itemID);
						if (quantity == -1) {
							bookDB.remove(b.getId());
							borrowedBooks.remove(b.getId());
							log.debug("Book is completely removed from DB.");
							return 1;
						} else if (quantity <= b.getNumberOfCopies()) {
							b.setNumberOfCopies(b.getNumberOfCopies() - quantity);
							log.debug("No. of copies decremented by quantity.");
							return 1;
						} else if (quantity > b.getNumberOfCopies()) {
							log.debug("Quantity to decrease is higher than available books in library. So doing nothing.");
							return 2;
						}
						else {
							log.debug("Returning without changing DB.");
							return 0;
						}
							
						/*
						 * // return 0 if operation is unsuccessful, 1 if its successful and 2 if input
						 * is // greater than available books in library and 3 if nothing is available.
						 * if (b.getNumberOfCopies() > quantity) {
						 * b.setNumberOfCopies(b.getNumberOfCopies() - quantity);
						 * System.out.println("Removed book from library only."); } else { quantity =
						 * quantity - b.getNumberOfCopies(); // b.setNumberOfCopies(0);
						 * bookDB.remove(itemID);
						 * System.out.println("All library owned books are deleted.");
						 * 
						 * List<String> usersWithBooks = borrowedBooks.get(itemID); if (usersWithBooks
						 * != null) { int iteration = usersWithBooks.size() <= quantity ?
						 * usersWithBooks.size() : quantity; for (int i = 0; i < iteration; i++) {
						 * System.out.println("Book is taken from " + usersWithBooks.get(i) +
						 * " and deleted."); usersWithBooks.remove(i); } } } return true;
						 */
					} else {
						log.debug("There is no item found in library to remove it.");
						return 3;
					}
				} catch (RemoteException e) {
					log.error("Unable to perform removeBooksFromLibrary() in Database.",e);
					e.printStackTrace();
					throw e;
				}
			}
		}
	}

	public List<Book> getAllBooks() {
		log.debug("Inside getAllBooks() method.");
		synchronized (bookDB) {
			List<Book> list = new ArrayList<>(bookDB.values());
			log.debug("Returning "+list.size()+" books.");
			return list;
		}
	}

	public boolean returnBook(String userID, String itemID) throws RemoteException {
		log.debug("Inside returnBook(String userID, String itemID) method.");
		log.debug("call parameters: userID-"+userID+" , itemID-"+itemID);
		// return the book to library and assign it to user if there is any in waiting
		// list for that book.
		synchronized (borrowedBooks) {
			synchronized (bookDB) {
				synchronized (waitingList) {
					try {
						if (borrowedBooks.containsKey(itemID) && borrowedBooks.get(itemID).contains(userID)) {
							if (borrowedBooks.get(itemID).remove(userID)) {
								if (waitingList.containsKey(itemID) && waitingList.get(itemID).size() > 0) {
									String waitingUser = waitingList.get(itemID).get(0);
									waitingList.get(itemID).remove(waitingUser);
									borrowedBooks.get(itemID).add(waitingUser);
									log.debug("Book re-assigned to " + waitingUser+" who was in waiting list.");
								} else {
									BookImpl book = bookDB.get(itemID);
									book.setNumberOfCopies(book.getNumberOfCopies() + 1);
									log.debug("Book returned to library.");
								}
								return true;
							} else
								return false;
						} else
							return false;
					} catch (RemoteException e) {
						log.error("Unable to perform returnBook() in Database.",e);
						e.printStackTrace();
						throw e;
					}
				}
			}
		}
	}

	public List<BookImpl> findItem(String itemName) throws RemoteException {
		log.debug("Inside findItem(String itemName) method.");
		log.debug("call parameters: itemName-"+itemName);
		// look in map and return it.
		List<BookImpl> books;
		synchronized (bookDB) {
			books = new ArrayList<>();
			BookImpl book;
			for (Iterator<String> iterator = bookDB.keySet().iterator(); iterator.hasNext();) {
				String string = (String) iterator.next();
				book = bookDB.get(string);
				try {
					if (book.getName().equalsIgnoreCase(itemName))
						books.add(book);
				} catch (RemoteException e) {
					log.error("Unable to perform findItem() in Database.",e);
					e.printStackTrace();
					throw e;
				}
			}
		}
		log.debug("no of books found with itemName are "+books.size());
		return books;

	}

	// 0 if user is not from this Library else 1
	public int borrowBook(String userID, String itemID, int thisLibraryUser) throws RemoteException {
		log.debug("Inside borrowBook(String userID, String itemID, int thisLibraryUser) method.");
		log.debug("call parameters: userID-"+userID+" ,itemID-"+itemID+" ,thisLibraryUser-"+thisLibraryUser);
		// return -1 if the book doesn't exist in library, 0 if it isn't borrowed, 1 if
		// book is borrowed and 2 if user can't
		// borrow more items from this library.
		synchronized (bookDB) {
			synchronized (borrowedBooks) {
				try {
					if (!bookDB.containsKey(itemID))
						return -1;
					else {
						if (thisLibraryUser == 0 && borrowedBooks.containsKey(itemID)
								&& borrowedBooks.get(itemID).contains(userID)) {
							log.debug("This user belongs to different university and already have a copy of request book.");
							return 2;
						}
						else {
							BookImpl book = bookDB.get(itemID);
							if (book.getNumberOfCopies() > 0) {
								book.setNumberOfCopies(book.getNumberOfCopies() - 1);
								if (borrowedBooks.containsKey(itemID)) {
									borrowedBooks.get(itemID).add(userID);
								} else {
									List<String> tempList = new LinkedList<>();
									tempList.add(userID);
									borrowedBooks.put(itemID, tempList);
								}
								log.debug("Assigned requested book to user.");
								return 1;
							} else {
								log.debug("Unable to assign requested book to user.");
								return 0;
							}		
						}
					}
				} catch (RemoteException e) {
					log.debug("Unable to perform borrowBook() in Database.",e);
					e.printStackTrace();
					throw e;
				}
			}
		}
	}

	
	public boolean addUserToWaitingList(String userID, String itemID) {
		log.debug("Inside addUserToWaitingList(String userID, String itemID) method.");
		log.debug("call parameters: userID-"+userID+" ,itemID-"+itemID);
		synchronized (waitingList) {
			if (waitingList.containsKey(itemID) && waitingList.get(itemID) != null) {
				waitingList.get(itemID).add(userID);
			} else {
				List<String> userList = new ArrayList<>();
				userList.add(userID);
				waitingList.put(itemID, userList);
			}
			log.debug("Added userId to waiting list.");
			return true;
		}

	}
}
