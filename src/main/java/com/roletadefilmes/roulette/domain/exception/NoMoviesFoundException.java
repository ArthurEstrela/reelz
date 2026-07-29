package com.roletadefilmes.roulette.domain.exception;

public class NoMoviesFoundException extends RuntimeException {

    public NoMoviesFoundException() {
        super("Nenhum filme foi encontrado. Altere os serviços, o gênero ou a vibe selecionada.");
    }
}
