-- Avatar do usuário (admin/secretaria/CEO/professor que usa o sistema).
-- A URL aponta pro bucket GCS uebiescola-media, exposto via storage.googleapis.com.
ALTER TABLE users ADD COLUMN IF NOT EXISTS photo_url VARCHAR(500);
