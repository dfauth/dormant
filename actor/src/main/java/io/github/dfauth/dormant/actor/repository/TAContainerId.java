package io.github.dfauth.dormant.actor.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TAContainerId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Column(name = "market", nullable = false)
    private String market;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "date", nullable = false)
    private LocalDate date;
}
