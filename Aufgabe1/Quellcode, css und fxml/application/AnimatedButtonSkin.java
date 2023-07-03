package application;

import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.util.Duration;

import javafx.scene.control.Button;
import javafx.scene.control.skin.ButtonSkin;

public class AnimatedButtonSkin extends ButtonSkin {

    public AnimatedButtonSkin(Button btn) {
        super(btn);

        final FadeTransition fadeIn = new FadeTransition(Duration.millis(150));
        final FadeTransition fadeOut = new FadeTransition(Duration.millis(100));
        fadeIn.setNode(btn);
        fadeOut.setNode(btn);
        fadeIn.setToValue(1);
        fadeOut.setToValue(0.6);
        fadeOut.setOnFinished(e -> {
            if (! btn.isPressed()) {
                fadeIn.play();
            }
        });
        btn.setOnMousePressed(e -> {
            fadeIn.stop();
            fadeOut.play();
        });
        btn.setOnMouseReleased(e -> {
            if (btn.isHover()) {
                fadeOut.stop();
                fadeIn.play();
            }
        });

        final ScaleTransition grow = new ScaleTransition(Duration.millis(100));
        grow.setNode(btn);
        grow.setInterpolator(Interpolator.EASE_OUT);
        grow.setToX(1.1);
        grow.setToY(1.1);
        btn.setOnMouseEntered(e -> grow.play());

        final ScaleTransition shrink = new ScaleTransition(Duration.millis(100));
        shrink.setNode(btn);
        shrink.setInterpolator(Interpolator.EASE_OUT);
        shrink.setToX(1);
        shrink.setToY(1);
        btn.setOnMouseExited(e -> {
            shrink.play();
            fadeIn.play();
        });
    }
}