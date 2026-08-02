package com.roletadefilmes.observability;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Locale;

@Component
public class ReelzMetrics {

    private final MeterRegistry registry;

    public ReelzMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public Timer.Sample startRouletteSpin() {
        return Timer.start(registry);
    }

    public void recordRouletteSpin(
            Timer.Sample sample,
            RouletteSpinOutcome outcome,
            String plan
    ) {
        var outcomeTag = outcome.name().toLowerCase(Locale.ROOT);
        var normalizedPlan = plan.toLowerCase(Locale.ROOT);

        registry.counter(
                "reelz.roulette.spins",
                "outcome", outcomeTag,
                "plan", normalizedPlan
        ).increment();
        sample.stop(Timer.builder("reelz.roulette.spin.duration")
                .description("Tempo total para processar um giro da roleta")
                .tag("outcome", outcomeTag)
                .tag("plan", normalizedPlan)
                .register(registry));
    }

    public void recordRouletteSpinAfterCommit(
            Timer.Sample sample,
            RouletteSpinOutcome outcome,
            String plan
    ) {
        afterCommit(() -> recordRouletteSpin(sample, outcome, plan));
    }

    public void recordUserRegistrationAfterCommit() {
        afterCommit(() -> registry.counter("reelz.users.registered").increment());
    }

    public void recordOnboardingCompletionAfterCommit(int watchedMoviesAdded) {
        afterCommit(() -> {
            registry.counter("reelz.onboarding.completed").increment();
            DistributionSummary.builder("reelz.onboarding.watched.movies")
                    .description("Quantidade de filmes marcados como assistidos no onboarding")
                    .register(registry)
                    .record(watchedMoviesAdded);
        });
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    public enum RouletteSpinOutcome {
        SUCCESS,
        REPLAYED,
        NO_MOVIES,
        LIMIT_EXCEEDED,
        INVALID_REQUEST,
        ERROR
    }
}
