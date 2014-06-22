package pondero.engine.test.stimuli;

import java.awt.Graphics2D;
import pondero.engine.elements.Element;

public abstract class VisualStimulus extends Stimulus {

    public VisualStimulus(final Element parent) {
        super(parent);
    }

    public abstract void render(final Graphics2D g2d, int surfaceWidth, int surfaceHeight);

}
