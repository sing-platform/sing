package br.cadi.common.domain;

import jakarta.persistence.Table;

public interface Trackable {
    Long getId();
    default String auditTableName() {
        // pega o nome da tabela via anotação @Table automaticamente
        Table table = this.getClass().getAnnotation(Table.class);
        return table != null ? table.name() : this.getClass().getSimpleName().toLowerCase();
    }
}
