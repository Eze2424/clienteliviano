package ar.edu.utn.frba.ddsi.clienteliviano.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RankingMensualResponse(
    Integer posicion,
    Long idDonante,
    String nombre,
    String tipoDonante,
    String categoria,
    Long misionesCumplidas,
    Integer insignias
) {}
