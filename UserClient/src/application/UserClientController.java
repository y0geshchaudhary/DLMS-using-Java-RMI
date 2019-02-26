package application;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import centralRepo.interfaces.Repository;
import centralRepo.interfaces.ServerDetail;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import server.interfaces.Book;
import server.interfaces.LibraryOperations;

public class UserClientController {

	@FXML
	private TextField userIdTF;

	@FXML
	private ChoiceBox<String> operationDD;

	/*@FXML
	private ChoiceBox<String> libraryDD;*/

	@FXML
	private TextField itemIdTF;

	@FXML
	private TextField itemNameTF;

	@FXML
	private Button goButton;

	@FXML
	private Button quitButton;

	@FXML
	private Label errorLabel;

	@FXML
	private TextArea outputTA;

	//private List<String> libraryLists = new ArrayList<>();
	private List<String> operations = new ArrayList<>();
	private static final Logger logger = Logger.getLogger(UserClientController.class.getName());
	private SimpleFormatter logFormatter = new SimpleFormatter();

	public void setup() {
		//Collections.addAll(libraryLists, "Concordia", "McGill", "Montreal");
		Collections.addAll(operations, "Borrow Item", "Find Item", "Return Item");
		operationDD.setItems(FXCollections.observableList(operations));
		operationDD.setValue(operations.get(0));
		/*libraryDD.setItems(FXCollections.observableList(libraryLists));
		libraryDD.setValue(libraryLists.get(0));*/
		logger.setLevel(Level.ALL);
	}

	@FXML
	void quit(ActionEvent event) {
		Platform.exit();
	}

	@FXML
	void performAction(ActionEvent event) {
		FileHandler fileHanlder = null;
		errorLabel.setText("");
		outputTA.setText("");
		if (validate()) {
			String action = operationDD.getValue().trim();
			String userId = userIdTF.getText().trim();
			String itemId = itemIdTF.getText().trim();
			String itemName = itemNameTF.getText().trim();
			LibraryOperations libraryOperations = null;
			if ((userId.startsWith("CON") || userId.startsWith("MCG") || userId.startsWith("MON"))) {

				try {
					Registry registry = LocateRegistry.getRegistry(Repository.CENTRAL_REPOSITORY_HOSTNAME,
							Repository.CENTRAL_REPOSITORY_PORT);
					Repository repository = (Repository) registry.lookup("Repository");
					ServerDetail server = repository.getServerDetails(userId.substring(0, 3));
					String serverHostname = server.getHostname();
					int serverPort = server.getPortNumber();
					String serverStub = server.getStubName();
					registry = LocateRegistry.getRegistry(serverHostname, serverPort);
					libraryOperations = (LibraryOperations) registry.lookup(serverStub);
				} catch (RemoteException e) {
					System.out.println("Issue fetching central registry.");
					errorLabel.setText("Issue fetching central registry.");
					e.printStackTrace();
				} catch (NotBoundException e) {
					System.out.println("Issue fetching central repository");
					errorLabel.setText("Issue fetching central repository");
					e.printStackTrace();
				}
				int intResult = -2;
				boolean boolResult;
				String stringResult = "";
				boolean userExists = false;
				try {
					userExists = libraryOperations.userExists(userId);
				} catch (RemoteException e) {
					System.out.println("Issue accessing Library method.");
					errorLabel.setText("Issue accessing Library method.");
					e.printStackTrace();
				}
				if (userExists) {
					String filename = "Logs/" + userId;
					// configure logger
					try {
						fileHanlder = new FileHandler(filename, true);
						fileHanlder.setLevel(Level.ALL);
						fileHanlder.setFormatter(logFormatter);
						logger.addHandler(fileHanlder);
					} catch (Exception e) {
						errorLabel.setText("Issue with log file.");
						e.printStackTrace();
					}
					logger.info("NEW REQUEST");
					logger.info("Request type:" + action);

					switch (action) {
					case "Borrow Item":
						logger.info("Item requested: " + itemId);
						if ((itemId.startsWith("CON") || itemId.startsWith("MCG") || itemId.startsWith("MON"))) {
							// return -1 if the book doesn't exist in library, 0 if it isn't borrowed, 1 if
							// book is borrowed and 2 if user can't
							// borrow more items from this library.
							try {
								intResult = libraryOperations.borrowItem(userId, itemId, "0");
							} catch (RemoteException e) {
								outputTA.setText(e.getLocalizedMessage());
								e.printStackTrace();
								logger.info(e.getLocalizedMessage());
							}
							if (intResult == -1) {
								outputTA.setText("The book doesn't exist in library.");
								logger.info("Received response as -1 which indicate that the book doesn't exist.");
							} else if (intResult == 0) {
								logger.info("Received response as 0 which indicate that the book is out of stock.");
								// popup for user to let decide if he want his name to be added to waiting list.
								Alert alert = new Alert(AlertType.CONFIRMATION);
								// alert.setTitle("Confirmation Dialog");
								alert.setHeaderText("This Book is not available this time.");
								alert.setContentText("Do you want to be added to waiting list?");

								Optional<ButtonType> result = alert.showAndWait();
								try {
									if (result.get() == ButtonType.OK) {
										boolResult = libraryOperations.addToWaitingList(userId, itemId);
										if (boolResult) {
											logger.info("user opt to get enrolled into waiting list for item" + itemId);
											outputTA.setText("Added " + userId + " to waiting list for this book.");
										} else {
											logger.info("Unable to add " + userId + " to waiting list.");
											outputTA.setText("Unable to add " + userId + " to waiting list.");
										}
									} else {
										logger.info("user didn't want to be added to waiting list.");
										outputTA.setText(
												"This book can't be borrowed this time and userId is not added to waiting list.");
									}

								} catch (RemoteException e) {
									logger.info(e.getLocalizedMessage());
									e.printStackTrace();
									outputTA.setText(e.getLocalizedMessage());
								}
							} else if (intResult == 1) {
								logger.info("Received response as 1 which indicate that the book is issues to user.");
								outputTA.setText("Book is issued to " + userId);
							} else if (intResult == 2) {
								logger.info("Received response as 2 which indicate that " + userId
										+ " has already borrowed a copy of book from " + itemId.substring(0, 3)
										+ "server.");
								outputTA.setText("This user already have one copy of this book from this library.");
							}
						} else {
							errorLabel.setText("Enter valid itemId.");
							logger.info("Item requested is invalid.");
						}
						break;

					case "Find Item":
						logger.info("Item Requested: "+itemName);
						try {
							List<Book> books = libraryOperations.findItem(userId, itemName);
							for (Book book : books) {
								stringResult = stringResult.concat(book.getId().concat(" ")
										.concat(String.valueOf(book.getNumberOfCopies()).concat(", ")));
							}
							stringResult = stringResult.substring(0, stringResult.length() - 2);
							logger.info("Details recieved : "+stringResult);
							outputTA.setText(stringResult);
						} catch (RemoteException e) {
							logger.info(e.getLocalizedMessage());
							outputTA.setText(e.getLocalizedMessage());
							e.printStackTrace();
						}
						break;

					case "Return Item":
						logger.info("Item to return : "+itemId);
						if ((itemId.startsWith("CON") || itemId.startsWith("MCG") || itemId.startsWith("MON"))) {
							try {
								stringResult = libraryOperations.returnItem(userId, itemId);
								logger.info("Response received from server : "+stringResult);
								outputTA.setText(stringResult);
							} catch (RemoteException e) {
								outputTA.setText(e.getLocalizedMessage());
								e.printStackTrace();
								logger.info(e.getLocalizedMessage());
							}
						} else {
							logger.info(itemId+" is not a valid itemId.");
							errorLabel.setText("Enter valid itemId.");
						}
						break;
					}
				} else
					errorLabel.setText("This userId is not found in Library database.");
			} else
				errorLabel.setText("Enter valid userId.");
		}
		if(fileHanlder!=null) fileHanlder.close();
	}

	private boolean validate() {
		errorLabel.setText("");
		boolean result = false;
		if (userIdTF.getText().trim().length() > 0) {
			if (operationDD.getValue().equals(operations.get(0)) || operationDD.getValue().equals(operations.get(2))) {
				if (itemIdTF.getText().trim().length() > 0)
					result = true;
				else
					errorLabel.setText("Enter Item Id.");
			} else if (operationDD.getValue().equals(operations.get(1))) {
				if (itemNameTF.getText().trim().length() > 0)
					result = true;
				else
					errorLabel.setText("Enter Item Name.");
			}
		} else {
			errorLabel.setText("Enter User Id.");
		}

		return result;
	}
}
