package br.com.uebiescola.core.infrastructure.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A-5 AUDITORIAADMINPLATAFORMA 03/09/2026: marca um endpoint GET como
 * "leitura sensivel". O AuditAspect registra uma entrada quando o
 * chamador for CEO (schoolId=null no token).
 *
 * Estrategia: nao registrar leitura de tudo — log de tudo vira ruido e
 * problema de retencao. So marcar endpoints que retornam dado pessoal de
 * titular (nome de aluno/responsavel, contato, saude, etc.) ou dados
 * comerciais das escolas-clientes.
 *
 * Exemplo:
 * <pre>
 *   {@literal @}AuditableRead(entity = "Escola", action = "Consultou")
 *   {@literal @}GetMapping("/schools/{id}")
 *   public School getById(...) { ... }
 * </pre>
 *
 * Se o metodo tem {@literal @}PathVariable numerico ou UUID, o Aspect
 * grava esse valor em details.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuditableRead {
    /** Nome legivel da entidade lida (Escola, Usuario, ...). */
    String entity();
    /** Verbo (default "Consultou"). Use "Exportou" pra downloads. */
    String action() default "Consultou";
}
