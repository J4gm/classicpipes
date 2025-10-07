package jagm.classicpipes.inventory.container;

import jagm.classicpipes.blockentity.PipeEntity;
import net.minecraft.world.Container;

public interface Filter extends Container {

    void setMatchComponents(boolean matchComponents);

    boolean shouldMatchComponents();

    PipeEntity getPipe();

    enum MatchingResult {

        ITEM(true),
        TAG(true),
        MOD(true),
        FALSE(false);

        public final boolean matches;

        MatchingResult(boolean matches) {
            this.matches = matches;
        }

    }

}
