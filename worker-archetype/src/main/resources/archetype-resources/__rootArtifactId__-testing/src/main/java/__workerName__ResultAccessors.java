package ${package};

import com.hpe.caf.util.ref.ReferencedData;

public final class ${workerName}ResultAccessors {
    private ${workerName}ResultAccessors() {
    }

    public static ReferencedData getTextData(final ${workerName}Result workerResult) {
        return workerResult.textData;
    }
}
