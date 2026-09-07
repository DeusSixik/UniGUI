package dev.sixik.isf.trigger;

import dev.sixik.isf.definition.IsfTriggerBinding;

/** Политика сопоставления binding с фактическим игровым событием. */
@FunctionalInterface
public interface IsfTriggerType {
    boolean matches(IsfTriggerBinding binding, IsfTriggerContext context);
}
