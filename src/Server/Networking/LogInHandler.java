package Server.Networking;

import Action.*;
import Logging.Logger;
import Model.User.*;
import Model.UserAuthenticator;
import Model.UserManager;
import Networking.CommLink;
import Networking.Gson.GsonUtils;
import Server.Game.Lobby;
import Server.Game.UserHandler;
import com.google.gson.JsonSyntaxException;

/**
 * Created by fiore on 10/05/2017.
 */
public class LogInHandler implements LinkHandler {

    private final UserHandler userHandler = Lobby.getInstance();

    private final UserAuthenticator userFactory = UserManager.getInstance();

    public void addClientComm(CommLink newLink) {
        Logger.log(Logger.LogLevel.Normal, "New client connected.");

        // Set link authentication method as link callback
        newLink.setOnMessage(this::clientHandshake);
    }

    /**
     * Takes care of client authentication before it can be able to do any other action
     *
     * @param link New link not bound to a user
     * @param message First message received on the link
     */
    private void clientHandshake(CommLink link, final String message) {
        BaseAction firstReceivedAction;

        try {
            firstReceivedAction = GsonUtils.fromGson(message);//Deserializes action

            //We only accept registration/login actions:
            if (!(firstReceivedAction instanceof LoginOrRegister)) {//This looks like switch case...
                BaseAction popup = new DisplayPopup(DisplayPopup.Level.Error, "You first have to login");
                link.sendMessage(popup);
                return;
            }

        } catch (JsonSyntaxException e) {
            //Send error to the client
            BaseAction popup = new DisplayPopup(DisplayPopup.Level.Error, "This server accepts only json strings");
            link.sendMessage(popup);
            return;
        }

        LoginOrRegister loginAction = (LoginOrRegister) firstReceivedAction;

        // Retrieves username and password
        String username = loginAction.getUsername();
        String passwordHash = loginAction.getPasswordHash();
        boolean isNewUser = loginAction.isNewUser();

        User authorizedUser = null;

        // Check the database for authentication or create new user
        try {

            if (isNewUser)
                authorizedUser = userFactory.createUser(username, passwordHash);
            else
                authorizedUser = userFactory.authenticateUser(username, passwordHash);

        } catch (UserAlreadyExistentException uaee) {
            Logger.log(Logger.LogLevel.Warning, "Attempted to add already existent user.\n" + uaee.getMessage());

            //Send error to the client
            BaseAction popup = new DisplayPopup(DisplayPopup.Level.Error, "User already exists.");
            link.sendMessage(popup);

            return;
        }
        catch (UserNotFoundException unfe) {
            Logger.log(Logger.LogLevel.Warning, "Login attempt resulted in user not found.\n" + unfe.getMessage());

            //Send error to the client
            BaseAction popup = new DisplayPopup(DisplayPopup.Level.Error, "User " + username + " not found.");
            link.sendMessage(popup);

            return;
        }
        catch (WrongPasswordException wpe) {
            Logger.log(Logger.LogLevel.Warning, "Login attempt with wrong password on user " + username + ".\n" + wpe.getMessage());

            //Send error to the client
            BaseAction popup = new DisplayPopup(DisplayPopup.Level.Error, "Wrong password.");
            link.sendMessage(popup);

            return;
        }
        catch (UserAlreadyLoggedException uale) {
            Logger.log(Logger.LogLevel.Warning, "User " + username + " attempted multiple login.\n" + uale.getMessage());

            BaseAction popup = new DisplayPopup(DisplayPopup.Level.Error, uale.getMessage());
            link.sendMessage(popup);
        }

        // If user is null log generic error and retry
        if(authorizedUser == null) {
            Logger.log(Logger.LogLevel.Warning, "Generic error while creating user.\n");

            //Send generic error to client
            BaseAction popup = new DisplayPopup(DisplayPopup.Level.Error, "Dunno uot appened... Doh!!");
            link.sendMessage(popup);

            return;
        }

        // Bind link to user data
        authorizedUser.setCommLink(link);

        Logger.log(Logger.LogLevel.Normal, "User " + authorizedUser.getUsername() + " connected.");

        //Send User object to connected user
        BaseAction sendUserObj = new UpdateUserObject(authorizedUser);
        link.sendMessage(sendUserObj);

        //Move user UI to Lobby
        BaseAction changeView = new ChangeClientView(ChangeClientView.View.LOBBY);
        link.sendMessage(changeView);

        // Pass user to user handler
        userHandler.addUser(authorizedUser);
    }
}
