package Action;

import Client.Datawarehouse;
import Client.UI.UserInterfaceFactory;
import Game.UserObjects.DomesticColor;
import Model.User.User;
import Server.Game.UserObjects.Domestic;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by fiore on 01/06/2017.
 */
public class DiceDomesticUpdate extends UserSpecific implements BaseAction {

    private final Map<DomesticColor, Integer> diceValues;

    private final Map<DomesticColor, Integer> domesticValues = new HashMap<>();

    public DiceDomesticUpdate(String username, Map<DomesticColor, Integer> dice, Map<DomesticColor, Domestic> domestics) {
        super(username);
        this.diceValues = dice;
        domestics.values().forEach(domestic -> domesticValues.put(domestic.getType(), domestic.getValue()));
    }

    @Override
    public void doAction(User user) {

        //Update dices only once (when my username is received)
        if (Datawarehouse.getInstance().getMyUsername().equals(getUsername())) {
            UserInterfaceFactory.getInstance().getGameUI().getDiceController().setNumbers(diceValues.get(DomesticColor.Black),
                    diceValues.get(DomesticColor.White), diceValues.get(DomesticColor.Orange));
        }

        // TODO: update user domestic value (dice and domestic values could be different)
    }
}
