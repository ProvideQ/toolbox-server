package edu.kit.provideq.toolbox.featuremodel.anomaly.voidmodel;

import de.ovgu.featureide.fm.core.base.IFeature;
import java.util.List;

public record FeatureModelVoidCheckResult(boolean isVoid, List<IFeature> counterExample) {
}
