package com.wbscouting.api.enums;

/**
 * Ciclo de vida do Scouting Desk (ADM-018):
 * PENDING → triagem ativa
 * REVIEWING → sendo analisado
 * APPROVED → aprovado, passível de promoção para Casting
 * DECLINED → declinado, passível de exclusão definitiva (Hard Delete)
 * PROMOTED → promovido para o catálogo oficial de modelos (fora da fila)
 * REJECTED → alias LEGADO (preferir DECLINED para novas operações)
 */
public enum SubmissionStatus {
    PENDING,
    REVIEWING,
    APPROVED,
    DECLINED,
    PROMOTED,
    REJECTED,
    CONTACTED,
    ARCHIVED
}
