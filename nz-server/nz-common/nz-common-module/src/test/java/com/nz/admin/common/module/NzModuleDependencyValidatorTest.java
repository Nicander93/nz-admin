package com.nz.admin.common.module;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NzModuleDependencyValidatorTest {

    @Test
    void shouldAcceptEnabledDependency() {
        NzModuleDescriptor system = new NzModuleDescriptor("system", "系统", "", "", List.of(), List.of(), true, "system");
        NzModuleDescriptor job = new NzModuleDescriptor("job", "任务", "", "", List.of("system"), List.of(), true, "job");

        assertDoesNotThrow(() -> NzModuleDependencyValidator.validate(List.of(system, job), Set.of("system", "job")));
    }

    @Test
    void shouldRejectDisabledDependency() {
        NzModuleDescriptor job = new NzModuleDescriptor("job", "任务", "", "", List.of("system"), List.of(), true, "job");

        assertThrows(IllegalStateException.class,
                () -> NzModuleDependencyValidator.validate(List.of(job), Set.of("job")));
    }
}
