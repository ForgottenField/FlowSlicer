package FlowSlicer.Mode;
import lombok.Getter;

import java.util.Comparator;
public class SDGSlicerComparator implements Comparator<SDGBackwardsSlicer> {
    @Getter
    private static SDGSlicerComparator instance = new SDGSlicerComparator();

    private SDGSlicerComparator() {
    }

    @Override
    public int compare(SDGBackwardsSlicer slicer1, SDGBackwardsSlicer slicer2) {
        final GlobalVisit globalVisit1 = slicer1.getGlobalVisit();
        final GlobalVisit globalVisit2 = slicer2.getGlobalVisit();
        if (globalVisit1.getContextSensitivity().size() > globalVisit2.getContextSensitivity().size()) {
            return -1;
        } else if (globalVisit1.getContextSensitivity().size() < globalVisit2.getContextSensitivity().size()) {
            return 1;
        }
        return 0;
    }
}
