package Server.Game.UserObjects;

import Action.TowersUpdate;
import Game.Cards.CardType;
import Game.Effects.Effect;
import Server.Game.Positions.Position;
import Game.Positions.PositionType;
import Game.UserObjects.Choosable;
import Game.UserObjects.DomesticColor;
import Logging.Logger;
import Networking.Gson.MySerializer;
import Server.Game.Positions.*;
import com.google.gson.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by fiore on 11/05/2017.
 */
public class GameTable {

    private final Map<CardType, List<TowerPosition>> towers = new HashMap<>();

    private final Map<Integer, Position> positions = new HashMap<>();

    private final List<GameUser> nextTurnOrder = Collections.synchronizedList(new ArrayList<>());

    private final Map<DomesticColor, Integer> diceValue = new HashMap<>();

    private volatile Effect currentFaithEffect = null;

    /**
     * Load game table object from specified input json and correct number of positions for specified player's number
     *
     * @param jsonSetupFile Setup file
     * @param players Number of players
     * @return Initialized game table
     * @throws IOException If setup file isn't found
     */
    public static GameTable load(String jsonSetupFile, int players) throws IOException {

        Path jsonSetupPath = Paths.get(jsonSetupFile);

        if(Files.notExists(jsonSetupPath))
            throw new FileNotFoundException("File doesn't exists!");

        // Initialize json deserializer
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Position.class, new MySerializer<Position>())
                .registerTypeAdapter(Effect.class, new MySerializer<Effect>())
                .create();

        GameTable table = gson.fromJson(Files.newBufferedReader(jsonSetupPath), GameTable.class);

        // Initialize aggregates map to empty
        Map<PositionType, List<Position>> aggregates = new HashMap<>();
        for(PositionType type : PositionType.values())
            aggregates.put(type, new ArrayList<>());

        // Add all tower positions to global list
        table.towers.values().forEach(tower -> tower.forEach(position -> table.positions.put(position.getNumber(), position)));

        // Remove addition harvest and production positions if two players only
        if(players < 3) {
            Position singleHarvest = table.positions.get(20);
            Position singleProduction = table.positions.get(30);

            Set<Integer> numbers = new HashSet<>(table.positions.keySet());

            for (Integer i : numbers) {

                PositionType current = table.positions.get(i).getType();

                if(current == PositionType.ProductionAction
                        || current == PositionType.HarvestAction)
                    table.positions.remove(i);
            }

            table.positions.put(20, singleHarvest);
            table.positions.put(30, singleProduction);
        }

        // Remove additional market positions if less than four players
        if(players < 4) {
            table.positions.remove(42);
            table.positions.remove(43);
        }

        // Populate aggregates map
        table.positions.forEach((number, position) -> aggregates.get(position.getType()).add(position));

        // Create position aggregates
        aggregates.values().forEach(PositionAggregate::aggregate);

        // Set order list to update in council positions
        aggregates.get(PositionType.Council).forEach(position -> ((CouncilPosition)position).setOrderList(table.nextTurnOrder));

        return table;
    }

    public static GameTable load(int players) throws IOException {
        try {
            return load("src/Server/Game/Positions/Serialize/table.json", players);
        } catch (IOException ioe) {
            Logger.log(Logger.LogLevel.Error, "Can't find standard table setup file.\n" + ioe.getMessage());
            throw ioe;
        }
    }

    /**
     * Get activable effects/affordable costs of requested positions for given user
     *
     * @param currentUser User to check positions for
     * @param requestedPositions Requested positions (null to get all positions)
     * @return Map of positions number and activable effects/affordable costs
     */
    @SuppressWarnings("unchecked")
    public Map<Integer, List<Choosable>> getPositions(GameUser currentUser, List<PositionType> requestedPositions) {
        final AtomicBoolean getAll = new AtomicBoolean();
        getAll.set(requestedPositions == null);

        Map<Integer, List<Choosable>> choseForPos = new HashMap<>();

        positions.values().forEach(pos -> {
            if(getAll.get() || requestedPositions.contains(pos.getType()))
                choseForPos.put(pos.getNumber(), pos.canOccupy(currentUser.getUserState()));
        });

        return choseForPos;
    }

    /**
     * Get current faith effect
     *
     * @return Permanent faith effect
     */
    public Effect getFaithEffect() {
        return currentFaithEffect;
    }

    /**
     * Set given cards in corresponding towers, update faith effect if specified
     *
     * @param newCards New set of cards
     * @param newFaithEffect Faith effect (if null previous will remain)
     * @return Update message with new card associated with tower positions
     */
    public TowersUpdate changeTurn(Map<CardType, List<Server.Game.Cards.Card>> newCards, Effect newFaithEffect) {

        // Free all positions
        positions.values().forEach(Position::free);

        // Update cards in tower positions
        newCards.forEach((type, list) ->
                towers.get(type).forEach(position ->
                        position.setCard(list.remove(0)))
        );

        // Update faith effect if necessary
        if(newFaithEffect != null)
            currentFaithEffect = newFaithEffect;

        // Return towers card update
        return new TowersUpdate(towers);
    }

    /**
     * Throw all three dice and return results
     *
     * @return Dice throw results
     */
    public Map<DomesticColor, Integer> getDiceValue() {

        diceValue.clear();
        Random die = new Random(System.currentTimeMillis());

        diceValue.put(DomesticColor.Black, die.nextInt(6) + 1);
        diceValue.put(DomesticColor.Orange, die.nextInt(6) + 1);
        diceValue.put(DomesticColor.White, die.nextInt(6) + 1);
        diceValue.put(DomesticColor.Neutral, 0);

        return diceValue;
    }

    /**
     * Get new players order for next turn
     *
     * @param currentOrder Players order from current round
     * @return Players order for next round
     */
    public List<GameUser> changeOrder(List<GameUser> currentOrder) {

        // Remove family in council from current order
        currentOrder.removeAll(nextTurnOrder);

        // Add left family at the end of list
        nextTurnOrder.addAll(currentOrder);

        // Create new order list
        List<GameUser> newOrder = new ArrayList<>(nextTurnOrder);

        // Clear council order
        nextTurnOrder.clear();

        // Return new round order
        return newOrder;
    }

    /**
     * Occupy requested position with given user
     *
     * @param currentUser Current user
     * @param positionNumber Number of position to occupy
     * @param chosenTs Chosen effects/cost to activate/pay occupying specified position
     * @return Updated position
     */
    @SuppressWarnings("unchecked")
    public Position occupy(GameUser currentUser, int positionNumber, List<Choosable> chosenTs) {

        // Updated position reference
        Position requestedPos = positions.get(positionNumber);

        PlayerState newState = (PlayerState) requestedPos.occupy(currentUser.getUserState(), chosenTs);

        currentUser.updateUserState(newState);

        // Return updated position
        return requestedPos;
    }

}
