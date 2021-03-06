package milasoft.accountchecker;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.dreambot.api.script.AbstractScript;
import org.dreambot.api.script.Category;
import org.dreambot.api.script.ScriptManifest;

@ScriptManifest(author = "Milasoft", category = Category.UTILITY, name = "Milasoft Account Checker", version = 1.0)
public class AccountChecker extends AbstractScript {

	String filePath = "";
	final String ACCOUNT_FILE = "accounts.txt";
	final String COMPLETED_FILE = "completed.txt";
	final String FAILED_FILE = "failed.txt";
	
	Point usernameField = new Point(316, 247);
	Rectangle existingUser = new Rectangle(394, 274, 136, 32);
	Rectangle backButton = new Rectangle(314, 309, 136, 32);
	Rectangle cancelButton = new Rectangle(394, 305, 136, 32);
	Rectangle tryAgain = new Rectangle(316, 260, 136, 32);
	
	Rectangle logoutX = new Rectangle(737, 4, 25, 18);
	Rectangle logoutButton = new Rectangle(595, 376, 136, 28);
	
	Account account;
	Queue<Account> accountQueue;
		
	@Override
	public void onStart() {
		createFilePath();
		accountQueue = new LinkedList<Account>();
		loadAccounts();
	}
	
	@Override
	public int onLoop() {
		if(getClient().isLoggedIn()) {
			if(!fileContainsUsername(filePath + COMPLETED_FILE)) {
				if(getPlayerSettings().getConfig(281) >= 1000) {
					account.setStatus(AccountStatus.COMPLETED_TUTORIAL);
				} else {
					account.setStatus(AccountStatus.STARTED_TUTORIAL);			
				}
				writeFile(filePath + COMPLETED_FILE);
			} else {
				if(getClientSettings().isResizableActive()) {
					getMouse().click(logoutX);
					sleep(2000, 3000);
					getMouse().click(logoutButton);
				} else {
					getTabs().logout();
				}
				sleepUntil(() -> !getClient().isLoggedIn(), 6000);
			}
		} else {
			if(accountQueue.peek() != null) {
				account = accountQueue.poll();
				if(!login()) {
					writeFile(filePath + FAILED_FILE);
				}
			} else {
				log("Out of accounts, shutting down.");
				stop();
			}
		}
		return 1500;
	}

	void loadAccounts() {
		try {
			Files.lines(Paths.get(filePath + ACCOUNT_FILE)).forEach(line -> {
				String[] accountData = line.split(":");
				accountQueue.add(new Account(accountData[0], accountData[1]));
			});
		} catch (FileNotFoundException e) {
			log("Can't find " + ACCOUNT_FILE);
			stop();
		} catch (IOException e) {
			log("Error reading data from file. Disable fresh start and retry.");
			e.printStackTrace();
			stop();
		}
	}
	
	void createFilePath() {
		String os = System.getProperty("os.name");
		if(os.contains("Windows")) {
			filePath = "C:/Users/" + System.getProperty("user.name") + "/milasoft/AccountChecker/";
		} else if(os.contains("Linux")) {
			filePath = "/home/" + System.getProperty("user.name") + "/milasoft/AccountChecker/";
		} else if(os.contains("Mac")) {
			filePath = "/Users/" + System.getProperty("user.name") + "/milasoft/AccountChecker/";
		} else {
			filePath = System.getProperty("user.home") + "/milasoft/AccountChecker/";
		}
		File fileDir = new File(filePath);
		fileDir.mkdirs();
	}
	
	void writeFile(String filename) {
		String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM/dd hh:mm a"));
		List<String> line = Arrays.asList(currentTime + ": " + account.getUsername() + " Status: " + account.getStatus().toString());
		try {
			Files.write(Paths.get(filename), line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	boolean fileContainsUsername(String filename) {
		try {
			return Files.lines(Paths.get(filename)).anyMatch(s -> s.contains(account.getUsername()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	boolean login() {
		if(getClient().getLoginIndex() == 0) {
			getMouse().click(existingUser);
			sleepUntil(() -> getClient().getLoginIndex() != 0, 5000);
		}
		if(getClient().getLoginIndex() == 2) {
			getMouse().click(usernameField);
			getKeyboard().type(account.getUsername());
            sleep(600);
            getKeyboard().type(account.getPassword());
            sleepUntil(() -> getClient().isLoggedIn() || getClient().getLoginIndex() != 2, 10500);
            if(getClient().isLoggedIn()) {
            	return true;
            } else {
            	switch(getClient().getLoginResponse()) {
            		case MEMBERS_AREA:
            			account.setStatus(AccountStatus.MEMBERS_AREA);
                		getMouse().click(tryAgain);
            			sleep(500);
            			getMouse().click(cancelButton);
                		return false;
            		default:
            			break;
            	}
            	switch(getClient().getLoginIndex()) {
            		case 3:
            			account.setStatus(AccountStatus.INCORRECT_INFO);
            			getMouse().click(tryAgain);
            			sleep(500);
            			getMouse().click(cancelButton);
            			return false;
            		case 12:
            			account.setStatus(AccountStatus.DISABLED);
            			getMouse().click(backButton);
            			sleep(500);
            			getMouse().click(cancelButton);
            			return false;
            	}
            }
		}
		return false;
	}
}
