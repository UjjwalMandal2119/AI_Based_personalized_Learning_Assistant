ALTER TABLE ai_recommendations
    ADD COLUMN provider_name      VARCHAR(30)  NULL AFTER suggested_actions,
    ADD COLUMN model_name         VARCHAR(80)  NULL AFTER provider_name,
    ADD COLUMN generation_duration_ms BIGINT   NULL AFTER model_name,
    ADD COLUMN fallback_used      TINYINT(1)   NOT NULL DEFAULT 0 AFTER generation_duration_ms;
