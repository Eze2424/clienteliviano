package ar.edu.utn.frba.ddsi.clienteliviano.models.vista;

public record BienDetalleVista(
    Long id,
    String descripcion,
    double cantidad,
    boolean esUsado,
    String fechaDeVencimiento,
    double peso,
    double volumen,
    String foto
) {}
