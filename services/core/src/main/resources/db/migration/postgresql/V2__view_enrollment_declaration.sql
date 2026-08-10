-- ============================================================================
-- View de apoio a declaracao de matricula (era V8 no monolito).
--
-- Conversoes em relacao ao T-SQL original:
--   STRING_AGG(x, ', ') WITHIN GROUP (ORDER BY y)  ->  string_agg(x, ', ' ORDER BY y)
--   LEFT(CAST(t AS VARCHAR(8)), 5)                 ->  to_char(t, 'HH24:MI')
--   is_active = 1 / is_primary = 1                 ->  IS TRUE  (colunas BOOLEAN)
--
-- A declaracao de matricula e documento oficial: continua sendo lida de forma
-- sincrona pelo core, nunca por read model eventualmente consistente.
-- ============================================================================

CREATE VIEW vw_enrollment_declaration AS
SELECT
    p.id                        AS student_id,
    p.name                      AS student_name,
    p.birth_date                AS student_birth_date,
    gp.name                     AS guardian_name,
    sr.id                       AS school_room_id,
    sr.name                     AS school_room_name,
    w.name                      AS workshop_name,
    CASE
        WHEN slot.slot_days IS NULL THEN NULL
        ELSE 'durante as ' || slot.slot_days
             || ', das '    || to_char(slot.slot_start_time, 'HH24:MI')
             || ' até as '  || to_char(slot.slot_end_time,   'HH24:MI')
    END                         AS period,
    tp.name                     AS teacher_name,
    e.id                        AS enrollment_id,
    e.enrollment_date           AS enrollment_date,
    e.is_active                 AS enrollment_active
FROM enrollment e
    JOIN      student  s  ON s.person_id  = e.student_id
    JOIN      person   p  ON p.id         = s.person_id
    LEFT JOIN student_guardian sg ON sg.student_id = s.person_id
    LEFT JOIN guardian g  ON g.person_id  = sg.guardian_id AND g.is_primary IS TRUE
    LEFT JOIN person   gp ON gp.id        = g.person_id
    JOIN      schoolroom sr ON sr.id      = e.schoolroom_id
    LEFT JOIN workshop w  ON w.id         = sr.workshop_id
    LEFT JOIN person   tp ON tp.id        = sr.teacher_id
    LEFT JOIN (
        SELECT
            slot.schoolroom_id,
            string_agg(
                CASE slot.day_of_week
                    WHEN 'MONDAY'    THEN 'segundas-feiras'
                    WHEN 'TUESDAY'   THEN 'terças-feiras'
                    WHEN 'WEDNESDAY' THEN 'quartas-feiras'
                    WHEN 'THURSDAY'  THEN 'quintas-feiras'
                    WHEN 'FRIDAY'    THEN 'sextas-feiras'
                    WHEN 'SATURDAY'  THEN 'sábados'
                    WHEN 'SUNDAY'    THEN 'domingos'
                END,
                ', '
                ORDER BY CASE slot.day_of_week
                    WHEN 'MONDAY'    THEN 1
                    WHEN 'TUESDAY'   THEN 2
                    WHEN 'WEDNESDAY' THEN 3
                    WHEN 'THURSDAY'  THEN 4
                    WHEN 'FRIDAY'    THEN 5
                    WHEN 'SATURDAY'  THEN 6
                    WHEN 'SUNDAY'    THEN 7
                END
            )                       AS slot_days,
            MIN(slot.start_time)    AS slot_start_time,
            MAX(slot.end_time)      AS slot_end_time
        FROM schoolroom_schedule_slot slot
        WHERE slot.is_active IS TRUE
        GROUP BY slot.schoolroom_id
    ) slot ON slot.schoolroom_id = sr.id
WHERE e.is_active IS TRUE;
