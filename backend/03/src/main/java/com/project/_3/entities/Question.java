package com.project._3.entities;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 3000, nullable = false)
    private String questionBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DifficultyLevel difficulty;

    @Enumerated(EnumType.STRING)
    private RequiredUsage requiredUsage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Topics topic;


    // ESPACO ENTRE ENTIDADE E ENUM PQ ME DA AGONIA ESTAR MUITO PERTO


    public enum QuestionType {
        MULTIPLE_CHOICE,
        PRACTICAL
    }

    public enum DifficultyLevel {
        EASY,
        MEDIUM,
        HARD
    }

    public enum RequiredUsage {
        WHILE,
        FOR,
        IF
    }

    public enum Topics {
        OPERADORES_TIPOS_E_VARIAVEIS,
        EXECUCAO_CONDICIONAL,
        OPERADORES_LOGICOS,
        LACOS,
        SUBPROGRAMAS,
        VETORES,
        ARRAYS,
        TIPOS_CRIADOS_PELO_PROGRAMADOR
    }
}
