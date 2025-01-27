package Client.UI.GUI.resources.gameComponents;

import Game.UserObjects.DomesticColor;
import Game.UserObjects.FamilyColor;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Cylinder;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by andrea on 05/06/17.
 */
public class Domestic3D extends Group {
    public static Domestic3D last;
    private final double CYLINDER_RADIUS = 12;
    private final double CYLINDER_HEIGHT = 35;
    private final double CYLINDER_MIN_Z = -5;
    private Server.Game.UserObjects.Domestic serverDomestic;
    private Translate translate;
    private Timeline timeline;
    private Map<DomesticColor, Color> cylinderTopColorMap = new HashMap<DomesticColor, Color>() {{
        put(DomesticColor.Black, Color.BLACK);
        put(DomesticColor.Orange, Color.ORANGE);
        put(DomesticColor.White, Color.WHITE);
        put(DomesticColor.Neutral, Color.GRAY);
    }};

    private Map<FamilyColor, Color> domesticColorMap = new HashMap<FamilyColor, Color>() {{
        put(FamilyColor.Green, Color.GREEN);
        put(FamilyColor.Blue, Color.BLUE);
        put(FamilyColor.Yellow, Color.YELLOW);
        put(FamilyColor.Red, Color.RED);
    }};


    /**
     * Creates a new 3D domestic, this will be placed in towers or gameTable places
     *
     * @param serverDomestic
     */
    public Domestic3D(Server.Game.UserObjects.Domestic serverDomestic) {


        //Create base cylinder
        Cylinder cylinder = new Cylinder(CYLINDER_RADIUS, CYLINDER_HEIGHT);

        //Put 'cover' of dice color on top of cylinder
        Circle cylinderTop = new Circle(CYLINDER_RADIUS / 1.5);


        //Color parts
        if (serverDomestic.getType().equals(DomesticColor.Neutral)) {
            //if domestic is the neutral one, covering is filled with family's color
            cylinderTop.setFill(domesticColorMap.get(serverDomestic.getFamilyColor()));

            //if domestis is neutral, cylinder is filled with neutral color:
            PhongMaterial phongMaterial = new PhongMaterial(cylinderTopColorMap.get(DomesticColor.Neutral));
            cylinder.setMaterial(phongMaterial);
        } else {
            //If domestic is regular one, cylinder is filled with family's color
            PhongMaterial phongMaterial = new PhongMaterial(domesticColorMap.get(serverDomestic.getFamilyColor()));
            cylinder.setMaterial(phongMaterial);
            //Cilynder top is filled with dice's color.
            cylinderTop.setFill(cylinderTopColorMap.get(serverDomestic.getType()));
        }

        Group cylinderTopGroup = new Group(cylinderTop);
        cylinderTopGroup.getTransforms().addAll(new Rotate(90, Rotate.X_AXIS),
                new Translate(cylinder.getLayoutX(), cylinder.getLayoutX(),
                        cylinder.getHeight() * 0.51));

        //Add Translate / Rotate
        translate = new Translate();
        translate.setX(100);
        translate.setY(100);
        translate.setZ(-100);

        Rotate rotate = new Rotate(90, Rotate.X_AXIS);

        getChildren().addAll(cylinder, cylinderTopGroup);
        getTransforms().addAll(translate, rotate);

        last = this;
    }

    public Translate getTranslate() {
        return translate;
    }

    public void setPos(double x, double y, double z) {
        translate.setX(x);
        translate.setY(y);
        translate.setZ(z);
    }
}
