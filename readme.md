jme-jfx-gui
-

**Requires**: Java 11

A JavaFX implementation for JmonkeyEngine that allows you to use regular a JavaFX
GUI in your games.

**History**  

Based on : https://github.com/empirephoenix/JME3-JFX  
This repository is no longer maintained, and is the original Java 7/8 implementation.

Based on : https://github.com/JavaSaBr/JME3-JFX  
This repository is based on the original JME-JFX repository above, and is a Java 10/11 implementation.  
  
**Introduction**  
This implementation is based on the JavaSaBr implementation of JME-JFX with the dependency to RLib removed
and any 3D scene code removed, leaving only the 2D GuiNode implementation.  

In practice it makes sense to use JavaFX as a 2D GUI, and for the 3D scene I recommend Lemur.

This implementation supports FXML and CSS scenes, which means you can use the Gluon SceneBuilder application
to create a fully-fledged themed scene with JavaFx Controllers.

**Add the library to your project**

``` groovy
not_uploaded_yet
```

**Integrate it into your game**

``` java

public class Main extends SimpleApplication {

    public static void main(String... args) {
        Main main = new Main();
        main.start();
    }

    TestJavaFx() {
        super(new StatsAppState());
    }


    @Override
    public void simpleInitApp() {

        // integrate javafx into jmonkey.
        JavaFxUI.initialize(this);

        // create a JavaFX control
        Button button = new Button("Click Me");
        
        // assign a click action
        button.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION, "I'm going to remove the button!");
            alert.showAndWait();

            // detach the button when it's clicked.
            JavaFxUI.detachChild(button);
        });

        // set the position of the butotn
        button.setLayoutX(30);
        button.setLayoutY(20);

        // attach it to the UI
        JavaFxUI.attachChild(button);
    }
}

```

**Controls**
You may create any JavaFX object that extends **javafx.scene.Node** in either code or fxml.
Controllers and loading fxml from resources are all supported.


Controls are added and removed similar to the jmonkey workflow via the JavaFxUI static class.

``` java

// add a control
JavaFxUI.attachChild(javafx.scene.Node node);

// remove a control
JavaFxUI.detachChild(javafx.scene.Node node);
JavaFxUI.detachChild(String fxId);

// get a control
JavaFxUI.getChild(String fxId);

```

Since all JavaFX controls run in the JavaFX thread, you may need to alternate between the JME thread and
the JavaFX thread.

``` java

// do something in the JavaFX thread (such as update a label text)
JavaFxUI.runInJavaFxThread(() -> {
    myLabel.setText("changed text");
});

// do something in the JME thread (such as manipulate the scene)
JavaFxUI.runInJmeThread(() -> {
    someJmeNode.setLocalTranslation(x, y, z);
});

```

Although JavaFX alerts are supported, you may wish to display an in-game custom dialog.
These dialogs allow you to dim the background and stop background objects being clicked.

``` java

// display a javafx control that is centered in the screen with a dimmed background.
JavaFxUI.showDialog(myJavaFxControl)

// display a javafx control that is centered and NOT dimmed.
JavaFxUI.showDialog(myJavaFxControl, false);

// remove the dialog
JavaFxUI.removeDialog();

```
