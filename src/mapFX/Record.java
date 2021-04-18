package mapFX;

import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;

public class Record {
    private int x;
    private int y;
    private String name;

    public Record(int x, int y, String name) {
        this.x = x;
        this.y = y;
        this.name = name;
    }

    @Override
    public String toString() {
        return String.format("X %4d | Y %4d | NAME %8s", x, y, name);
    }

    public void printOnCanvas(Pane canvas) {
        double transX = canvas.getWidth() * 0.5 + x * 0.5;
        double transY = canvas.getHeight() * 0.75 - y * 0.5;
        Circle point = new Circle(8);
        point.setTranslateX(transX);
        point.setTranslateY(transY);
        Tooltip tooltip = new Tooltip(this.toString());
        //tooltip.setShowDelay(Duration.ZERO);
        Tooltip.install(point, tooltip);
        canvas.getChildren().add(point);
    }
}