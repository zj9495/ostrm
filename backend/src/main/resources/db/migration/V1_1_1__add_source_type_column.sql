-- Add source_type column to openlist_config table
-- Existing rows default to 'OPENLIST' to preserve backward compatibility
ALTER TABLE openlist_config ADD COLUMN source_type VARCHAR(20) NOT NULL DEFAULT 'OPENLIST';
