jme-jfx-gui
-

 [ ![Download](https://api.bintray.com/packages/jayfella/com.jayfella/jme-jfx-11/images/download.svg) ](https://bintray.com/jayfella/com.jayfella/jme-jfx-11/_latestVersion) 
 

**Requires**: Java 11

A JavaFX 11 implementation for JmonkeyEngine that allows you to use a regular JavaFX GUI in your games.

**Introduction**
-  
This implementation is based on the JavaSaBr implementation of JME-JFX with the dependency to RLib removed
and any 3D scene code removed, leaving only the 2D GuiNode implementation.
Additionally no external repository URLs are required. Simply add the dependency to your project and start using it.

Why no 3D scene implementation? In practice it makes sense to use JavaFX as a 2D GUI, and for the 3D scene I recommend
Lemur.

This implementation fully supports FXML and CSS, which means you can use the Gluon SceneBuilder application
to create a fully-fledged themed scene with JavaFx Controllers. The JavaFX process of creating objects is unchanged
and follows the regular JavaFX workflow.

**Add the library to your project**
-

**Gradle**
``` groovy

// add the openjfx plugin so you can use javafx
plugins {
    id 'org.openjfx.javafxplugin' version '0.0.7'
}

// choose the javafx modules you want to import.
javafx { 
    version = "11.0.2"
    modules = [ 'javafx.controls', 'javafx.fxml' ]
}

// import the jme-jfx dependency
dependencies {
    implementation 'com.jayfella:jme-jfx-11:1.1.4'
}
```


**Integrate it into your game**
-
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

        JavaFxUI.initialize(this);
        
        Button button = new Button("Click Me");
        JavaFxUI.getInstance().attachChild(button);
    }
}

```

**Controls**
-
You may create any JavaFX object that extends **javafx.scene.Node** in either code or fxml.
Controllers and loading fxml from resources are all supported.

Controls are added and removed similar to the jmonkey workflow via the JavaFxUI static class.

``` java

// add a control
JavaFxUI.getInstance().attachChild(javafx.scene.Node node);

// remove a control
JavaFxUI.getInstance().detachChild(javafx.scene.Node node);
JavaFxUI.getInstance().detachChild(String fxId);

// get a control
JavaFxUI.getInstance().getChild(String fxId);

```

Since all JavaFX controls run in the JavaFX thread, you may need to alternate between the JME thread and
the JavaFX thread. There are two convenience methods provided to do this.

``` java

// do something in the JavaFX thread (such as update a label text)
JavaFxUI.getInstance().runInJavaFxThread(() -> {
    myLabel.setText("changed text");
});

// do something in the JME thread (such as manipulate the scene)
JavaFxUI.getInstance().runInJmeThread(() -> {
    someJmeNode.setLocalTranslation(x, y, z);
});

```

Although JavaFX alerts are supported, you may wish to display an in-game custom dialog.
These dialogs allow you to dim the background and stop background objects being clicked.

``` java

// display a javafx control that is centered in the screen with a dimmed background.
JavaFxUI.getInstance().showDialog(myJavaFxControl)

// display a javafx control that is centered and NOT dimmed.
JavaFxUI.getInstance().showDialog(myJavaFxControl, false);

// remove the dialog
JavaFxUI.getInstance().removeDialog();

```

**Update Loops**
-
If you need an update loop in your control, implement the JmeUpdateLoop interface and you will be provided with a
 `update(float tpf)` method that will be called every frame.

``` java
public class MyControl implements JmeUpdateLoop {
    @Override
    public void update(float tpf) {
        // update logic goes here...
    }
}
```

**Add and Remove Notifications**
-
If your control requires notification that it has been added or removed, implement the SceneNotifier interface.
You will be provided with an `onAttached(Application app)` and `void onDetached()` method that is called when it is added and removed from the scene.

``` java
public class MyControl implements SceneNotifier {
    @Override
    public void onAttached(Application app) {
        // called whenever you add this control to the JavaFX scene.
    }

    @Override
    public void onDetached() {
        // called whenever this control is removed from the scene.
    }

}
```
