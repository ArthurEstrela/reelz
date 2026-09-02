package com.roletadefilmes.observability;

import com.roletadefilmes.observability.CineGiroMetrics.RouletteSpinOutcome;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CineGiroMetricsTest {

    @Test
    void shouldRecordTheProductFunnelWithoutHighCardinalityTags() {
        var registry = new SimpleMeterRegistry();
        var metrics = new CineGiroMetrics(registry);
        var sample = metrics.startRouletteSpin();

        metrics.recordUserRegistrationAfterCommit();
        metrics.recordOnboardingCompletionAfterCommit(7);
        metrics.recordRouletteSpin(sample, RouletteSpinOutcome.SUCCESS, "free");

        assertThat(registry.counter("reelz.users.registered").count()).isEqualTo(1);
        assertThat(registry.counter("reelz.onboarding.completed").count()).isEqualTo(1);
        assertThat(registry.get("reelz.onboarding.watched.movies").summary().totalAmount())
                .isEqualTo(7);
        assertThat(registry.get("reelz.roulette.spins")
                .tags("outcome", "success", "plan", "free")
                .counter()
                .count()).isEqualTo(1);
        assertThat(registry.get("reelz.roulette.spin.duration")
                .tags("outcome", "success", "plan", "free")
                .timer()
                .count()).isEqualTo(1);
    }
}
