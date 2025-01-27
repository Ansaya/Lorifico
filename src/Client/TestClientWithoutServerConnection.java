package Client;

import Client.UI.GUI.GameUIController;
import Client.UI.GUI.resources.gameComponents.MyCameraGroup;
import Client.UI.UserInterface;
import Client.UI.UserInterfaceFactory;
import Game.UserObjects.FamilyColor;
import com.budhash.cliche.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by andrea on 10/05/2017.
 *
 * This main is used for testing purpose only
 */
public class TestClientWithoutServerConnection {


    public static void main(String[] args) {
        UserInterface userInterface = UserInterfaceFactory.getInstance(UserInterfaceFactory.UserInterfaceType.JAVAFX);


        final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
        executor.schedule(new Runnable() {
            @Override
            public void run() {
                userInterface.getGameUI().showPage();
            }
        }, 1, TimeUnit.SECONDS);


        new Thread(() -> {
            try {
                ShellFactory.createConsoleShell("Debuggenzo", "?list per conoscere i comandi", new DebugLorenzo())
                        .commandLoop();
            } catch (IOException e) {
                System.err.println("Unable to start CLI Interface");
                e.printStackTrace();
            }
        }).start();

        userInterface.init(args);

    }

    public static class DebugLorenzo implements ShellDependent {
        private Shell shell;

        private UserInterface userInterface = UserInterfaceFactory.getInstance();

        @Command
        public String showTower() {
            GameUIController gameUIController = (GameUIController) UserInterfaceFactory.getInstance().getGameUI();
            gameUIController.getCameraGroup().setView(MyCameraGroup.CameraPosition.TOWERS);
            return "OK";
        }

        @Command
        public String showGameTable() {
            GameUIController gameUIController = (GameUIController) UserInterfaceFactory.getInstance().getGameUI();
            gameUIController.getCameraGroup().setView(MyCameraGroup.CameraPosition.GAMETABLE);
            return "OK";
        }

        @Command
        public String setDice(int one, int two, int three) {
            userInterface.getGameUI().getDiceController().setNumbers(one, two, three);
            return "OK";
        }

        @Command
        public String setFaithCards(@Param(name = "CardID") int first, @Param(name = "CardID") int second, @Param(name = "CardID") int third) {
            userInterface.getGameUI().getFaithController().showFaithCards(first, second, third);
            return "OK";
        }

        @Command
        public String showCard(@Param(name = "Posizione") int position, @Param(name = "CardID") int cardid) {
            userInterface.getGameUI().getTowersController().showCardOnTowers(position, cardid);
            return "OK";
        }

        @Command
        public String removeAllCards() {
            userInterface.getGameUI().getTowersController().removeAllCardsFromTowers();
            return "OK";
        }

        @Command
        public String removeCard(int cardNumber) {
            userInterface.getGameUI().getTowersController().removeCardFromTower(cardNumber);
            return "OK";
        }

        @Command
        public String setRoundOrder() {
            List<FamilyColor> roundOrder = new ArrayList<FamilyColor>() {{
                add(FamilyColor.Green);
                add(FamilyColor.Red);
            }};
            UserInterfaceFactory.getInstance().getGameUI().getRoundOrderController().setGameOrder(roundOrder);

            return "OK";
        }

        @Command
        public String setAnotherRoundOrder() {
            List<FamilyColor> roundOrder = new ArrayList<FamilyColor>() {{
                add(FamilyColor.Yellow);
                add(FamilyColor.Red);
                add(FamilyColor.Green);
                add(FamilyColor.Blue);
            }};
            UserInterfaceFactory.getInstance().getGameUI().getRoundOrderController().setGameOrder(roundOrder);

            return "OK";
        }

        public void cliSetShell(Shell theShell) {
            this.shell = theShell;
        }

        public Shell getShell() {
            return shell;
        }
    }
}