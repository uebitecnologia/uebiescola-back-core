-- Fix existing users that don't have the 'active' column set
UPDATE users SET active = true WHERE active IS NULL;
