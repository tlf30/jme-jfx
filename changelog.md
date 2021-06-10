1.2.1
---
* Modify build scripts for maven central release

1.2.0
---
* Fix cursors not working
* Fix input events for more consistent consumption
* Modifications for jfx 12+
* Modification for jme 3.4.0
* Other minor fixes

1.1.5
---
* Fix for situations when *keyup* events are being consumed by javafx when JME consumes *keydown*.
* Fix for situations when *mouseup* events are being consumed by javafx when JME comsumes *mousedown*.

1.1.4
---
* Fix for dialogs not centering correctly.

1.1.3
---
* Fix for the jfx thread closing if no scene is present.

1.1.2
---
* Added support for vertical mouse-wheel scrolling.

1.1.0
---
* Set jmonkey version to latest release instead of latest version.
* Remove unnecessary logging dependencies.
* Add optional String... argument to JavaFxUI.initialize to allow importing global css stylesheets.
* Replace System.out.println outputs to logging system
* Use singleton pattern. All methods are now called from .getInstance()

